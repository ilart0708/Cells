package com.cells.backend.security;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

final class ProblemResponseWriter {

    private ProblemResponseWriter() {
    }

    // Only fixed, trusted strings are passed in, so no JSON escaping is needed.
    static void write(HttpServletResponse response, int status, String title, String detail)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"type\":\"about:blank\",\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\"}"
                        .formatted(title, status, detail));
    }
}