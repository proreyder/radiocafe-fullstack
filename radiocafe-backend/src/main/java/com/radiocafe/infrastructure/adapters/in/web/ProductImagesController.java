package com.radiocafe.infrastructure.adapters.in.web;

import com.radiocafe.domain.model.ImagenProducto;
import com.radiocafe.domain.ports.in.ProductImagesUseCase;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@io.swagger.v3.oas.annotations.tags.Tag(name="Imágenes", description="Fotos JPG/PNG de productos. Subida y galería exclusivas del administrador; lectura pública.")
@RestController @RequestMapping("/api/v1/images")
public class ProductImagesController {
    private final ProductImagesUseCase images;
    public ProductImagesController(ProductImagesUseCase images) { this.images = images; }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ImagenProducto upload(@RequestPart("file") MultipartFile file) throws IOException { return images.subir(file.getBytes()); }
    @GetMapping @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<ImagenProducto> list() { return images.listar(); }
    @GetMapping("/files/{nombre}")
    public ResponseEntity<byte[]> file(@PathVariable String nombre) {
        byte[] data = images.leer(nombre);
        return ResponseEntity.ok().contentType(nombre.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
            .header("X-Content-Type-Options", "nosniff").body(data);
    }
}
