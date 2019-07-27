package com.xsc.trace;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * created by XSC on 2019/7/27 10:06
 */
public class TraceHttpResponseWrap extends HttpServletResponseWrapper {

    private ByteArrayOutputStream outputStream;

    private PrintWriter printWriter;

    public TraceHttpResponseWrap(HttpServletResponse response) {
        super(response);
        outputStream = new ByteArrayOutputStream();
        printWriter = new PrintWriter(outputStream);
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return printWriter;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new ServletOutputStream() {
            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {

            }

            @Override
            public void write(int b) throws IOException {
                outputStream.write(b);
            }
        };
    }

    public void flush() {
        try {
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ByteArrayOutputStream getByteArrayOutputStream() {
        return outputStream;
    }

    public String getContent() {
        flush();
        return outputStream.toString();
    }
}
