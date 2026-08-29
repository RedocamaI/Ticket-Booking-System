package com.redocmi.api_gateway.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

public class HeaderMutatingRequest extends HttpServletRequestWrapper {
    private final Map<String, String> additionalHeaders;

    HeaderMutatingRequest(HttpServletRequest request,
                          String userId, String role) {
        super(request);
        this.additionalHeaders = new HashMap<>();
        this.additionalHeaders.put("X-User-Id", userId);
        this.additionalHeaders.put("X-User-Role", role);
    }

    @Override
    public String getHeader(String name) {
        if(this.additionalHeaders.containsKey(name))
            return additionalHeaders.get(name);

        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if(additionalHeaders.containsKey(name)) {
            return Collections.enumeration(
                    Collections.singletonList(additionalHeaders.get(name))
            );
        }

        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> names = Collections.list(super.getHeaderNames());
        names.addAll(additionalHeaders.keySet());

        return Collections.enumeration(names);
    }
}
