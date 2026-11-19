package com.radiocafe.domain.ports.in;
import com.radiocafe.domain.model.*;
import java.util.List;
public interface ManageUsersUseCase {
    List<Usuario> listar();
    Usuario crear(String nombre,String correo,String password,Rol rol,String telefono,Long actor);
    Usuario actualizar(Long id,String nombre,Rol rol,boolean activo,String telefono,Long actor);
}
