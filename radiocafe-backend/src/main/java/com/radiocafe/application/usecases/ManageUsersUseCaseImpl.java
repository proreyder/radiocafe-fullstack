package com.radiocafe.application.usecases;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.ManageUsersUseCase;
import com.radiocafe.domain.ports.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
public class ManageUsersUseCaseImpl implements ManageUsersUseCase {
    private final UserRepositoryPort users;
    private final PasswordPort passwords;
    public ManageUsersUseCaseImpl(UserRepositoryPort users,PasswordPort passwords) {this.users=users;this.passwords=passwords;}
    private void administrador(Long actor) {
        var u=users.buscarPorId(actor).orElseThrow(()->BusinessException.noEncontrado("Administrador no encontrado."));
        if(!u.activo()||u.rol()!=Rol.ADMINISTRADOR) throw new BusinessException(BusinessException.Tipo.PROHIBIDO,"Se requiere un administrador activo.");
    }
    @Transactional(readOnly=true) public List<Usuario> listar() {return users.obtenerTodos();}
    @Transactional public Usuario crear(String nombre,String correo,String password,Rol rol,String telefono,Long actor) {
        users.bloquearAdministradores();administrador(actor);
        String email=correo.strip().toLowerCase(Locale.ROOT);
        if(users.existePorCorreo(email)) throw BusinessException.conflicto("Este correo ya está registrado.");
        return users.guardar(new Usuario(null,nombre.strip(),email,passwords.codificar(password),rol,telefono,true,0));
    }
    @Transactional public Usuario actualizar(Long id,String nombre,Rol rol,boolean activo,String telefono,Long actor) {
        users.bloquearAdministradores();administrador(actor);
        var u=users.bloquearPorId(id).orElseThrow(()->BusinessException.noEncontrado("Usuario no encontrado."));
        if(id.equals(actor) && (!activo || rol!=Rol.ADMINISTRADOR)) throw BusinessException.conflicto("No puedes desactivar tu cuenta ni quitarte el rol de administrador.");
        if(u.rol()==Rol.ADMINISTRADOR && u.activo() && (!activo || rol!=Rol.ADMINISTRADOR)
                && users.obtenerTodos().stream().filter(x->x.activo()&&x.rol()==Rol.ADMINISTRADOR).count()<=1)
            throw BusinessException.conflicto("Debe permanecer al menos un administrador activo.");
        long version=u.seguridadVersion()+((u.rol()!=rol || u.activo()!=activo)?1:0);
        return users.guardar(new Usuario(id,nombre.strip(),u.correo(),u.password(),rol,telefono,activo,version,u.fotoUrl(),u.direccion()));
    }
}
