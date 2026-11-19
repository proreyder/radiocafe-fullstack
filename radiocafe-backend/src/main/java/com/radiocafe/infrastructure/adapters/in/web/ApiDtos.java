package com.radiocafe.infrastructure.adapters.in.web;
import com.radiocafe.domain.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
public final class ApiDtos {
 private ApiDtos() {}
 public record Register(@NotBlank @Size(max=100) String nombre,@NotBlank @Email @Size(max=150) String correo,@NotBlank @Size(min=8,max=72) String password) {}
 public record Login(@NotBlank @Email String correo,@NotBlank @Size(max=72) String password) {}
 public record UserView(Long idUsuario,String nombre,String correo,Rol role,boolean activo,String telefono) {
  public static UserView of(Usuario u) {return new UserView(u.idUsuario(),u.nombre(),u.correo(),u.rol(),u.activo(),u.telefono());}
 }
 public record CreateUser(@NotBlank @Size(max=100) String nombre,@NotBlank @Email @Size(max=150) String correo,@NotBlank @Size(min=8,max=72) String password,@NotNull Rol rol,@Size(max=20) String telefono) {}
 public record UpdateUser(@NotBlank @Size(max=100) String nombre,@NotNull Rol rol,@NotNull Boolean activo,@Size(max=20) String telefono) {}
 public record StockAdjustment(@NotNull @Min(-100000) @Max(100000) Integer cantidad,@NotBlank @Size(min=5,max=200) String motivo) {}
 public record ProductInput(@NotBlank @Size(max=120) String nombre,@Size(max=2000) String descripcion,
  @NotBlank @Pattern(regexp="CAFE|PASTELERIA|COMBO") String categoria,@NotNull @DecimalMin("0.01") @DecimalMax("99999.99") @Digits(integer=5,fraction=2) BigDecimal precio,
  @Size(max=2000) @Pattern(regexp="^$|https://[^\\s]+") String imagenUrl, Boolean personalizable) {
  public Producto toDomain() {return new Producto(null,nombre,descripcion,categoria,precio,imagenUrl,true,Boolean.TRUE.equals(personalizable));}
 }
 public record PersonalizationInput(@NotBlank @Size(max=30) String tamano,@NotBlank @Size(max=30) String leche,@NotBlank @Size(max=30) String crema,@Size(max=5) List<@NotBlank @Size(max=30) String> extras) {
  public Personalizacion toDomain() { return new Personalizacion(tamano,leche,crema,extras); }
 }
 public record OrderLine(@NotNull @Positive Long idProducto,@NotNull @Min(1) @Max(99) Integer cantidad,@Valid PersonalizationInput personalizacion) {}
 public record OrderInput(@NotNull TipoEntrega tipoEntrega,@Size(max=500) String direccionEntrega,@NotEmpty @Size(max=50) List<@NotNull @Valid OrderLine> detalles) {
  public Pedido toDomain(Long usuario) {var p=new Pedido();p.setIdUsuario(usuario);p.setTipoEntrega(tipoEntrega);p.setDireccionEntrega(direccionEntrega);
   // Product names and prices are exclusively supplied by the catalog use case.
   for(var d:detalles) {var line=new DetallePedido();line.setIdProducto(d.idProducto());line.setCantidad(d.cantidad());if(d.personalizacion()!=null) line.setPersonalizacion(d.personalizacion().toDomain());p.getDetalles().add(line);}return p;}
 }
 public record StatusInput(@NotNull EstadoPedido nuevoEstado) {}
 public record IllustrationInput(@NotBlank @Size(max=150) String tituloObra,@NotBlank @Size(max=2000) @Pattern(regexp="https://[^\\s]+") String imagenUrl) {}
 public record RedeemInput(@NotBlank @Size(max=50) String codigoPremio) {}
 public record IllustrationView(Long idIlustracion,Long idUsuario,String tituloObra,String imagenUrl,Integer votosCount,Boolean esGanador,EstadoPremio estadoPremio,String codigoPremio) {
  public static IllustrationView of(IlustracionKappu i,boolean verCodigo) {return new IllustrationView(i.getIdIlustracion(),i.getIdUsuario(),i.getTituloObra(),i.getImagenUrl(),i.getVotosCount(),i.getEsGanador(),i.getEstadoPremio(),verCodigo?i.getCodigoPremio():null);}
 }
}
