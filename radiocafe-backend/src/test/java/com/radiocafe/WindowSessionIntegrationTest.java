package com.radiocafe;
import com.jayway.jsonpath.JsonPath;
import com.radiocafe.infrastructure.config.TabSecurityContextRepository;
import com.radiocafe.infrastructure.adapters.out.persistence.repository.SpringDataUsuarioRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class WindowSessionIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired SpringDataUsuarioRepository users;
 private MockHttpSession browser;
 private String adminTab,clientTab;
 private static final String HEADER=TabSecurityContextRepository.HEADER;
 @BeforeEach void setup() throws Exception {
  adminTab=UUID.randomUUID().toString();clientTab=UUID.randomUUID().toString();browser=new MockHttpSession();
  login(adminTab,"admin@radiocafe.com");login(clientTab,"cliente@radiocafe.com");
 }
 private MvcResult login(String tab,String email) throws Exception {
  return mvc.perform(post("/api/v1/auth/login").session(browser).header(HEADER,tab).with(csrf()).contentType(MediaType.APPLICATION_JSON)
   .content("{\"correo\":\""+email+"\",\"password\":\"RadioCafe2026!\"}")).andExpect(status().isOk()).andReturn();
 }
 private void role(String tab,String role) throws Exception {mvc.perform(get("/api/v1/auth/me").session(browser).header(HEADER,tab)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value(role));}
 @Test void sharedCookieKeepsIdentitiesAndPermissionsSeparateAcrossRefreshes() throws Exception {
  for(int i=0;i<3;i++) {role(adminTab,"ADMINISTRADOR");role(clientTab,"CLIENTE");}
  mvc.perform(get("/api/v1/users").session(browser).header(HEADER,adminTab)).andExpect(status().isOk());
  mvc.perform(get("/api/v1/users").session(browser).header(HEADER,clientTab)).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/auth/me").session(browser)).andExpect(status().isUnauthorized());
 }
 @Test void selectorAloneOrBrowserCookieAloneDoesNotGrantWindowIdentity() throws Exception {
  mvc.perform(get("/api/v1/auth/me").header(HEADER,adminTab)).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/auth/me").session(browser).header(HEADER,UUID.randomUUID())).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/auth/me").session(browser)).andExpect(status().isUnauthorized());
 }
 @Test void closingOneWindowSessionKeepsTheOtherAuthenticated() throws Exception {
  mvc.perform(post("/api/v1/auth/logout").session(browser).header(HEADER,adminTab).with(csrf())).andExpect(status().isNoContent());
  assertThat(browser.isInvalid()).isFalse();
  mvc.perform(get("/api/v1/auth/me").session(browser).header(HEADER,adminTab)).andExpect(status().isUnauthorized());role(clientTab,"CLIENTE");
  login(adminTab,"admin@radiocafe.com");
  mvc.perform(post("/api/v1/auth/logout").session(browser).header(HEADER,clientTab).with(csrf())).andExpect(status().isNoContent());role(adminTab,"ADMINISTRADOR");
 }
 @Test void loginWithFreshSelectorDoesNotReplaceSourceWindow() throws Exception {
  String newTab=UUID.randomUUID().toString();login(newTab,"barista@radiocafe.com");role(adminTab,"ADMINISTRADOR");role(clientTab,"CLIENTE");role(newTab,"BARISTA");
  mvc.perform(get("/api/v1/orders").session(browser).header(HEADER,newTab)).andExpect(status().isOk());
  mvc.perform(get("/api/v1/users").session(browser).header(HEADER,newTab)).andExpect(status().isForbidden());
 }
 @Test void realCsrfSurvivesLoginInAnotherWindowAndIsStillRequired() throws Exception {
  var result=mvc.perform(get("/api/v1/auth/csrf").session(browser).header(HEADER,adminTab)).andExpect(status().isOk()).andReturn();
  String token=JsonPath.read(result.getResponse().getContentAsString(),"$.token");
  login(UUID.randomUUID().toString(),"barista@radiocafe.com");
  mvc.perform(post("/api/v1/commerce/quote").session(browser).header(HEADER,clientTab).header("X-CSRF-TOKEN",token).contentType(MediaType.APPLICATION_JSON).content("{\"subtotal\":20}")).andExpect(status().isOk());
  mvc.perform(post("/api/v1/commerce/quote").session(browser).header(HEADER,clientTab).contentType(MediaType.APPLICATION_JSON).content("{\"subtotal\":20}")).andExpect(status().isForbidden());
 }
 @Test void newProductImmediatelySupportsClientOpinionEvenWithZeroStock() throws Exception {
  var created=mvc.perform(post("/api/v1/products").session(browser).header(HEADER,adminTab).with(csrf()).contentType(MediaType.APPLICATION_JSON)
   .content("{\"nombre\":\"Nuevo producto con opiniones\",\"categoria\":\"CAFE\",\"precio\":7.5,\"stock\":0}")).andExpect(status().isCreated()).andReturn();
  Number product=JsonPath.read(created.getResponse().getContentAsString(),"$.idProducto");
  mvc.perform(get("/api/v1/commerce/reviews").param("producto",product.toString())).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(post("/api/v1/commerce/products/"+product+"/reviews").session(browser).header(HEADER,clientTab).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"estrellas\":5,\"comentario\":\"Me gusta este nuevo café\"}"))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.autor").value("Cliente demo"));
  mvc.perform(get("/api/v1/commerce/reviews").param("producto",product.toString())).andExpect(status().isOk()).andExpect(jsonPath("$[0].comentario").value("Me gusta este nuevo café"));
  mvc.perform(post("/api/v1/commerce/products/"+product+"/reviews").session(browser).header(HEADER,adminTab).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"estrellas\":5,\"comentario\":\"Admin no debe opinar\"}")).andExpect(status().isForbidden());
 }
 @Test void revokingOneAccountDoesNotInvalidateOtherWindowSessions() throws Exception {
  String email="windows-"+UUID.randomUUID()+"@example.com";
  mvc.perform(post("/api/v1/auth/register").session(browser).header(HEADER,clientTab).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Cliente revocado\",\"correo\":\""+email+"\",\"password\":\"RadioCafe2026!\"}")).andExpect(status().isCreated());
  String accountTab=UUID.randomUUID().toString();login(accountTab,email);var account=users.findByCorreo(email).orElseThrow();
  mvc.perform(patch("/api/v1/users/"+account.getIdUsuario()).session(browser).header(HEADER,adminTab).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Cliente revocado\",\"rol\":\"CLIENTE\",\"activo\":false}")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/auth/me").session(browser).header(HEADER,accountTab)).andExpect(status().isUnauthorized());
  role(adminTab,"ADMINISTRADOR");role(clientTab,"CLIENTE");assertThat(browser.isInvalid()).isFalse();
 }
 @Test void rejectsInvalidSelectorAndLegacyLogoutDoesNotRemoveWindowSessions() throws Exception {
  mvc.perform(get("/api/v1/auth/me").session(browser).header(HEADER,"bad")).andExpect(status().isBadRequest());
  mvc.perform(post("/api/v1/auth/login").session(browser).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"barista@radiocafe.com\",\"password\":\"RadioCafe2026!\"}")).andExpect(status().isOk());
  mvc.perform(post("/api/v1/auth/logout").session(browser).with(csrf())).andExpect(status().isNoContent());role(adminTab,"ADMINISTRADOR");role(clientTab,"CLIENTE");
 }
}
