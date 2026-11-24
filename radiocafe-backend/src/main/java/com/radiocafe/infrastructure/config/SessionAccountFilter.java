package com.radiocafe.infrastructure.config;
import com.radiocafe.domain.ports.out.UserRepositoryPort;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
/** Account state/role changes revoke existing sessions at their next request. */
public class SessionAccountFilter extends OncePerRequestFilter {
    private final UserRepositoryPort users;
    private final org.springframework.security.web.context.SecurityContextRepository contexts;
    public SessionAccountFilter(UserRepositoryPort users,org.springframework.security.web.context.SecurityContextRepository contexts) {this.users=users;this.contexts=contexts;}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null && auth.getPrincipal() instanceof AuthenticatedUser principal) {
            var current=users.buscarPorCorreo(auth.getName());
            if(current.isEmpty() || !current.get().activo() || current.get().seguridadVersion()!=principal.getSeguridadVersion()) {
                SecurityContextHolder.clearContext();
                contexts.saveContext(SecurityContextHolder.createEmptyContext(),req,res);
                TabSecurityContextRepository.invalidateIfUnsharedLegacySession(req);
                res.setStatus(401);res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"message\":\"Tu cuenta o sus permisos cambiaron. Inicia sesión nuevamente.\"}");return;
            }
        }
        chain.doFilter(req,res);
    }
}
