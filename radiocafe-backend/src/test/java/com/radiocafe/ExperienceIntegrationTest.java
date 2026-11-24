package com.radiocafe;
import com.jayway.jsonpath.JsonPath;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.*;
import com.radiocafe.infrastructure.adapters.out.persistence.repository.*;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;import org.springframework.mock.web.*;import org.springframework.http.MediaType;
import java.math.BigDecimal;import java.time.*;import java.nio.file.Path;import java.awt.image.BufferedImage;import javax.imageio.ImageIO;import java.io.ByteArrayOutputStream;import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.*;import static org.hamcrest.Matchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Import(ExperienceIntegrationTest.TimeConfig.class)
class ExperienceIntegrationTest {
 static class MutableClock extends Clock {private Instant instant=Instant.parse("2026-11-23T14:00:00Z");public ZoneId getZone(){return ZoneId.of("America/Lima");}public Clock withZone(ZoneId zone){return Clock.fixed(instant,zone);}public Instant instant(){return instant;}void at(String value){instant=Instant.parse(value);}}
 @TestConfiguration static class TimeConfig {@Bean @Primary MutableClock testClock(){return new MutableClock();}}
 @TempDir static Path images;
 @DynamicPropertySource static void imageDirectory(DynamicPropertyRegistry r){r.add("app.images.directory",()->images.toString());}
 @Autowired MockMvc mvc;@Autowired MutableClock clock;
 @Autowired NotificacionRepository notifications;@Autowired BaristaPresenciaRepository presence;
 @Autowired SpringDataOrderRepository orders;@Autowired SpringDataProductoRepository products;@Autowired MovimientoInventarioRepository movements;
 @Autowired SpringDataIllustrationRepository illustrations;@Autowired VotoRepository votes;@Autowired ConcursoRepository contest;
 private MockHttpSession admin,client,barista;private Long product;
 @BeforeEach void setup() throws Exception {
  clock.at("2026-11-23T14:00:00Z");notifications.deleteAll();presence.deleteAll();orders.deleteAll();movements.deleteAll();votes.deleteAll();illustrations.deleteAll();contest.deleteAll();contest.save(new ConcursoEntity());
  var p=new ProductoEntity();p.setNombre("Café experiencia");p.setCategoria("CAFE");p.setPrecio(BigDecimal.TEN);p.setStock(20);p.setDisponible(true);p.setPersonalizable(false);product=products.save(p).getIdProducto();
  admin=login("admin@radiocafe.com");client=login("cliente@radiocafe.com");barista=login("barista@radiocafe.com");
 }
 @AfterEach void cleanup(){notifications.deleteAll();presence.deleteAll();votes.deleteAll();illustrations.deleteAll();contest.deleteAll();contest.save(new ConcursoEntity());}
 MockHttpSession login(String email)throws Exception{return (MockHttpSession)mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\""+email+"\",\"password\":\"RadioCafe2026!\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
 MvcResult postJson(String path,String body,MockHttpSession user,int code)throws Exception{return mvc.perform(post(path).session(user).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().is(code)).andReturn();}
 MvcResult patchJson(String path,String body,MockHttpSession user,int code)throws Exception{return mvc.perform(patch(path).session(user).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().is(code)).andReturn();}
 long id(MvcResult result,String field)throws Exception{return ((Number)JsonPath.read(result.getResponse().getContentAsString(),field)).longValue();}
 long order()throws Exception{return id(postJson("/api/v1/orders","{\"tipoEntrega\":\"PICKUP\",\"detalles\":[{\"idProducto\":"+product+",\"cantidad\":1}]}",client,201),"$.idPedido");}
 void available()throws Exception{patchJson("/api/v1/barista/availability","{\"disponible\":true}",barista,200);}
 String entry(int index){return "{\"tituloObra\":\"Kappu arte "+index+"\",\"nombreParticipante\":\"Artista "+index+"\",\"descripcion\":\"Un zorrito inspirado en el café\",\"imagenUrl\":\"https://example.com/fox.png\"}";}
 long entryId(int i)throws Exception{return id(postJson("/api/v1/kappu/entries",entry(i),client,201),"$.idIlustracion");}
 byte[] png()throws Exception{var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(24,24,BufferedImage.TYPE_INT_RGB),"png",out);return out.toByteArray();}
 @Test void availabilityRespectsLimaHoursAndRequiresANewShiftEachDay()throws Exception{
  clock.at("2026-11-23T10:59:00Z");patchJson("/api/v1/barista/availability","{\"disponible\":true}",barista,409);
  clock.at("2026-11-23T11:00:00Z");available();mvc.perform(get("/api/v1/barista/me").session(barista)).andExpect(jsonPath("$.disponible").value(true));
  clock.at("2026-11-24T01:29:00Z");mvc.perform(get("/api/v1/barista/me").session(barista)).andExpect(jsonPath("$.disponible").value(true));
  clock.at("2026-11-24T01:30:00Z");mvc.perform(get("/api/v1/barista/me").session(barista)).andExpect(jsonPath("$.disponible").value(false));
  clock.at("2026-11-24T11:00:00Z");mvc.perform(get("/api/v1/barista/me").session(barista)).andExpect(jsonPath("$.disponible").value(false));
 }
 @Test void onlyBaristaCanSetTheirAvailability()throws Exception{
  patchJson("/api/v1/barista/availability","{\"disponible\":true}",client,403);
  available();mvc.perform(get("/api/v1/baristas").session(client)).andExpect(status().isOk()).andExpect(jsonPath("$[0].disponible").value(true));
  patchJson("/api/v1/barista/availability","{\"disponible\":false}",barista,200);mvc.perform(get("/api/v1/baristas").session(client)).andExpect(jsonPath("$[0].disponible").value(false));
 }
 @Test void estimateStartsPreparationPersistsAndNotifiesOnlyCustomer()throws Exception{
  long target=order();available();
  var response=patchJson("/api/v1/orders/"+target+"/preparation","{\"minutos\":15}",barista,200);
  assertThat((String)JsonPath.read(response.getResponse().getContentAsString(),"$.estadoPedido")).isEqualTo("EN_PREPARACION");
  mvc.perform(get("/api/v1/orders/"+target).session(client)).andExpect(jsonPath("$.minutosPreparacion").value(15)).andExpect(jsonPath("$.preparacionEstimada").value("2026-11-23T14:15:00Z")).andExpect(jsonPath("$.nombreBarista").value("Barista"));
  mvc.perform(get("/api/v1/notifications").session(client)).andExpect(jsonPath("$.noLeidas").value(1)).andExpect(jsonPath("$.elementos[0].tipo").value("PEDIDO"));
  mvc.perform(get("/api/v1/notifications").session(admin)).andExpect(jsonPath("$.noLeidas").value(0));
 }
 @Test void unavailableBaristaClientAndInvalidMinutesCannotEstimate()throws Exception{
  long target=order();patchJson("/api/v1/orders/"+target+"/preparation","{\"minutos\":15}",barista,409);
  patchJson("/api/v1/orders/"+target+"/preparation","{\"minutos\":15}",client,403);available();
  for(int minutes:new int[]{0,241})patchJson("/api/v1/orders/"+target+"/preparation","{\"minutos\":"+minutes+"}",barista,400);
  mvc.perform(get("/api/v1/orders/"+target).session(client)).andExpect(jsonPath("$.estadoPedido").value("PENDIENTE"));
 }
 @Test void orderStateChangesNotifyClientAndKeepEstimateSnapshot()throws Exception{
  long target=order();available();patchJson("/api/v1/orders/"+target+"/preparation","{\"minutos\":12}",barista,200);
  patchJson("/api/v1/orders/"+target+"/status","{\"nuevoEstado\":\"LISTO\"}",barista,200);patchJson("/api/v1/orders/"+target+"/status","{\"nuevoEstado\":\"ENTREGADO\"}",barista,200);
  patchJson("/api/v1/orders/"+target+"/preparation","{\"minutos\":10}",barista,409);
  mvc.perform(get("/api/v1/notifications").session(client)).andExpect(jsonPath("$.noLeidas").value(3));mvc.perform(get("/api/v1/orders/"+target).session(client)).andExpect(jsonPath("$.minutosPreparacion").value(12));
 }
 @Test void supplyAlertArrivesOnlyToAdminsAndCanOnlyBeReadByOwner()throws Exception{
  postJson("/api/v1/barista/shortages","{\"insumo\":\"Leche\",\"cantidad\":3,\"unidad\":\"litros\",\"detalle\":\"Falta para preparar los pedidos\"}",barista,204);
  var box=mvc.perform(get("/api/v1/notifications").session(admin)).andExpect(jsonPath("$.noLeidas").value(1)).andExpect(jsonPath("$.elementos[0].autor").value("Barista")).andReturn();long notice=id(box,"$.elementos[0].id");
  mvc.perform(patch("/api/v1/notifications/"+notice+"/read").session(client).with(csrf())).andExpect(status().isNotFound());
  mvc.perform(patch("/api/v1/notifications/"+notice+"/read").session(admin).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get("/api/v1/notifications").session(admin)).andExpect(jsonPath("$.noLeidas").value(0));mvc.perform(get("/api/v1/notifications").session(client)).andExpect(jsonPath("$.noLeidas").value(0));
 }
 @Test void notificationReadAllAndSupplyPermissionsAreProtected()throws Exception{
  postJson("/api/v1/barista/shortages","{\"insumo\":\"Café\",\"cantidad\":1,\"unidad\":\"kg\",\"detalle\":\"Reponer para el turno\"}",client,403);
  postJson("/api/v1/barista/shortages","{\"insumo\":\"Café\",\"cantidad\":0,\"unidad\":\"kg\",\"detalle\":\"Reponer para el turno\"}",barista,400);
  mvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
  mvc.perform(patch("/api/v1/notifications/read-all").session(admin)).andExpect(status().isForbidden());
  mvc.perform(patch("/api/v1/notifications/read-all").session(admin).with(csrf())).andExpect(status().isNoContent());
 }
 @Test void clientCanUploadImageEnrollAndNotifyAdminWithoutMixingProductPhotos()throws Exception{
  var uploaded=mvc.perform(multipart("/api/v1/kappu/images").file(new MockMultipartFile("file","kappu.png","image/png",png())).session(client).with(csrf())).andExpect(status().isCreated()).andReturn();
  String url=JsonPath.read(uploaded.getResponse().getContentAsString(),"$.url");assertThat(url).startsWith("/api/v1/kappu/images/files/");
  mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG));
  mvc.perform(get("/api/v1/images").session(admin)).andExpect(jsonPath("$[*].url",not(hasItem(url))));
  postJson("/api/v1/kappu/entries",entry(1).replace("https://example.com/fox.png",url),client,201);
  mvc.perform(get("/api/v1/kappu/gallery")).andExpect(status().isOk()).andExpect(jsonPath("$.ilustraciones[0].nombreParticipante").value("Artista 1")).andExpect(jsonPath("$.ilustraciones[0].descripcion").value("Un zorrito inspirado en el café"));
  mvc.perform(get("/api/v1/notifications").session(admin)).andExpect(jsonPath("$.elementos[0].tipo").value("KAPPU"));
 }
 @Test void enrollmentAndImageValidationRequireClientAndRealPngOrJpeg()throws Exception{
  postJson("/api/v1/kappu/entries",entry(1),admin,403);postJson("/api/v1/kappu/entries",entry(1).replace("Artista 1",""),client,400);
  mvc.perform(multipart("/api/v1/kappu/images").file(new MockMultipartFile("file","fake.png","image/png","<svg/>".getBytes())).session(client).with(csrf())).andExpect(status().isBadRequest());
  mvc.perform(multipart("/api/v1/kappu/images").file(new MockMultipartFile("file","kappu.png","image/png",png())).session(admin).with(csrf())).andExpect(status().isForbidden());
 }
 @Test void contestDatesRejectEarlyEntriesAndAutomaticallyCloseAtDeadline()throws Exception{
  String dates="{\"inicio\":\"2026-11-23T10:00:00\",\"fin\":\"2026-11-23T11:00:00\"}";
  mvc.perform(put("/api/v1/kappu/schedule").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(dates)).andExpect(status().isOk()).andExpect(jsonPath("$.fase").value("PROXIMAMENTE"));
  postJson("/api/v1/kappu/entries",entry(1),client,409);clock.at("2026-11-23T15:00:00Z");long target=entryId(1);
  clock.at("2026-11-23T16:00:00Z");mvc.perform(get("/api/v1/kappu/status")).andExpect(jsonPath("$.cerrado").value(true)).andExpect(jsonPath("$.finalistas[0]").value(target));postJson("/api/v1/kappu/entries",entry(2),client,409);
 }
 @Test void closingFreezesThreeTopLikesAndOnlyFinalistCanWinPublicly()throws Exception{
  var ids=new ArrayList<Long>();for(int i=0;i<5;i++)ids.add(entryId(i));int[] likes={5,8,8,1,0};for(int i=0;i<ids.size();i++){var e=illustrations.findById(ids.get(i)).orElseThrow();e.setVotosCount(likes[i]);illustrations.save(e);}
  postJson("/api/v1/kappu/select-winner/"+ids.get(1),"{}",admin,409);
  postJson("/api/v1/kappu/close","{}",admin,200);
  assertThat(illustrations.findAll().stream().filter(i->Boolean.TRUE.equals(i.getEsFinalista())).map(IlustracionKappuEntity::getIdIlustracion).toList()).containsExactlyInAnyOrder(ids.get(0),ids.get(1),ids.get(2));
  postJson("/api/v1/kappu/select-winner/"+ids.get(3),"{}",admin,409);postJson("/api/v1/kappu/"+ids.get(3)+"/votes","{}",client,409);postJson("/api/v1/kappu/entries",entry(9),client,409);
  postJson("/api/v1/kappu/select-winner/"+ids.get(1),"{}",admin,200);
  mvc.perform(get("/api/v1/kappu/gallery")).andExpect(jsonPath("$.estado.fase").value("GANADOR")).andExpect(jsonPath("$.ilustraciones[?(@.esGanador==true)].codigoPremio",everyItem(nullValue())));
  mvc.perform(get("/api/v1/kappu").session(client)).andExpect(jsonPath("$[?(@.esGanador==true)].codigoPremio",everyItem(startsWith("KAPPU-WIN-"))));
  postJson("/api/v1/kappu/select-winner/"+ids.get(2),"{}",admin,409);
 }
 @Test void likePersistsAndContestControlsRequireAdmin()throws Exception{
  long target=entryId(1);postJson("/api/v1/kappu/"+target+"/votes","{}",client,200);mvc.perform(get("/api/v1/kappu/my-vote").session(client)).andExpect(jsonPath("$.votoRegistrado").value(true));postJson("/api/v1/kappu/"+target+"/votes","{}",client,409);
  postJson("/api/v1/kappu/close","{}",client,403);mvc.perform(post("/api/v1/kappu/close").session(admin)).andExpect(status().isForbidden());
 }
}
