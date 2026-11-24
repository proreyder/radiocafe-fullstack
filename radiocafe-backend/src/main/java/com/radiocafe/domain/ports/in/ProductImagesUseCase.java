package com.radiocafe.domain.ports.in;

import com.radiocafe.domain.model.ImagenProducto;
import java.util.List;

public interface ProductImagesUseCase {
    ImagenProducto subir(byte[] contenido);
    ImagenProducto subirIlustracion(byte[] contenido);
    byte[] leerIlustracion(String nombre);
    ImagenProducto subirPerfil(byte[] contenido); byte[] leerPerfil(String nombre);
    List<ImagenProducto> listar();
    byte[] leer(String nombre);
}
