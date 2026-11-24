package com.radiocafe;
import com.jayway.jsonpath.JsonPath;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.ProductoEntity;
import com.radiocafe.infrastructure.adapters.out.persistence.repository.*;
import com.radiocafe.infrastructure.config.TabSecurityContextRepository;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;import org.springframework.mock.web.*;import org.springframework.http.MediaType;
import java.math.BigDecimal;import java.util.UUID;import java.nio.file.Path;import java.awt.image.BufferedImage;import java.io.ByteArrayOutputStream;import javax.imageio.ImageIO;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.*;import static org.hamcrest.Matchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AccountIntegrationTest {
 @TempDir static Path media;@DynamicPropertySource static void props(DynamicPropertyRegistry r){r.add("app.images.directory",()->media.toString());}
 @Autowired MockMvc mvc;@Autowired SpringDataUsuarioRepository users;@Autowired SpringDataProductoRepository products;
 @Autowired SpringDataOrderRepository orders;@Autowired MovimientoInventarioRepository movements;
 private MockHttpSession browser;private String adminTab,clientTab,email;private Long account;
 static final String H=TabSecurityContextRepository.HEADER,PASSWORD="RadioCafe2026!";
 @BeforeEach void setup()throws Exception{
  orders.deleteAll();movements.deleteAll();browser=new MockHttpSession();adminTab=UUID.randomUUID().toString();clientTab=UUID.randomUUID().toString();email="profile-"+UUID.randomUUID()+"@example.com";
  mvc.perform(post("/api/v1/auth/register").session(browser).header(H,clientTab).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Mi perfil\",\"correo\":\""+email+"\",\"password\":\""+PASSWORD+"\"}")).andExpect(status().isCreated());
  login(adminTab,"admin@radiocafe.com",PASSWORD,200);login(clientTab,email,PASSWORD,200);account=users.findByCorreo(email).orElseThrow().getIdUsuario();
 }
 void login(String tab,String correo,String password,int expected)throws Exception{mvc.perform(post("/api/v1/auth/login").session(browser).header(H,tab).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\""+correo+"\",\"password\":\""+password+"\"}")).andExpect(status().is(expected));}
 String password(String current,String next){return "{\"actual\":\""+current+"\",\"nueva\":\""+next+"\"}";}
 MvcResult changePassword(String current,String next,int expected)throws Exception{return mvc.perform(put("/api/v1/account/password").session(browser).header(H,clientTab).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(password(current,next))).andExpect(status().is(expected)).andReturn();}
 byte[] png()throws Exception{var output=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(20,20,BufferedImage.TYPE_INT_RGB),"png",output);return output.toByteArray();}
 String upload()throws Exception{var result=mvc.perform(multipart("/api/v1/account/avatar").file(new MockMultipartFile("file","../../avatar.png","image/png",png())).session(browser).header(H,clientTab).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist()).andReturn();return JsonPath.read(result.getResponse().getContentAsString(),"$.fotoUrl");}
 @Test void profileUpdatesOnlyOwnerAndCannotChangeRoleEmailOrSecurityVersion()throws Exception{
  String body="{\"nombre\":\"Nombre actualizado\",\"telefono\":\"999123456\",\"direccion\":\"Calle de prueba 12\",\"rol\":\"ADMINISTRADOR\",\"correo\":\"admin@radiocafe.com\",\"activo\":false,\"idUsuario\":1}";
  mvc.perform(patch("/api/v1/account/profile").session(browser).header(H,clientTab).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Nombre actualizado")).andExpect(jsonPath("$.correo").value(email)).andExpect(jsonPath("$.role").value("CLIENTE")).andExpect(jsonPath("$.activo").value(true)).andExpect(jsonPath("$.password").doesNotExist());
  mvc.perform(get("/api/v1/auth/me").session(browser).header(H,clientTab)).andExpect(jsonPath("$.direccion").value("Calle de prueba 12"));
  mvc.perform(get("/api/v1/auth/me").session(browser).header(H,adminTab)).andExpect(jsonPath("$.role").value("ADMINISTRADOR")).andExpect(jsonPath("$.nombre").value("Administrador"));
 }
 @Test void profileRequiresAuthenticationValidationAndCsrf()throws Exception{
  mvc.perform(get("/api/v1/account/activity")).andExpect(status().isUnauthorized());
  mvc.perform(patch("/api/v1/account/profile").session(browser).header(H,clientTab).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Otra cuenta\"}")).andExpect(status().isForbidden());
  mvc.perform(patch("/api/v1/account/profile").session(browser).header(H,clientTab).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"   \"}")).andExpect(status().isBadRequest());
 }
 @Test void passwordChangeKeepsCurrentWindowRevokesSameAccountAndPreservesAdmin()throws Exception{
  String other=UUID.randomUUID().toString();login(other,email,PASSWORD,200);String next="MiNuevaClave2026!";
  changePassword(PASSWORD,next,200);
  mvc.perform(get("/api/v1/auth/me").session(browser).header(H,clientTab)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("CLIENTE"));
  mvc.perform(get("/api/v1/auth/me").session(browser).header(H,other)).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/auth/me").session(browser).header(H,adminTab)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMINISTRADOR"));
  login(UUID.randomUUID().toString(),email,PASSWORD,401);login(UUID.randomUUID().toString(),email,next,200);
  assertThat(users.findById(account).orElseThrow().getPassword()).startsWith("$2").isNotEqualTo(next);
 }
 @Test void wrongCurrentWeakUnchangedOrOversizedPasswordAreRejected()throws Exception{
  changePassword("equivocada","OtraClave2026!",400);changePassword(PASSWORD,"corta",400);changePassword(PASSWORD,PASSWORD,409);changePassword(PASSWORD,"é".repeat(40),400);
  login(UUID.randomUUID().toString(),email,PASSWORD,200);assertThat(users.findById(account).orElseThrow().getSeguridadVersion()).isZero();
 }
 @Test void avatarIsPrivateAndNeverAppearsInProductLibrary()throws Exception{
  String url=upload();assertThat(url).matches("/api/v1/account/avatar/[a-f0-9-]{36}\\.png");
  mvc.perform(get(url).session(browser).header(H,clientTab)).andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG)).andExpect(header().string("Cache-Control",containsString("no-store")));
  mvc.perform(get(url).session(browser).header(H,adminTab)).andExpect(status().isNotFound());mvc.perform(get(url)).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/images").session(browser).header(H,adminTab)).andExpect(jsonPath("$[*].url",not(hasItem(url))));
  mvc.perform(delete("/api/v1/account/avatar").session(browser).header(H,clientTab).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.fotoUrl").doesNotExist());
  mvc.perform(get(url).session(browser).header(H,clientTab)).andExpect(status().isNotFound());
 }
 @Test void avatarRejectsFakeImagesAndOversizedFiles()throws Exception{
  for(byte[] data:new byte[][]{"<svg/>".getBytes(),new byte[5*1024*1024+1]})mvc.perform(multipart("/api/v1/account/avatar").file(new MockMultipartFile("file","photo.png","image/png",data)).session(browser).header(H,clientTab).with(csrf())).andExpect(status().isBadRequest());
 }
 @Test void adminEditsAndPasswordChangesKeepExistingPhotoAndAddress()throws Exception{
  String url=upload();mvc.perform(patch("/api/v1/account/profile").session(browser).header(H,clientTab).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Mi perfil\",\"direccion\":\"Calle conservada\"}")).andExpect(status().isOk());
  mvc.perform(patch("/api/v1/users/"+account).session(browser).header(H,adminTab).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Cambio administrativo\",\"rol\":\"CLIENTE\",\"activo\":true}")).andExpect(status().isOk()).andExpect(jsonPath("$.fotoUrl").value(url)).andExpect(jsonPath("$.direccion").value("Calle conservada"));
  changePassword(PASSWORD,"MiOtraClave2026!",200);mvc.perform(get(url).session(browser).header(H,clientTab)).andExpect(status().isOk());
 }
 long order(String tab,Long product,int count)throws Exception{var result=mvc.perform(post("/api/v1/orders").session(browser).header(H,tab).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"tipoEntrega\":\"PICKUP\",\"detalles\":[{\"idProducto\":"+product+",\"cantidad\":"+count+"}]}")).andExpect(status().isCreated()).andReturn();return ((Number)JsonPath.read(result.getResponse().getContentAsString(),"$.idPedido")).longValue();}
 @Test void activityContainsOnlyOwnOrdersAndDistinguishesCancelledAndDelivered()throws Exception{
  var p=new ProductoEntity();p.setNombre("Producto de cuenta");p.setCategoria("CAFE");p.setPrecio(new BigDecimal("7.50"));p.setStock(20);p.setDisponible(true);p=products.save(p);
  long mine=order(clientTab,p.getIdProducto(),2);order(adminTab,p.getIdProducto(),1);
  mvc.perform(get("/api/v1/account/activity").session(browser).header(H,clientTab).param("usuario","1")).andExpect(status().isOk()).andExpect(jsonPath("$.movimientos.length()").value(1)).andExpect(jsonPath("$.pedidosVigentes").value(15));
  mvc.perform(patch("/api/v1/orders/"+mine+"/status").session(browser).header(H,adminTab).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nuevoEstado\":\"CANCELLED\"}")).andExpect(status().isOk());
  long delivered=order(clientTab,p.getIdProducto(),2);for(String state:new String[]{"EN_PREPARACION","LISTO","ENTREGADO"})mvc.perform(patch("/api/v1/orders/"+delivered+"/status").session(browser).header(H,adminTab).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nuevoEstado\":\""+state+"\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/account/activity").session(browser).header(H,clientTab)).andExpect(jsonPath("$.movimientos.length()").value(2)).andExpect(jsonPath("$.entregados").value(15)).andExpect(jsonPath("$.cancelados").value(15)).andExpect(jsonPath("$.pedidosVigentes").value(15));
 }
}
