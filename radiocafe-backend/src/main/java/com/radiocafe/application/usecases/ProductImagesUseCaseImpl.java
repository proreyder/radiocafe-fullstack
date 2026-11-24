package com.radiocafe.application.usecases;

import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.ProductImagesUseCase;
import com.radiocafe.domain.ports.out.ImageStoragePort;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.List;

@Service
public class ProductImagesUseCaseImpl implements ProductImagesUseCase {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private final ImageStoragePort storage;
    public ProductImagesUseCaseImpl(ImageStoragePort storage) { this.storage = storage; }

    public ImagenProducto subir(byte[] contenido) {return procesar(contenido,"producto");}
    public ImagenProducto subirIlustracion(byte[] contenido) {return procesar(contenido,"kappu");}
    public byte[] leerIlustracion(String nombre) {if(!nombre.matches(ImagenProducto.FILE_PATTERN)) throw BusinessException.noEncontrado("Imagen no encontrada.");return storage.leerIlustracion(nombre);}
    public ImagenProducto subirPerfil(byte[] contenido) {return procesar(contenido,"perfil");}
    public byte[] leerPerfil(String nombre) {if(!nombre.matches(ImagenProducto.FILE_PATTERN)) throw BusinessException.noEncontrado("Imagen no encontrada.");return storage.leerPerfil(nombre);}
    private ImagenProducto procesar(byte[] contenido,String destino) {
        if (contenido.length == 0 || contenido.length > MAX_BYTES)
            throw invalid("Selecciona una imagen JPG o PNG de hasta 5 MB.");
        // Inspect dimensions before decoding; never trust the submitted name or MIME type.
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(contenido))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid("El archivo debe ser una imagen JPG o PNG válida.");
            var reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                if (!format.equals("png") && !format.equals("jpeg")) throw invalid("Solo se permiten imágenes JPG o PNG.");
                reader.setInput(input);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long)width * height > 12_000_000)
                    throw invalid("La imagen no puede superar los 12 megapíxeles.");
                var image = reader.read(0);
                var output = new ByteArrayOutputStream();
                ImageIO.write(image, format, output);
                // Re-encode to strip metadata and any content appended to the original image.
                String extension=format.equals("jpeg")?"jpg":"png";return switch(destino){case "perfil"->storage.guardarPerfil(output.toByteArray(),extension);case "kappu"->storage.guardarIlustracion(output.toByteArray(),extension);default->storage.guardar(output.toByteArray(),extension);};
            } finally { reader.dispose(); }
        } catch (IOException e) { throw invalid("No se pudo leer la imagen. Usa un archivo JPG o PNG válido."); }
    }
    public List<ImagenProducto> listar() { return storage.listar(); }
    public byte[] leer(String nombre) {
        if (!nombre.matches(ImagenProducto.FILE_PATTERN)) throw BusinessException.noEncontrado("Imagen no encontrada.");
        return storage.leer(nombre);
    }
    private BusinessException invalid(String message) { return new BusinessException(BusinessException.Tipo.INVALIDO, message); }
}
