package com.radiocafe.domain.ports.out;

import com.radiocafe.domain.model.ImagenProducto;
import java.util.List;

public interface ImageStoragePort {
    ImagenProducto guardar(byte[] contenido, String extension);
    ImagenProducto guardarIlustracion(byte[] contenido,String extension);
    byte[] leerIlustracion(String nombre);
    ImagenProducto guardarPerfil(byte[] contenido,String extension); byte[] leerPerfil(String nombre);
    List<ImagenProducto> listar();
    byte[] leer(String nombre);
}
