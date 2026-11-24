package com.radiocafe.infrastructure.adapters.out.persistence;

import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.out.ImageStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.file.*;
import java.util.*;

@Component
public class LocalImageStorageAdapter implements ImageStoragePort {
    private final Path directory;
    public LocalImageStorageAdapter(@Value("${app.images.directory:./uploads/products}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }
    public ImagenProducto guardar(byte[] contenido,String extension) {return guardarEn(directory,"/api/v1/images/files/",contenido,extension);}
    public ImagenProducto guardarIlustracion(byte[] contenido,String extension) {return guardarEn(directory.resolve("kappu"),"/api/v1/kappu/images/files/",contenido,extension);}
    public ImagenProducto guardarPerfil(byte[] contenido,String extension) {return guardarEn(directory.resolve("avatars"),"/api/v1/account/avatar/",contenido,extension);}
    public byte[] leerPerfil(String nombre) {return leerEn(directory.resolve("avatars"),nombre);}
    private ImagenProducto guardarEn(Path root,String prefix,byte[] contenido, String extension) {
        try {
            Files.createDirectories(root);
            Path file = root.resolve(UUID.randomUUID() + "." + extension);
            Files.write(file, contenido, StandardOpenOption.CREATE_NEW);
            return info(file,prefix);
        } catch (IOException e) { throw new UncheckedIOException("No se pudo guardar la imagen.", e); }
    }
    public List<ImagenProducto> listar() {
        if (!Files.exists(directory)) return List.of();
        try (var files = Files.list(directory)) {
            return files.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
                .filter(p -> p.getFileName().toString().matches(ImagenProducto.FILE_PATTERN))
                .map(this::info).sorted(Comparator.comparing(ImagenProducto::fecha).reversed()).toList();
        } catch (IOException e) { throw new UncheckedIOException("No se pudieron listar las imágenes.", e); }
    }
    public byte[] leer(String nombre) {return leerEn(directory,nombre);}
    public byte[] leerIlustracion(String nombre) {return leerEn(directory.resolve("kappu"),nombre);}
    private byte[] leerEn(Path root,String nombre) {
        Path file = root.resolve(nombre).normalize();
        if (!file.getParent().equals(root) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
            throw BusinessException.noEncontrado("Imagen no encontrada.");
        try { return Files.readAllBytes(file); }
        catch (IOException e) { throw new UncheckedIOException("No se pudo leer la imagen.", e); }
    }
    private ImagenProducto info(Path file) {return info(file,"/api/v1/images/files/");}
    private ImagenProducto info(Path file,String prefix) {
        try {
            String name = file.getFileName().toString();
            return new ImagenProducto(name, prefix + name, Files.size(file), Files.getLastModifiedTime(file).toInstant());
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
}
