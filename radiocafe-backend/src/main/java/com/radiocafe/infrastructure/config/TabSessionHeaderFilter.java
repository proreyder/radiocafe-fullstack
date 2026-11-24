package com.radiocafe.infrastructure.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Reject malformed selectors before Spring attempts to load a context. */
public class TabSessionHeaderFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String tab = request.getHeader(TabSecurityContextRepository.HEADER);
        if (tab != null && !TabSecurityContextRepository.valid(tab)) {
            response.setStatus(400); response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Identificador de ventana inválido. Recarga la página.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
