package com.xsc.trace;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author xia
 * @date 2019/7/22 14:12
 */
public class HttpRequestLogFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestLogFilter.class);

    private String[] excludeUrls;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = TraceUtils.buildTraceId();
        MDC.put(Constants.TRACE_ID, traceId);
        long start = System.currentTimeMillis();
        boolean flag = true;
        try {
            for (String s : excludeUrls) {
                Pattern pattern = Pattern.compile(s);
                Matcher matcher = pattern.matcher(request.getRequestURI());
                if (matcher.find()) {
                    flag = false;
                    break;
                }
            }
            if (!flag) {
                filterChain.doFilter(request, response);
            } else {
                TraceHttpResponseWrap responseWrap = new TraceHttpResponseWrap(response);
                filterChain.doFilter(request, responseWrap);
                PrintWriter writer = response.getWriter();
                writer.write(responseWrap.getContent());
                if (responseWrap.getContentType().contains("application/json")) {
                    long end = System.currentTimeMillis();
                    saveLog(traceId, request, responseWrap, start, end);
                }
            }
        } finally {
            MDC.remove("TRACE_ID");
        }
    }

    private void saveLog(String traceId, HttpServletRequest request, TraceHttpResponseWrap responseWrapper, long start, long end) {
        try {
            Map<String, String[]> map = request.getParameterMap();
            List<String> params = Lists.newArrayList();
            if (MapUtils.isNotEmpty(map)) {
                for (Map.Entry<String, String[]> entry : map.entrySet()) {
                    for (String str : entry.getValue()) {
                        params.add(entry.getKey() + "=" + StringUtils.trimToEmpty(str));
                    }
                }
            }
            String paramStr = Joiner.on("&").join(params);
            if (StringUtils.equals(request.getContentType(), MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
                paramStr = URLDecoder.decode(paramStr, "UTF-8");
            }
            BufferedReader reader = request.getReader();
            StringBuilder requestBody = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("traceId:").append(traceId)
                    .append(",time=").append(end - start).append("ms")
                    .append(",url=").append(request.getRequestURI())
                    .append(",method=").append(request.getMethod())
                    .append(",request=").append(paramStr)
                    .append(",requestBody=").append(requestBody.toString())
                    .append(",status=").append(responseWrapper.getStatus())
                    .append(",response=").append(responseWrapper.getContent());
            LOGGER.info(sb.toString());
        } catch (IOException e) {
            LOGGER.error("日志记录出错!", e);
        }
    }

    public void setExcludeUrls(String[] excludeUrls) {
        this.excludeUrls = excludeUrls;
    }
}
