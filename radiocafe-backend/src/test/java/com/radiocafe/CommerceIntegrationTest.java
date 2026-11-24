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
import java.time.LocalDateTime;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class CommerceIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired SpringDataProductoRepository products;
 @Autowired SpringDataOrderRepository orders;
 @Autowired MovimientoInventarioRepository movements;
 @Autowired CampanaRepository campaigns;
 @Autowired OpinionRepository reviews;
 @Autowired ConsultaRepository support;
 @Autowired TiendaRepository shop;
 private MockHttpSession admin,client;private Long product;
 @BeforeEach void setup() throws Exception {
  orders.deleteAll();movements.deleteAll();campaigns.deleteAll();reviews.deleteAll();support.deleteAll();shop.deleteAll();
  var p=new ProductoEntity();p.setNombre("Café comercio "+UUID.randomUUID());p.setCategoria("CAFE");p.setPrecio(new BigDecimal("10.00"));p.setStock(10);p.setDisponible(true);p.setPersonalizable(false);product=products.save(p).getIdProducto();
  admin=login("admin@radiocafe.com");client=login("cliente@radiocafe.com");
 }
 @AfterEach void cleanup() {campaigns.deleteAll();reviews.deleteAll();support.deleteAll();shop.deleteAll();}
 private MockHttpSession login(String email) throws Exception {return (MockHttpSession)mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\""+email+"\",\"password\":\"RadioCafe2026!\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
 private MvcResult send(String path,String body,MockHttpSession session,int status) throws Exception {return mvc.perform(post(path).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().is(status)).andReturn();}
 private long id(MvcResult result,String field) throws Exception {return ((Number)JsonPath.read(result.getResponse().getContentAsString(),field)).longValue();}
 private String campaign(String code,int percent,int minimum,LocalDateTime start,LocalDateTime end) {return "{\"titulo\":\"Pausa especial\",\"mensaje\":\"Promoción para disfrutar\",\"codigo\":"+(code==null?"null":"\""+code+"\"")+",\"porcentaje\":"+percent+",\"minimo\":"+minimum+",\"inicio\":\""+start+"\",\"fin\":\""+end+"\",\"activa\":true}";}
 private String campaign(String code,int percent,int minimum) {return campaign(code,percent,minimum,LocalDateTime.now().minusDays(1),LocalDateTime.now().plusDays(1));}
 private long campaignId(String code,int percent,int minimum) throws Exception {return id(send("/api/v1/commerce/campaigns",campaign(code,percent,minimum),admin,201),"$.id");}
 private String order(String code) {return "{\"tipoEntrega\":\"PICKUP\",\"codigoCupon\":"+(code==null?"null":"\""+code+"\"")+",\"descuento\":9999,\"subtotal\":1,\"detalles\":[{\"idProducto\":"+product+",\"cantidad\":2}]}";}
 @Test void catalogCanCreateAndEditStockWithLedger() throws Exception {
  String body="{\"nombre\":\"Pan nuevo\",\"categoria\":\"PASTELERIA\",\"precio\":3,\"stock\":7}";
  var created=send("/api/v1/products",body,admin,201);long target=id(created,"$.idProducto");
  assertThat(products.findById(target).orElseThrow().getStock()).isEqualTo(7);
  mvc.perform(put("/api/v1/products/"+target).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body.replace("\"stock\":7","\"stock\":10,\"stockAnterior\":7"))).andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(10));
  var log=movements.findTop200ByIdProductoOrderByIdDesc(target);assertThat(log).hasSize(2);assertThat(log.get(0).getCantidad()).isEqualTo(3);assertThat(log.get(1).getTipo()).isEqualTo("INICIAL");
 }
 @Test void staleStockEditConflictsAndMetadataOnlyPreservesLatestStock() throws Exception {
  var p=products.findById(product).orElseThrow();p.setStock(9);products.save(p);
  String body="{\"nombre\":\"Nombre editado\",\"categoria\":\"CAFE\",\"precio\":12,\"stock\":5,\"stockAnterior\":10}";
  mvc.perform(put("/api/v1/products/"+product).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
  assertThat(products.findById(product).orElseThrow().getNombre()).startsWith("Café comercio");
  mvc.perform(put("/api/v1/products/"+product).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body.replace("\"stock\":5","\"stock\":10"))).andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(9));
 }
 @Test void stockValidationAndAuthorization() throws Exception {
  String body="{\"nombre\":\"Inválido\",\"categoria\":\"CAFE\",\"precio\":2,\"stock\":-1}";send("/api/v1/products",body,admin,400);send("/api/v1/products",body.replace(":-1",":5"),client,403);
 }
 @Test void couponRecalculatesServerPricesAndPersistsHistoricalDiscount() throws Exception {
  long campaign=campaignId("CAFE10",10,10);
  var result=send("/api/v1/orders",order("cafe10"),client,201);long target=id(result,"$.idPedido");
  assertThat((Number)JsonPath.read(result.getResponse().getContentAsString(),"$.total")).extracting(Number::doubleValue).isEqualTo(18.0);
  mvc.perform(get("/api/v1/orders/"+target).session(client)).andExpect(jsonPath("$.subtotal").value(20)).andExpect(jsonPath("$.descuento").value(2)).andExpect(jsonPath("$.codigoCupon").value("CAFE10"));
  mvc.perform(put("/api/v1/commerce/campaigns/"+campaign).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(campaign("CAFE10",50,10))).andExpect(status().isOk());
  mvc.perform(get("/api/v1/orders/"+target).session(client)).andExpect(jsonPath("$.total").value(18));
  mvc.perform(patch("/api/v1/orders/"+target+"/status").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nuevoEstado\":\"CANCELLED\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.descuento").value(2));
  assertThat(products.findById(product).orElseThrow().getStock()).isEqualTo(10);
 }
 @Test void bestAutomaticPromotionAndCouponDoNotStack() throws Exception {
  campaignId(null,5,0);campaignId(null,20,0);campaignId("CUPON10",10,0);
  var auto=send("/api/v1/commerce/quote","{\"subtotal\":20}",client,200);assertThat((Number)JsonPath.read(auto.getResponse().getContentAsString(),"$.importe")).extracting(Number::doubleValue).isEqualTo(4.0);
  mvc.perform(post("/api/v1/commerce/quote").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"subtotal\":20,\"codigo\":\"cupon10\"}")).andExpect(jsonPath("$.importe").value(2));
  var result=send("/api/v1/orders",order(null),client,201);assertThat((Number)JsonPath.read(result.getResponse().getContentAsString(),"$.total")).extracting(Number::doubleValue).isEqualTo(16.0);
 }
 @Test void invalidCouponRollsBackStockOrderAndInventory() throws Exception {
  send("/api/v1/orders",order("NOEXISTE"),client,409);assertThat(products.findById(product).orElseThrow().getStock()).isEqualTo(10);assertThat(orders.count()).isZero();assertThat(movements.count()).isZero();
 }
 @Test void datesMinimumInactiveAndDiscountBoundsAreValidated() throws Exception {
  campaignId("MINIMO",10,50);send("/api/v1/orders",order("MINIMO"),client,409);
  send("/api/v1/commerce/campaigns",campaign("VENCIDO",10,0,LocalDateTime.now().minusDays(2),LocalDateTime.now().minusDays(1)),admin,201);send("/api/v1/orders",order("VENCIDO"),client,409);
  send("/api/v1/commerce/campaigns",campaign("FUTURO",10,0,LocalDateTime.now().plusDays(1),LocalDateTime.now().plusDays(2)),admin,201);send("/api/v1/orders",order("FUTURO"),client,409);
  send("/api/v1/commerce/campaigns",campaign("INACTIVO",10,0).replace("true}","false}"),admin,201);send("/api/v1/orders",order("INACTIVO"),client,409);
  send("/api/v1/commerce/campaigns",campaign("MAL",101,0),admin,400);
  send("/api/v1/commerce/campaigns",campaign("FECHAS",10,0,LocalDateTime.now(),LocalDateTime.now().minusDays(1)),admin,400);
  mvc.perform(get("/api/v1/commerce/campaigns")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
 }
 @Test void duplicateCouponConflictsAndOneHundredPercentNeverDiscountsShipping() throws Exception {
  campaignId("GRATIS",100,0);send("/api/v1/commerce/campaigns",campaign("gratis",50,0),admin,409);
  var result=send("/api/v1/orders",order("GRATIS").replace("PICKUP","DELIVERY").replace("\"descuento\":9999","\"direccionEntrega\":\"Calle de prueba 123\",\"descuento\":9999"),client,201);
  assertThat((Number)JsonPath.read(result.getResponse().getContentAsString(),"$.total")).extracting(Number::doubleValue).isEqualTo(5.0);
 }
 @Test void clientsCanReviewOnceAndAdminsCanModerateWithoutDeleting() throws Exception {
  String url="/api/v1/commerce/products/"+product+"/reviews",body="{\"estrellas\":5,\"comentario\":\"Excelente café\",\"usuario\":999}";
  long review=id(send(url,body,client,201),"$.id");send(url,body,client,409);
  mvc.perform(get("/api/v1/commerce/reviews").param("producto",product.toString())).andExpect(jsonPath("$[0].autor").value("Cliente demo"));
  mvc.perform(patch("/api/v1/commerce/reviews/"+review).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"visible\":false}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/commerce/reviews").param("producto",product.toString())).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(get("/api/v1/commerce/admin/reviews").session(admin)).andExpect(jsonPath("$.length()").value(1));
  assertThat(reviews.findById(review).orElseThrow().getComentario()).isEqualTo("Excelente café");
 }
 @Test void reviewPermissionsAndRatingValidation() throws Exception {
  String path="/api/v1/commerce/products/"+product+"/reviews";
  send(path,"{\"estrellas\":6,\"comentario\":\"Prueba\"}",client,400);send(path,"{\"estrellas\":5,\"comentario\":\"Prueba\"}",admin,403);
 }
 @Test void supportIsPrivateAndOnlyAdminCanReply() throws Exception {
  long ticket=id(send("/api/v1/commerce/support","{\"asunto\":\"Mi pedido\",\"mensaje\":\"Necesito ayuda con el recojo\"}",client,201),"$.id");
  String email="privacy-"+UUID.randomUUID()+"@example.com";send("/api/v1/auth/register","{\"nombre\":\"Otra cuenta\",\"correo\":\""+email+"\",\"password\":\"RadioCafe2026!\"}",admin,201);
  var other=login(email);mvc.perform(get("/api/v1/commerce/support").session(other)).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(patch("/api/v1/commerce/support/"+ticket).session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"respuesta\":\"Falsa\"}")).andExpect(status().isForbidden());
  mvc.perform(patch("/api/v1/commerce/support/"+ticket).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"respuesta\":\"Te atenderemos en el local\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/commerce/support").session(client)).andExpect(jsonPath("$[0].estado").value("RESPONDIDA")).andExpect(jsonPath("$[0].respuesta").value("Te atenderemos en el local"));
 }
 @Test void shopConfigurationIsPublicButOnlyAdminCanChangeIt() throws Exception {
  String body="{\"nombre\":\"RadioCafé\",\"tipoComercio\":\"CAFETERIA_PASTELERIA\",\"descripcion\":\"Café y pan artesanal\",\"horario\":\"Lunes a sábado de 8 a 20\",\"telefono\":\"999123456\",\"correo\":\"atencion@example.com\"}";
  mvc.perform(put("/api/v1/commerce/shop").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
  mvc.perform(put("/api/v1/commerce/shop").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
  mvc.perform(get("/api/v1/commerce/shop")).andExpect(status().isOk()).andExpect(jsonPath("$.tipoComercio").value("CAFETERIA_PASTELERIA"));
  assertThat(products.findById(product)).isPresent();
 }
 @Test void recommendationsUseClientHistoryAndExcludeUnavailableProducts() throws Exception {
  send("/api/v1/orders",order(null),client,201);
  mvc.perform(get("/api/v1/commerce/recommendations").session(client)).andExpect(status().isOk()).andExpect(jsonPath("$[0].motivo").value("Por tus pedidos anteriores")).andExpect(jsonPath("$[*].producto.stock",everyItem(greaterThan(0))));
 }
 @Test void campaignsAndModerationRequireAdminAndCsrf() throws Exception {
  send("/api/v1/commerce/campaigns",campaign("PROHIBIDO",10,0),client,403);
  mvc.perform(post("/api/v1/commerce/campaigns").session(admin).contentType(MediaType.APPLICATION_JSON).content(campaign("SINCSRF",10,0))).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/commerce/admin/campaigns").session(client)).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/commerce/admin/reviews")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/commerce/support")).andExpect(status().isUnauthorized());
 }
}
