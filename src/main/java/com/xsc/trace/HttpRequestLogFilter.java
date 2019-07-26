package com.xsc.trace;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author xia
 * @date 2019/7/22 14:12
 */
public class HttpRequestLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString().replaceAll("-", "");
        MDC.put("TRACE_ID", traceId);
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long end = System.currentTimeMillis();

            MDC.remove("TRACE_ID");
        }
    }

    private void saveLog(String traceId, HttpServletRequest request, HttpServletResponse response, long start, long end) {
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
            PrintWriter writer = response.getWriter();
            writer.
            StringBuilder sb = new StringBuilder();
            sb.append("traceId:").append(traceId)
                    .append(",time=").append(end - start).append("ms")
                    .append(",url:").append(request.getRequestURI())
                    .append(",request=").append(paramStr)
                    .append(",requestBody=").append(requestBody.toString())
                    .append(",response=").append(response.get)

        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }

    }
}
