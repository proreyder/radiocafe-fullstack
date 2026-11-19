package com.radiocafe.infrastructure.adapters.out.persistence;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.out.ProductRepositoryPort;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.ProductoEntity;
import com.radiocafe.infrastructure.adapters.out.persistence.repository.SpringDataProductoRepository;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class ProductoRepositoryAdapter implements ProductRepositoryPort {
 private final SpringDataProductoRepository repository;
 public ProductoRepositoryAdapter(SpringDataProductoRepository repository) {this.repository=repository;}
 public Optional<Producto> bloquearPorId(Long id) {return repository.bloquear(id).map(this::toDomain);}
 public List<Producto> obtenerTodos() {return repository.findAll().stream().map(this::toDomain).toList();}
 public boolean existenProductos() {return repository.count()>0;}
 public Producto guardar(Producto d) {var e=new ProductoEntity();e.setIdProducto(d.idProducto());
e.setPersonalizable(d.personalizable());
e.setStock(d.stock());
e.setNombre(d.nombre());
e.setDescripcion(d.descripcion());
e.setCategoria(d.categoria());
e.setPrecio(d.precio());
e.setImagenUrl(d.imagenUrl());
e.setDisponible(d.disponible());return toDomain(repository.save(e));}
 public Optional<Producto> buscarPorId(Long id) {return repository.findById(id).map(this::toDomain);}
 public List<Producto> obtenerTodosDisponibles() {return repository.findByDisponibleTrue().stream().map(this::toDomain).toList();}
 public List<Producto> obtenerPorCategoria(String categoria) {return repository.findByCategoriaAndDisponibleTrue(categoria).stream().map(this::toDomain).toList();}
 public void eliminarPorId(Long id) {repository.deleteById(id);}
 private Producto toDomain(ProductoEntity e) {return new Producto(e.getIdProducto(),e.getNombre(),e.getDescripcion(),e.getCategoria(),e.getPrecio(),e.getImagenUrl(),e.getDisponible(),e.getPersonalizable(),e.getStock());}
}
