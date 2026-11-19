package com.radiocafe.application.usecases;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.CatalogUseCase;
import com.radiocafe.domain.ports.out.ProductRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
public class CatalogUseCaseImpl implements CatalogUseCase {
    private final ProductRepositoryPort products;
    public CatalogUseCaseImpl(ProductRepositoryPort products) {this.products=products;}
    public List<Producto> listar() {return products.obtenerTodosDisponibles();}
    @Transactional public Producto guardar(Long id,Producto p) {
        int stock=id==null?0:products.bloquearPorId(id).orElseThrow(()->BusinessException.noEncontrado("Producto no encontrado.")).stockDisponible();
        return products.guardar(new Producto(id,p.nombre().strip(),p.descripcion(),p.categoria(),p.precio(),p.imagenUrl(),true,Boolean.TRUE.equals(p.personalizable()),stock));
    }
    @Transactional public void eliminar(Long id) {
        var p=products.bloquearPorId(id).orElseThrow(()->BusinessException.noEncontrado("Producto no encontrado."));
        products.guardar(new Producto(id,p.nombre(),p.descripcion(),p.categoria(),p.precio(),p.imagenUrl(),false,p.personalizable(),p.stockDisponible()));
    }
}
