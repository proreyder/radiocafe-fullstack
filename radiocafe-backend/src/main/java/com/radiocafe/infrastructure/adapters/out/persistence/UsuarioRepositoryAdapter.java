package com.radiocafe.infrastructure.adapters.out.persistence;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.out.UserRepositoryPort;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.UsuarioEntity;
import com.radiocafe.infrastructure.adapters.out.persistence.repository.SpringDataUsuarioRepository;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class UsuarioRepositoryAdapter implements UserRepositoryPort {
 private final SpringDataUsuarioRepository repository;
 public UsuarioRepositoryAdapter(SpringDataUsuarioRepository repository) {this.repository=repository;}
 public List<Usuario> obtenerTodos() {return repository.findAll().stream().map(this::toDomain).toList();}
 public void bloquearAdministradores() {repository.bloquearAdministradores();}
 public Usuario guardar(Usuario d) {var e=new UsuarioEntity();e.setIdUsuario(d.idUsuario());
e.setActivo(d.activo());
e.setSeguridadVersion(d.seguridadVersion());
e.setNombre(d.nombre());
e.setCorreo(d.correo());
e.setPassword(d.password());
e.setRol(d.rol());
e.setTelefono(d.telefono());return toDomain(repository.save(e));}
 public Optional<Usuario> buscarPorId(Long id) {return repository.findById(id).map(this::toDomain);}
 public Optional<Usuario> buscarPorCorreo(String correo) {return repository.findByCorreo(correo).map(this::toDomain);}
 public boolean existePorCorreo(String correo) {return repository.existsByCorreo(correo);}
 private Usuario toDomain(UsuarioEntity e) {return new Usuario(e.getIdUsuario(),e.getNombre(),e.getCorreo(),e.getPassword(),e.getRol(),e.getTelefono(),e.isActivo(),e.getSeguridadVersion());}
}
