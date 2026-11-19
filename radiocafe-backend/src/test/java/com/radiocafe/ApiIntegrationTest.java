package com.radiocafe;
import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.*;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.ConcursoEntity;
import com.radiocafe.infrastructure.adapters.out.persistence.repository.*;
import com.jayway.jsonpath.JsonPath;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ApiIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired SpringDataOrderRepository orders;
 @Autowired SpringDataIllustrationRepository illustrations;
 @Autowired SpringDataProductoRepository products;
 @Autowired VotoRepository votes;
 @Autowired ConcursoRepository contest;
 @Autowired SelectWinnerUseCase selectWinner;
 @Autowired RedeemPrizeUseCase redeemPrize;
 private MockHttpSession client,admin;
 private long product;
 private String orderBody;
 private static final String PASSWORD="RadioCafe2026!";
 @BeforeEach void setup() throws Exception {
  orders.deleteAll();votes.deleteAll();illustrations.deleteAll();contest.deleteAll();contest.save(new ConcursoEntity());
  client=login("cliente@radiocafe.com");admin=login("admin@radiocafe.com");
  var p=products.findByDisponibleTrue().get(0);p.setPrecio(new java.math.BigDecimal("4.50"));p.setStock(100);products.save(p);product=p.getIdProducto();
  orderBody="{\"tipoEntrega\":\"DELIVERY\",\"direccionEntrega\":\"Av. Lima 123\",\"detalles\":[{\"idProducto\":"+product+",\"cantidad\":2}]}";
 }
 private MockHttpSession login(String email) throws Exception {
  var result=mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\""+email+"\",\"password\":\""+PASSWORD+"\"}")).andExpect(status().isOk()).andReturn();
  return (MockHttpSession)result.getRequest().getSession(false);
 }
 private long id(MvcResult r,String path) throws Exception {return ((Number)JsonPath.read(r.getResponse().getContentAsString(),path)).longValue();}
 private long createOrder() throws Exception {return id(mvc.perform(post("/api/v1/orders").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(orderBody)).andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(14)).andExpect(jsonPath("$.detalles[0].cantidad").value(2)).andReturn(),"$.idPedido");}
 private long participate() throws Exception {return id(mvc.perform(post("/api/v1/kappu").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"tituloObra\":\"Kappu Café\",\"imagenUrl\":\"https://example.com/kappu.png\"}")).andExpect(status().isCreated()).andReturn(),"$.idIlustracion");}
 private void state(long id,String state,MockHttpSession session,int expected) throws Exception {mvc.perform(patch("/api/v1/orders/"+id+"/status").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nuevoEstado\":\""+state+"\"}")).andExpect(status().is(expected));}
 @Test void apiRequiresLoginAndCsrf() throws Exception {
  mvc.perform(get("/api/v1/orders")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/v1/orders").session(client).contentType(MediaType.APPLICATION_JSON).content(orderBody)).andExpect(status().isForbidden());
 }
 @Test void actualCsrfTokenAndSessionSupportLoginAndLogout() throws Exception {
  var r=mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
  String token=JsonPath.read(r.getResponse().getContentAsString(),"$.token");
  var session=(MockHttpSession)r.getRequest().getSession(false);
  var logged=mvc.perform(post("/api/v1/auth/login").session(session).header("X-CSRF-TOKEN",token).contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"cliente@radiocafe.com\",\"password\":\"RadioCafe2026!\"}")).andExpect(status().isOk()).andReturn();
  var authenticated=(MockHttpSession)logged.getRequest().getSession(false);
  mvc.perform(get("/api/v1/auth/me").session(authenticated)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("CLIENTE")).andExpect(jsonPath("$.password").doesNotExist());
  mvc.perform(post("/api/v1/auth/logout").session(authenticated).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
 }
 @Test void rejectsWrongPassword() throws Exception {mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"cliente@radiocafe.com\",\"password\":\"incorrecta\"}")).andExpect(status().isUnauthorized());}
 @Test void registrationEnforcesValidationUniquenessAndClientRole() throws Exception {
  String email="test-"+UUID.randomUUID()+"@example.com";
  String body="{\"nombre\":\"Nuevo Cliente\",\"correo\":\""+email+"\",\"password\":\"RadioCafe2026!\",\"role\":\"ADMINISTRADOR\"}";
  mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("CLIENTE")).andExpect(jsonPath("$.password").doesNotExist());
  mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
  mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\" \"}")).andExpect(status().isBadRequest());
 }
 @Test void clientCannotModifyCatalogOrSelectWinner() throws Exception {
  mvc.perform(post("/api/v1/products").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Café\",\"categoria\":\"CAFE\",\"precio\":4.5}")).andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/kappu/select-winner/1").session(client).with(csrf())).andExpect(status().isForbidden());
 }
 @Test void adminCanCreateEditAndRetireProduct() throws Exception {
  var result=mvc.perform(post("/api/v1/products").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Nuevo Café\",\"descripcion\":\"Especial\",\"categoria\":\"CAFE\",\"precio\":8.25,\"imagenUrl\":\"\"}")).andExpect(status().isCreated()).andReturn();
  long id=id(result,"$.idProducto");
  mvc.perform(put("/api/v1/products/"+id).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Café actualizado\",\"categoria\":\"CAFE\",\"precio\":9.5}")).andExpect(status().isOk()).andExpect(jsonPath("$.precio").value(9.5));
  mvc.perform(delete("/api/v1/products/"+id).session(admin).with(csrf())).andExpect(status().isNoContent());assertThat(products.findById(id).orElseThrow().getDisponible()).isFalse();
 }
 @Test void orderKeepsDetailsAndOriginalPricesThroughEveryState() throws Exception {
  long id=createOrder();var p=products.findById(product).orElseThrow();p.setPrecio(new java.math.BigDecimal("99.00"));products.save(p);
  state(id,"EN_PREPARACION",admin,200);state(id,"LISTO",admin,200);state(id,"EN_CAMINO",admin,200);state(id,"ENTREGADO",admin,200);
  mvc.perform(get("/api/v1/orders/"+id).session(client)).andExpect(status().isOk()).andExpect(jsonPath("$.detalles.length()").value(1)).andExpect(jsonPath("$.detalles[0].precioUnitario").value(4.5)).andExpect(jsonPath("$.subtotal").value(9)).andExpect(jsonPath("$.total").value(14));
  state(id,"PENDIENTE",admin,409);assertThat(orders.findById(id).orElseThrow().getDetalles()).hasSize(1);
 }
 @Test void missingAddressInvalidQuantityAndUnknownProductAreRejected() throws Exception {
  mvc.perform(post("/api/v1/orders").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(orderBody.replace("Av. Lima 123",""))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/v1/orders").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(orderBody.replace("cantidad\":2","cantidad\":0"))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/v1/orders").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(orderBody.replace("idProducto\":"+product,"idProducto\":99999999"))).andExpect(status().isNotFound());
 }
 @Test void nullOrderLineIsRejectedAsValidationError() throws Exception {
  mvc.perform(post("/api/v1/orders").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"tipoEntrega\":\"PICKUP\",\"detalles\":[null]}" )).andExpect(status().isBadRequest());
 }
 @Test void clientCannotForgePriceOwnerOrState() throws Exception {
  String tampered=orderBody.replace("\"tipoEntrega\"","\"idUsuario\":99999,\"estadoPedido\":\"ENTREGADO\",\"tipoEntrega\"").replace("\"cantidad\":2","\"cantidad\":2,\"precioUnitario\":0.01");
  mvc.perform(post("/api/v1/orders").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(tampered)).andExpect(status().isCreated()).andExpect(jsonPath("$.estadoPedido").value("PENDIENTE")).andExpect(jsonPath("$.total").value(14)).andExpect(jsonPath("$.idUsuario").value(userId(client)));
 }
 private long userId(MockHttpSession session) throws Exception {return id(mvc.perform(get("/api/v1/auth/me").session(session)).andReturn(),"$.idUsuario");}
 @Test void customersCannotReadSomeoneElsesOrder() throws Exception {
  long id=createOrder();String email="other-"+UUID.randomUUID()+"@example.com";
  mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Otro\",\"correo\":\""+email+"\",\"password\":\"RadioCafe2026!\"}")).andExpect(status().isCreated());var other=login(email);
  mvc.perform(get("/api/v1/orders/"+id).session(other)).andExpect(status().isForbidden());mvc.perform(get("/api/v1/orders").session(other)).andExpect(jsonPath("$.length()").value(0));
 }
 @Test void rolesRestrictKitchenAndDeliveryOperations() throws Exception {
  long id=createOrder();var barista=login("barista@radiocafe.com");var driver=login("repartidor@radiocafe.com");
  state(id,"EN_PREPARACION",driver,403);state(id,"EN_PREPARACION",barista,200);state(id,"LISTO",barista,200);state(id,"EN_CAMINO",barista,403);state(id,"EN_CAMINO",driver,200);state(id,"ENTREGADO",barista,403);state(id,"ENTREGADO",driver,200);
 }
 @Test void contestSupportsVoteWinnerReadableQrAndSingleRedemption() throws Exception {
  long id=participate();mvc.perform(post("/api/v1/kappu/"+id+"/votes").session(client).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.votosCount").value(1));
  mvc.perform(post("/api/v1/kappu/"+id+"/votes").session(client).with(csrf())).andExpect(status().isConflict());
  var result=mvc.perform(post("/api/v1/kappu/select-winner/"+id).session(admin).with(csrf())).andExpect(status().isOk()).andReturn();String code=JsonPath.read(result.getResponse().getContentAsString(),"$.codigoPremio");
  byte[] png=mvc.perform(get("/api/v1/kappu/"+id+"/qr").session(client)).andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG)).andReturn().getResponse().getContentAsByteArray();
  var bitmap=new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(ImageIO.read(new ByteArrayInputStream(png)))));assertThat(new MultiFormatReader().decode(bitmap).getText()).isEqualTo(code);
  mvc.perform(post("/api/v1/kappu/select-winner/"+id).session(admin).with(csrf())).andExpect(status().isConflict());
  mvc.perform(post("/api/v1/kappu/"+id+"/votes").session(client).with(csrf())).andExpect(status().isConflict());
  String body="{\"codigoPremio\":\""+code+"\"}";
  mvc.perform(post("/api/v1/kappu/redeem-prize").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/kappu/redeem-prize").session(login("barista@radiocafe.com")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.estadoPremio").value("RECLAMADO"));
  mvc.perform(post("/api/v1/kappu/redeem-prize").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
 }
 @Test void prizeCodesArePrivateToOwnerAndCashier() throws Exception {
  long id=participate();selectWinner.seleccionarGanador(id);var driver=login("repartidor@radiocafe.com");
  mvc.perform(get("/api/v1/kappu").session(driver)).andExpect(status().isOk()).andExpect(jsonPath("$[0].codigoPremio").isEmpty());
  mvc.perform(get("/api/v1/kappu/"+id+"/qr").session(driver)).andExpect(status().isForbidden());
 }
 private List<Boolean> race(Callable<Boolean> action) throws Exception {
  var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
  Callable<Boolean> task=()->{start.await();return action.call();};
  try {var a=pool.submit(task);var b=pool.submit(task);start.countDown();return List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));} finally {pool.shutdownNow();}
 }
 @Test void concurrentWinnerSelectionOnlySucceedsOnce() throws Exception {
  long id=participate();var results=race(()->{try {selectWinner.seleccionarGanador(id);return true;}catch(BusinessException e){return false;}});assertThat(results).containsExactlyInAnyOrder(true,false);
 }
 @Test void concurrentRedemptionOnlySucceedsOnce() throws Exception {
  long id=participate();String code=selectWinner.seleccionarGanador(id).getCodigoPremio();var results=race(()->{try {redeemPrize.canjearPremioPorCodigo(code);return true;}catch(BusinessException e){return false;}});assertThat(results).containsExactlyInAnyOrder(true,false);
 }

 @Test void personalizationOptionsComeFromServer() throws Exception {
  mvc.perform(get("/api/v1/products/options").session(client)).andExpect(status().isOk())
   .andExpect(jsonPath("$.tamanos[1].id").value("GRANDE")).andExpect(jsonPath("$.tamanos[1].recargo").value(1))
   .andExpect(jsonPath("$.maxExtras").value(5));
 }
 private String customOrder(String size,String milk,String extras) {
  return """
   {"tipoEntrega":"PICKUP","detalles":[{"idProducto":%d,"cantidad":2,"precioUnitario":0.01,
    "personalizacion":{"tamano":"%s","leche":"%s","crema":"SIN_CREMA","extras":%s}}]}
   """.formatted(product,size,milk,extras);
 }
 @Test void personalizedPricesAndChoicesPersistThroughStateChanges() throws Exception {
  var p=products.findById(product).orElseThrow();p.setPersonalizable(true);products.save(p);
  String body=customOrder("GRANDE","AVENA","[\"VAINILLA\"]");
  var result=mvc.perform(post("/api/v1/orders").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(13.5)).andExpect(jsonPath("$.detalles[0].precioUnitario").value(6.75))
   .andExpect(jsonPath("$.detalles[0].personalizacionDescripcion").value(org.hamcrest.Matchers.containsString("Bebida de avena"))).andReturn();
  long id=id(result,"$.idPedido");state(id,"EN_PREPARACION",admin,200);
  mvc.perform(get("/api/v1/orders/"+id).session(client)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(13.5))
   .andExpect(jsonPath("$.detalles[0].personalizacionDescripcion").value(org.hamcrest.Matchers.containsString("Jarabe de vainilla")));
 }
 @Test void unknownAndDuplicateCustomizationChoicesAreRejected() throws Exception {
  var p=products.findById(product).orElseThrow();p.setPersonalizable(true);products.save(p);
  for(String body:List.of(customOrder("GIGANTE","ENTERA","[]"),customOrder("ALTO","INVENTADA","[]"),customOrder("ALTO","ENTERA","[\"MOCHA\",\"MOCHA\"]"),customOrder("ALTO","ENTERA","[\"GRATIS\"]")))
   mvc.perform(post("/api/v1/orders").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
 }
 @Test void originalPresentationProductCannotBeCustomized() throws Exception {
  var p=products.findById(product).orElseThrow();p.setPersonalizable(false);products.save(p);
  mvc.perform(post("/api/v1/orders").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(customOrder("ALTO","ENTERA","[]"))).andExpect(status().isBadRequest());
 }
}
