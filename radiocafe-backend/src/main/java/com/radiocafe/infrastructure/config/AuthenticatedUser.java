package com.radiocafe.infrastructure.config;
import com.radiocafe.domain.model.Usuario;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
public class AuthenticatedUser extends User {
    private static final long serialVersionUID=1L;
    private final long seguridadVersion;
    public AuthenticatedUser(Usuario u) {
        super(u.correo(),u.password(),u.activo(),true,true,true,List.of(new SimpleGrantedAuthority("ROLE_"+u.rol().name())));
        seguridadVersion=u.seguridadVersion();
    }
    public long getSeguridadVersion() {return seguridadVersion;}
}
