package com.radiocafe.infrastructure.config;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.out.*;
import com.radiocafe.infrastructure.adapters.out.persistence.repository.ConcursoRepository;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.ConcursoEntity;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
@Component
public class DataInitializer implements CommandLineRunner {
 @Value("${app.bootstrap.admin-email:}") private String adminEmail;
 @Value("${app.bootstrap.admin-password:}") private String adminPassword;
 private final InventoryRepositoryPort inventory;
 private final ConcursoRepository contest;private final UserRepositoryPort users;private final ProductRepositoryPort products;private final PasswordPort passwords;private final boolean demo;
 public DataInitializer(InventoryRepositoryPort inventory,ConcursoRepository contest,UserRepositoryPort users,ProductRepositoryPort products,PasswordPort passwords,@Value("${app.demo.enabled}") boolean demo) {this.inventory=inventory;this.contest=contest;this.users=users;this.products=products;this.passwords=passwords;this.demo=demo;}
 @Override @Transactional public void run(String...args) {
  if(!contest.existsById(1L)) contest.save(new ConcursoEntity());
  if(!adminEmail.isBlank() && !users.existePorCorreo(adminEmail.strip().toLowerCase(java.util.Locale.ROOT))) {
   if(adminPassword.length()<12 || adminPassword.length()>72) throw new IllegalArgumentException("ADMIN_PASSWORD debe tener entre 12 y 72 caracteres.");
   users.guardar(new Usuario(null,"Administrador",adminEmail.strip().toLowerCase(java.util.Locale.ROOT),passwords.codificar(adminPassword),Rol.ADMINISTRADOR,null));
  }
  for(var p:products.obtenerTodos()) {
   if(p.stock()==null) {
    int initial=demo?50:0;products.guardar(p.conStock(initial));
    inventory.registrar(new MovimientoInventario(null,p.idProducto(),p.nombre(),initial,initial,"INICIAL",demo?"Saldo inicial de demostración":"Inicio de control de inventario",null,null,java.time.LocalDateTime.now()));
   }
  }
  if(!demo)return;
  account("Administrador","admin@radiocafe.com",Rol.ADMINISTRADOR);account("Barista","barista@radiocafe.com",Rol.BARISTA);account("Repartidor","repartidor@radiocafe.com",Rol.REPARTIDOR);account("Cliente demo","cliente@radiocafe.com",Rol.CLIENTE);
  // Upgrade only demo records that predate the personalization field, preserving later admin choices.
  for (var p : products.obtenerTodosDisponibles()) {
   if (p.personalizable() == null) products.guardar(new Producto(p.idProducto(),p.nombre(),p.descripcion(),p.categoria(),p.precio(),p.imagenUrl(),p.disponible(),p.nombre().equals("Flat White Doble") || p.nombre().equals("Cold Brew Radio"),p.stockDisponible()));
  }
  if(!products.existenProductos()) {
   product("Flat White Doble","Doble espresso con leche texturizada.","CAFE","4.50");product("Cold Brew Radio","Extracción en frío de 18 horas.","CAFE","5.00");product("Croissant Mantequilla","Hojaldre francés recién horneado.","PASTELERIA","3.50");product("Combo Sintonía","Café, sándwich y jugo natural.","COMBO","12.00");product("Café en Grano 250g","Selección especial de Villa Rica.","CAFE","18.00");product("Cheesecake Frutos Rojos","Pastel con mermelada artesanal.","PASTELERIA","6.50");
  }
 }
 private void account(String nombre,String correo,Rol rol) {if(!users.existePorCorreo(correo)) users.guardar(new Usuario(null,nombre,correo,passwords.codificar("RadioCafe2026!"),rol,null));}
 private void product(String nombre,String desc,String categoria,String precio) {
  var p=products.guardar(new Producto(null,nombre,desc,categoria,new BigDecimal(precio),"",true,nombre.equals("Flat White Doble") || nombre.equals("Cold Brew Radio"),50));
  inventory.registrar(new MovimientoInventario(null,p.idProducto(),p.nombre(),50,50,"INICIAL","Saldo inicial de demostración",null,null,java.time.LocalDateTime.now()));
 }
}
