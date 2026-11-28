package com.radiocafe;
import com.jayway.jsonpath.JsonPath;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.ProductoEntity;
import com.radiocafe.infrastructure.adapters.out.persistence.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import java.math.BigDecimal;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class GuestOrderIntegrationTest {
 @Autowired MockMvc mvc;@Autowired SpringDataProductoRepository products;@Autowired SpringDataOrderRepository orders;
 @Autowired GuestOrderAccessRepository access;@Autowired SpringDataUsuarioRepository users;@Autowired MovimientoInventarioRepository movements;
 long product;
 @BeforeEach void setup(){access.deleteAll();orders.deleteAll();movements.deleteAll();var p=new ProductoEntity();p.setNombre("Café invitado");p.setDescripcion("Café de prueba");p.setCategoria("CAFE");p.setPrecio(new BigDecimal("8.50"));p.setDisponible(true);p.setPersonalizable(false);p.setStock(10);product=products.save(p).getIdProducto();}
 String body(){return """
 {"contacto":{"nombre":"Invitada Prueba","telefono":"999123456","correo":"invitada@example.com"},"pedido":{"tipoEntrega":"PICKUP","detalles":[{"idProducto":%d,"cantidad":2,"precioUnitario":0.01}],"idUsuario":1,"total":0.01}}
 """.formatted(product);}
 MvcResult create()throws Exception{return mvc.perform(post("/api/v1/guest/orders").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isCreated()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.pedido.total").value(17.0)).andReturn();}
 long id(MvcResult result)throws Exception{return ((Number)JsonPath.read(result.getResponse().getContentAsString(),"$.pedido.idPedido")).longValue();}
 String token(MvcResult result)throws Exception{return JsonPath.read(result.getResponse().getContentAsString(),"$.seguimientoToken");}
 MockHttpSession admin()throws Exception{var result=mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"admin@radiocafe.com\",\"password\":\"RadioCafe2026!\"}")).andExpect(status().isOk()).andReturn();return (MockHttpSession)result.getRequest().getSession(false);}
 @Test void guestDoesNotCreateAccountUsesServerPricesAndReservesStock()throws Exception{
  long count=users.count();var result=create();var p=orders.findById(id(result)).orElseThrow();
  assertThat(users.count()).isEqualTo(count);assertThat(p.getIdUsuario()).isNull();assertThat(p.getContactoNombre()).isEqualTo("Invitada Prueba");assertThat(p.getContactoTelefono()).isEqualTo("999123456");
  assertThat(products.findById(product).orElseThrow().getStock()).isEqualTo(8);
  assertThat(access.findById(id(result)).orElseThrow().getTokenHash()).hasSize(64).isNotEqualTo(token(result));
 }
 @Test void trackingRequiresExactSecretAndDoesNotExposeContactOrSecretHash()throws Exception{
  var result=create();var path="/api/v1/guest/orders/"+id(result);
  mvc.perform(get(path)).andExpect(status().isNotFound());mvc.perform(get(path).header("X-Order-Token","x".repeat(43))).andExpect(status().isNotFound());
  mvc.perform(get(path).header("X-Order-Token",token(result))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.estadoPedido").value("PENDIENTE")).andExpect(jsonPath("$.metodoPago").value("PAGO_AL_RECOGER")).andExpect(jsonPath("$.contacto").doesNotExist()).andExpect(jsonPath("$.direccionEntrega").doesNotExist()).andExpect(jsonPath("$.tokenHash").doesNotExist()).andExpect(jsonPath("$.seguimientoToken").doesNotExist());
  mvc.perform(get("/api/v1/guest/orders/"+(id(result)+1)).header("X-Order-Token",token(result))).andExpect(status().isNotFound());
 }
 @Test void csrfContactAndDeliveryValidationLeaveStockUnchanged()throws Exception{
  mvc.perform(post("/api/v1/guest/orders").contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isForbidden());
  for(String invalid:new String[]{body().replace("invitada@example.com","malcorreo"),body().replace("999123456","x"),body().replace("Invitada Prueba"," "),body().replace("PICKUP","DELIVERY"),body().replace("\"contacto\":{","\"contacto\":null,\"ignored\":{")})mvc.perform(post("/api/v1/guest/orders").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
  assertThat(orders.count()).isZero();assertThat(products.findById(product).orElseThrow().getStock()).isEqualTo(10);
 }
 @Test void guestCannotAccessPrivateListsChangeStateOrForgeAccount()throws Exception{
  var result=create();mvc.perform(get("/api/v1/orders")).andExpect(status().isUnauthorized());mvc.perform(get("/api/v1/orders/"+id(result)).header("X-Order-Token",token(result))).andExpect(status().isUnauthorized());
  mvc.perform(patch("/api/v1/orders/"+id(result)+"/status").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nuevoEstado\":\"ENTREGADO\"}")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/v1/orders").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"tipoEntrega\":\"PICKUP\",\"detalles\":[{\"idProducto\":"+product+",\"cantidad\":1}]}")).andExpect(status().isUnauthorized());
 }
 @Test void staffCanPrepareGuestOrderTrackingUpdatesAndCancellationRestoresStock()throws Exception{
  var result=create();var session=admin();String path="/api/v1/orders/"+id(result);
  mvc.perform(get("/api/v1/orders").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$[0].contacto.nombre").value("Invitada Prueba"));
  mvc.perform(patch(path+"/preparation").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"minutos\":12}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/guest/orders/"+id(result)).header("X-Order-Token",token(result))).andExpect(status().isOk()).andExpect(jsonPath("$.estadoPedido").value("EN_PREPARACION")).andExpect(jsonPath("$.minutosPreparacion").value(12));
  mvc.perform(patch(path+"/status").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nuevoEstado\":\"CANCELLED\"}")).andExpect(status().isOk());
  assertThat(products.findById(product).orElseThrow().getStock()).isEqualTo(10);assertThat(orders.findById(id(result)).orElseThrow().getContactoNombre()).isEqualTo("Invitada Prueba");
 }
 @Test void publicOptionsAndQuotesWorkButStockAndDiscountRulesRemainEnforced()throws Exception{
  mvc.perform(get("/api/v1/products/options")).andExpect(status().isOk());
  mvc.perform(post("/api/v1/commerce/quote").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"subtotal\":17}")).andExpect(status().isOk());
  mvc.perform(post("/api/v1/guest/orders").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body().replace("\"cantidad\":2","\"cantidad\":11"))).andExpect(status().isConflict());
  mvc.perform(post("/api/v1/guest/orders").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body().replace("\"tipoEntrega\":\"PICKUP\"","\"codigoCupon\":\"INVALIDO\",\"tipoEntrega\":\"PICKUP\""))).andExpect(status().isConflict());
  assertThat(orders.count()).isZero();assertThat(products.findById(product).orElseThrow().getStock()).isEqualTo(10);
 }
}
