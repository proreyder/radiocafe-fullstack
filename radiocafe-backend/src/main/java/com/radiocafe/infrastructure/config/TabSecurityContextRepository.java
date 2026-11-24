package com.radiocafe.infrastructure.config;

import jakarta.servlet.http.*;
import org.springframework.security.core.context.*;
import org.springframework.security.web.context.*;

/** A browser cookie identifies the container; a window key selects its own identity. */
public class TabSecurityContextRepository implements SecurityContextRepository {
    public static final String HEADER = "X-RadioCafe-Tab";
    public static boolean valid(String value) {
        return value != null && value.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
    }
    public static void invalidateIfUnsharedLegacySession(HttpServletRequest request) {
        if (request.getHeader(HEADER) != null) return;
        var session = request.getSession(false);
        if (session == null) return;
        boolean otherWindows = java.util.Collections.list(session.getAttributeNames()).stream()
            .anyMatch(name -> name.startsWith("RADIOCAFE_TAB_CONTEXT_"));
        if (!otherWindows) session.invalidate();
    }
    private HttpSessionSecurityContextRepository delegate(HttpServletRequest request) {
        var repository = new HttpSessionSecurityContextRepository();
        repository.setDisableUrlRewriting(true);
        String tab = request.getHeader(HEADER);
        if (tab != null) {
            if (!valid(tab)) throw new IllegalArgumentException("Identificador de ventana inválido.");
            repository.setSpringSecurityContextKey("RADIOCAFE_TAB_CONTEXT_" + tab);
        }
        return repository;
    }
    @Override public SecurityContext loadContext(HttpRequestResponseHolder holder) {
        return delegate(holder.getRequest()).loadContext(holder);
    }
    @Override public DeferredSecurityContext loadDeferredContext(HttpServletRequest request) {
        return delegate(request).loadDeferredContext(request);
    }
    @Override public boolean containsContext(HttpServletRequest request) { return delegate(request).containsContext(request); }
    @Override public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        delegate(request).saveContext(context, request, response);
    }
}
