package com.xsc.trace;

import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.xsc.util.date.DateTimeUtil;
import com.xsc.util.json.JsonUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author xia
 * @date 2019/7/29 16:59
 */
public class DubboAccessLogFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DubboAccessLogFilter.class);

    private String fileName;

    private ConcurrentHashMap<String, List<String>> msgMap = new ConcurrentHashMap<>();

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder().setNameFormat("dubbo-log").build();

    private static final ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(3, 5,
            60, TimeUnit.SECONDS, new LinkedBlockingDeque<>(), THREAD_FACTORY, new ThreadPoolExecutor.AbortPolicy());

    public DubboAccessLogFilter(String fileName) {
        this.fileName = fileName;
        String home = System.getProperty("catalina.base");
        if (home == null) {
            home = "target";
        } else {
            home = home + "/logs";
        }
        try {
            home = new File(home).getCanonicalPath();
        } catch (IOException e) {
            LOGGER.debug("logger homepath failed: ", e);
        }
        fileName = home + "/" + fileName;
    }

    private static String getTraceId() {
        String traceId = MDC.get(Constants.TRACE_ID);
        if (StringUtils.isBlank(traceId)) {
            traceId = TraceUtils.buildTraceId();
        }
        return traceId;
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        long start = System.currentTimeMillis();
        Result result = invoker.invoke(invocation);
        long end = System.currentTimeMillis();
        log(invoker, invocation, result, start, end);
        return result;
    }

    private void log(Invoker<?> invoker, Invocation invocation, Result result, long start, long end) {
        String msg = buildMsg(invoker, invocation, result, start, end);
        EXECUTOR_SERVICE.submit(new MyTask());
        List<String> msgList = msgMap.get(fileName);
        if (msgList == null) {
            msgMap.putIfAbsent(fileName, Lists.newArrayList());
            msgList = msgMap.get(fileName);
        }
        msgList.add(msg);
    }

    private String buildMsg(Invoker<?> invoker, Invocation invocation, Result result, long start, long end) {
        StringBuilder sb = new StringBuilder();
        try {
            RpcContext context = RpcContext.getContext();
            String consumer;
            int consumerPort;
            String provider;
            int providerPort;
            if (context.isConsumerSide()) {
                consumer = context.getLocalHost();
                consumerPort = context.getLocalPort();
                provider = context.getRemoteHost();
                providerPort = context.getRemotePort();
            } else {
                consumer = context.getRemoteHost();
                consumerPort = context.getRemotePort();
                provider = context.getLocalHost();
                providerPort = context.getLocalPort();
            }
            String serviceName = invoker.getInterface().getName();
            String version = invoker.getUrl().getParameter(com.alibaba.dubbo.common.Constants.VERSION_KEY);
            String group = invoker.getUrl().getParameter(com.alibaba.dubbo.common.Constants.GROUP_KEY);
            String consumerApp = invoker.getUrl().getParameter(com.alibaba.dubbo.common.Constants.APPLICATION_KEY);
            String traceId = MDC.get(Constants.TRACE_ID);
            sb.append("[").append(DateTimeUtil.parse4Y2M2D2H2M2S(new Date())).append("] ")
                    .append(" [").append(traceId).append("] ")
                    .append(" [").append(consumerApp).append("] ")
                    .append(end - start).append(" ")
                    .append(consumer).append(':').append(consumerPort)
                    .append(" -> ")
                    .append(provider).append(':').append(providerPort)
                    .append(" - ");
            if (null != group && group.length() > 0) {
                sb.append(group).append("/");
            }
            sb.append(serviceName);
            if (null != version && version.length() > 0) {
                sb.append(":").append(version);
            }
            sb.append(" ");
            sb.append(invocation.getMethodName());
            sb.append("(");
            Class<?>[] types = invocation.getParameterTypes();
            if (types != null && types.length > 0) {
                boolean first = true;
                for (Class<?> type : types) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(",");
                    }
                    sb.append(type.getName());
                }
            }
            sb.append(") ");
            Object[] args = invocation.getArguments();
            if (args != null && args.length > 0) {
                sb.append(JsonUtil.of(args));
            }
        } catch (Throwable t) {
            LOGGER.warn("Exception in AccessLogFilter of service(" + invoker + " -> " + invocation + ")", t);
        }
        return sb.toString();
    }

    private class MyTask implements Runnable {

        @Override
        public void run() {
            synchronized (EXECUTOR_SERVICE) {
                try {
                    if (MapUtils.isNotEmpty(msgMap)) {
                        for (Map.Entry<String, List<String>> entry : msgMap.entrySet()) {
                            String fileName = entry.getKey();
                            List<String> msgs = entry.getValue();
                            File file = new File(fileName);
                            File parent = file.getParentFile();
                            if (parent != null && !parent.exists()) {
                                parent.mkdirs();
                            }
                            if (file.exists()) {
                                String now = new SimpleDateFormat("yyyyMMdd").format(new Date());
                                String last = new SimpleDateFormat("yyyyMMdd").format(new Date(file.lastModified()));
                                if (!now.equals(last)) {
                                    File archive = new File(file.getAbsolutePath() + "." + last);
                                    file.renameTo(archive);
                                }
                            }
                            FileWriter writer = new FileWriter(file, true);
                            try {
                                for (String msg : msgs) {
                                    writer.write(msg);
                                    writer.write("\r\n");
                                }
                                writer.flush();
                            } finally {
                                writer.close();
                            }
                            msgs.clear();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Activate(group = {com.alibaba.dubbo.common.Constants.PROVIDER})
    private static class ProviderFilter implements Filter {

        private DubboAccessLogFilter filter;

        public ProviderFilter() {
            this.filter = new DubboAccessLogFilter("dubbo-provider.log");
        }

        @Override
        public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
            RpcContext context = RpcContext.getContext();
            MDC.put(Constants.TRACE_ID, context.getAttachment(Constants.TRACE_ID));
            try {
                return filter.invoke(invoker, invocation);
            } finally {
                MDC.remove(Constants.TRACE_ID);
            }

        }
    }

    @Activate(group = {com.alibaba.dubbo.common.Constants.CONSUMER})
    private static class ConsumerFilter implements Filter {

        private DubboAccessLogFilter filter;

        public ConsumerFilter() {
            this.filter = new DubboAccessLogFilter("dubbo-consumer.log");
        }

        @Override
        public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
            RpcContext context = RpcContext.getContext();
            context.setAttachment(Constants.TRACE_ID, getTraceId());
            return filter.invoke(invoker, invocation);
        }
    }
}
