package com.tcs.payments.security;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class MultiReadHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cached;

    public MultiReadHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cached = request.getInputStream().readAllBytes();
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream bais = new ByteArrayInputStream(cached);
        return new ServletInputStream() {
            @Override public int read() { return bais.read(); }
            @Override public boolean isFinished() { return bais.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener readListener) { /* no-op */ }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    public String getCachedBody(String charset) throws IOException {
        String cs = (charset != null ? charset : "UTF-8");
        return new String(cached, cs);
    }
}
