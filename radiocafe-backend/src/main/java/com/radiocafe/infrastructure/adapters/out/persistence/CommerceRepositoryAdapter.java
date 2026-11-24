package com.radiocafe.infrastructure.adapters.out.persistence;
import com.radiocafe.domain.model.Comercio.*;
import com.radiocafe.domain.ports.out.CommerceRepositoryPort;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.*;
import com.radiocafe.infrastructure.adapters.out.persistence.repository.*;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class CommerceRepositoryAdapter implements CommerceRepositoryPort {
 private final CampanaRepository campaigns;private final OpinionRepository reviews;private final ConsultaRepository support;private final TiendaRepository shop;
 public CommerceRepositoryAdapter(CampanaRepository campaigns,OpinionRepository reviews,ConsultaRepository support,TiendaRepository shop) {this.campaigns=campaigns;this.reviews=reviews;this.support=support;this.shop=shop;}
 private Campana view(CampanaEntity e) {return new Campana(e.getId(),e.getTitulo(),e.getMensaje(),e.getCodigo(),e.getPorcentaje(),e.getMinimo(),e.getInicio(),e.getFin(),e.isActiva());}
 private Opinion view(OpinionEntity e) {return new Opinion(e.getId(),e.getProducto(),e.getUsuario(),e.getAutor(),e.getEstrellas(),e.getComentario(),e.isVisible(),e.getFecha());}
 private Consulta view(ConsultaEntity e) {return new Consulta(e.getId(),e.getUsuario(),e.getAutor(),e.getAsunto(),e.getMensaje(),e.getRespuesta(),e.getEstado(),e.getFecha(),e.getRespondida());}
 private Tienda view(TiendaEntity e) {return new Tienda(e.getNombre(),e.getTipoComercio(),e.getDescripcion(),e.getHorario(),e.getTelefono(),e.getCorreo());}
 public List<Campana> campanas() {return campaigns.findAllByOrderByIdDesc().stream().map(this::view).toList();}
 public Optional<Campana> campana(Long id) {return campaigns.findById(id).map(this::view);}
 public Campana guardar(Campana c) {return view(campaigns.save(new CampanaEntity(c.id(),c.titulo(),c.mensaje(),c.codigo(),c.porcentaje(),c.minimo(),c.inicio(),c.fin(),c.activa())));}
 public List<Opinion> opiniones(Long producto,boolean todas) {return (todas?reviews.findAllByOrderByFechaDesc():reviews.findByProductoAndVisibleTrueOrderByFechaDesc(producto)).stream().filter(o->producto==null||producto.equals(o.getProducto())).map(this::view).toList();}
 public Optional<Opinion> opinion(Long id) {return reviews.findById(id).map(this::view);}
 public boolean yaOpino(Long producto,Long usuario) {return reviews.existsByProductoAndUsuario(producto,usuario);}
 public Opinion guardar(Opinion o) {return view(reviews.save(new OpinionEntity(o.id(),o.producto(),o.usuario(),o.autor(),o.estrellas(),o.comentario(),o.visible(),o.fecha())));}
 public List<Consulta> consultas(Long usuario) {return (usuario==null?support.findAllByOrderByFechaDesc():support.findByUsuarioOrderByFechaDesc(usuario)).stream().map(this::view).toList();}
 public Optional<Consulta> consulta(Long id) {return support.findById(id).map(this::view);}
 public Consulta guardar(Consulta c) {return view(support.save(new ConsultaEntity(c.id(),c.usuario(),c.autor(),c.asunto(),c.mensaje(),c.respuesta(),c.estado(),c.fecha(),c.respondida())));}
 public Tienda tienda() {return shop.findById(1L).map(this::view).orElse(new Tienda("RadioCafé","CAFETERIA_PASTELERIA","Café de especialidad, panes y pastelería artesanal. Delivery y recojo en local.","Consulta nuestros horarios desde Atención al cliente.","",""));}
 public Tienda guardar(Tienda t) {return view(shop.save(new TiendaEntity(1L,t.nombre(),t.tipoComercio(),t.descripcion(),t.horario(),t.telefono(),t.correo())));}
}
