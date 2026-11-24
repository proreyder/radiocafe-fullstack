package com.radiocafe;

import com.radiocafe.domain.model.*;
import com.radiocafe.domain.ports.in.*;
import com.radiocafe.infrastructure.adapters.out.persistence.entity.ProductoEntity;
import com.radiocafe.infrastructure.adapters.out.persistence.repository.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class OperationsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SpringDataProductoRepository products;
    @Autowired SpringDataOrderRepository orders;
    @Autowired MovimientoInventarioRepository movements;
    @Autowired SpringDataUsuarioRepository users;
    @Autowired CreateOrderUseCase createOrder;
    @Autowired ManageOrderStateUseCase changeOrder;
    @Autowired ManageUsersUseCase manageUsers;
    private MockHttpSession admin,client;
    private Long product,adminId,clientId;
    private static final String PASSWORD="RadioCafe2026!";

    @BeforeEach void setup() throws Exception {
        orders.deleteAll();movements.deleteAll();
        var p=new ProductoEntity();p.setNombre("Stock de prueba "+UUID.randomUUID());p.setCategoria("CAFE");p.setPrecio(new BigDecimal("4.50"));p.setStock(3);p.setDisponible(true);p.setPersonalizable(true);
        product=products.save(p).getIdProducto();
        admin=login("admin@radiocafe.com");client=login("cliente@radiocafe.com");
        adminId=users.findByCorreo("admin@radiocafe.com").orElseThrow().getIdUsuario();clientId=users.findByCorreo("cliente@radiocafe.com").orElseThrow().getIdUsuario();
    }
    private MockHttpSession login(String correo) throws Exception {
        return (MockHttpSession)mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"correo\":\""+correo+"\",\"password\":\""+PASSWORD+"\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }
    private long id(MvcResult r,String field) throws Exception {return ((Number)JsonPath.read(r.getResponse().getContentAsString(),field)).longValue();}
    private String orderBody(int quantity) {return "{\"tipoEntrega\":\"PICKUP\",\"detalles\":[{\"idProducto\":"+product+",\"cantidad\":"+quantity+"}]}";}
    private Pedido pedido(int quantity) {var p=new Pedido();p.setIdUsuario(clientId);p.setTipoEntrega(TipoEntrega.PICKUP);p.agregarDetalle(new DetallePedido(product,"No confiable",quantity,BigDecimal.ONE));return p;}
    private long order(int quantity) throws Exception {return id(mvc.perform(post("/api/v1/orders").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(orderBody(quantity))).andExpect(status().isCreated()).andReturn(),"$.idPedido");}
    private void updateUser(Long target,String role,boolean active,MockHttpSession actor,int expected) throws Exception {
        mvc.perform(patch("/api/v1/users/"+target).session(actor).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nombre\":\"Cuenta de prueba\",\"rol\":\""+role+"\",\"activo\":"+active+"}")).andExpect(status().is(expected));
    }
    private Usuario account(Rol role) {return manageUsers.crear("Cuenta de prueba","ops-"+UUID.randomUUID()+"@example.com",PASSWORD,role,null,adminId);}
    private List<Boolean> race(Callable<Boolean> first,Callable<Boolean> second) throws Exception {
        var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {var a=pool.submit(()->{start.await();return first.call();});var b=pool.submit(()->{start.await();return second.call();});start.countDown();return List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));} finally {pool.shutdownNow();}
    }
    @Test void inventoryPermissionsAreEnforced() throws Exception {
        mvc.perform(get("/api/v1/inventory").session(client)).andExpect(status().isForbidden());
        var barista=login("barista@radiocafe.com");mvc.perform(get("/api/v1/inventory").session(barista)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/inventory/"+product+"/adjustments").session(barista).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":2,\"motivo\":\"Reposición\"}")).andExpect(status().isForbidden());
    }
    @Test void adjustmentsCreateHistoryAndRejectNegativeBalanceAndZero() throws Exception {
        mvc.perform(post("/api/v1/inventory/"+product+"/adjustments").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":5,\"motivo\":\"Reposición de prueba\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(8));
        for(int delta:new int[]{-9,0}) mvc.perform(post("/api/v1/inventory/"+product+"/adjustments").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"cantidad\":"+delta+",\"motivo\":\"Corrección de prueba\"}")).andExpect(status().is(delta==0?400:409));
        assertThat(products.findById(product).orElseThrow().getStock()).isEqualTo(8);
        mvc.perform(get("/api/v1/inventory/movements").param("idProducto",product.toString()).session(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].idUsuario").value(adminId)).andExpect(jsonPath("$[0].saldo").value(8));
    }
    @Test void orderDiscountsStockAndCancellationRestoresOnlyOnce() throws Exception {
        long id=order(2);assertThat(products.findById(product).orElseThrow().getStock()).isEqualTo(1);
        mvc.perform(patch("/api/v1/orders/"+id+"/status").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nuevoEstado\":\"CANCELLED\"}")).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/orders/"+id+"/status").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nuevoEstado\":\"CANCELLED\"}")).andExpect(status().isConflict());
        assertThat(products.findById(product).orElseThrow().getStock()).isEqualTo(3);
        assertThat(movements.findTop200ByIdProductoOrderByIdDesc(product)).hasSize(2);
    }
    @Test void variantsShareOneProductStock() throws Exception {
        String body="{\"tipoEntrega\":\"PICKUP\",\"detalles\":[{\"idProducto\":"+product+",\"cantidad\":2},{\"idProducto\":"+product+",\"cantidad\":2,\"personalizacion\":{\"tamano\":\"GRANDE\",\"leche\":\"ENTERA\",\"crema\":\"SIN_CREMA\",\"extras\":[]}}]}";
        mvc.perform(post("/api/v1/orders").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        assertThat(products.findById(product).orElseThrow().getStock()).isEqualTo(3);assertThat(orders.count()).isZero();assertThat(movements.count()).isZero();
    }
    @Test void missingStockInSecondProductRollsBackWholeOrder() throws Exception {
        var second=new ProductoEntity();second.setNombre("Agotado");second.setCategoria("CAFE");second.setPrecio(BigDecimal.TEN);second.setStock(0);long id=products.save(second).getIdProducto();
        String body="{\"tipoEntrega\":\"PICKUP\",\"detalles\":[{\"idProducto\":"+product+",\"cantidad\":1},{\"idProducto\":"+id+",\"cantidad\":1}]}";
        mvc.perform(post("/api/v1/orders").session(client).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        assertThat(products.findById(product).orElseThrow().getStock()).isEqualTo(3);assertThat(orders.count()).isZero();assertThat(movements.count()).isZero();
    }
    @Test void concurrentLastUnitHasOneBuyer() throws Exception {
        var p=products.findById(product).orElseThrow();p.setStock(1);products.save(p);
        Callable<Boolean> buy=()->{try {createOrder.crearPedido(pedido(1));return true;}catch(BusinessException e){return false;}};
        assertThat(race(buy,buy)).containsExactlyInAnyOrder(true,false);assertThat(products.findById(product).orElseThrow().getStock()).isZero();assertThat(orders.count()).isEqualTo(1);assertThat(movements.count()).isEqualTo(1);
    }
    @Test void concurrentCancellationRestoresOnce() throws Exception {
        long id=order(2);Callable<Boolean> cancel=()->{try {changeOrder.cambiarEstadoPedido(id,EstadoPedido.CANCELLED,adminId);return true;}catch(BusinessException e){return false;}};
        assertThat(race(cancel,cancel)).containsExactlyInAnyOrder(true,false);assertThat(products.findById(product).orElseThrow().getStock()).isEqualTo(3);assertThat(movements.count()).isEqualTo(2);
    }
    @Test void legacyOrdersDoNotCreateUnreservedStockOnCancellation() throws Exception {
        long id=order(1);var legacy=orders.findById(id).orElseThrow();legacy.setStockDescontado(false);orders.save(legacy);
        changeOrder.cambiarEstadoPedido(id,EstadoPedido.CANCELLED,adminId);assertThat(products.findById(product).orElseThrow().getStock()).isEqualTo(2);
    }
    @Test void catalogMetadataChangesPreserveExistingStock() throws Exception {
        String body="{\"nombre\":\"Editado\",\"categoria\":\"CAFE\",\"precio\":7}";
        mvc.perform(put("/api/v1/products/"+product).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(3));
        mvc.perform(post("/api/v1/products").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andExpect(jsonPath("$.stock").value(0));
    }
    @Test void adminCanCreateStaffWhoCanLoginAndPasswordIsNeverReturned() throws Exception {
        String email="staff-"+UUID.randomUUID()+"@example.com";
        mvc.perform(post("/api/v1/users").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Barista nuevo\",\"correo\":\""+email+"\",\"password\":\"RadioCafe2026!\",\"rol\":\"BARISTA\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("BARISTA")).andExpect(jsonPath("$.activo").value(true)).andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/v1/inventory").session(login(email))).andExpect(status().isOk());
        assertThat(users.findByCorreo(email).orElseThrow().getPassword()).startsWith("$2").isNotEqualTo(PASSWORD);
        mvc.perform(get("/api/v1/users").session(admin)).andExpect(status().isOk()).andExpect(jsonPath("$[0].password").doesNotExist());
    }
    @Test void nonAdminsCannotManageUsers() throws Exception {
        for(var session:List.of(client,login("barista@radiocafe.com"),login("repartidor@radiocafe.com"))) {
            mvc.perform(get("/api/v1/users").session(session)).andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/users").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"No permitido\",\"correo\":\"no@example.com\",\"password\":\"RadioCafe2026!\",\"rol\":\"ADMINISTRADOR\"}")).andExpect(status().isForbidden());
        }
    }
    @Test void administratorCannotDisableOrDemoteOwnAccount() throws Exception {
        updateUser(adminId,"ADMINISTRADOR",false,admin,409);updateUser(adminId,"CLIENTE",true,admin,409);
    }
    @Test void disabledAccountLosesExistingSessionAndCannotLogin() throws Exception {
        var u=account(Rol.BARISTA);var session=login(u.correo());updateUser(u.idUsuario(),"BARISTA",false,admin,200);
        mvc.perform(get("/api/v1/inventory").session(session)).andExpect(status().isUnauthorized());assertThat(session.isInvalid()).isTrue();
        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\""+u.correo()+"\",\"password\":\"RadioCafe2026!\"}")).andExpect(status().isUnauthorized());
        updateUser(u.idUsuario(),"BARISTA",true,admin,200);mvc.perform(get("/api/v1/inventory").session(login(u.correo()))).andExpect(status().isOk());
    }
    @Test void roleChangeRevokesOldPrivilegesImmediately() throws Exception {
        var u=account(Rol.BARISTA);var oldSession=login(u.correo());updateUser(u.idUsuario(),"CLIENTE",true,admin,200);
        mvc.perform(get("/api/v1/inventory").session(oldSession)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/inventory").session(login(u.correo()))).andExpect(status().isForbidden());
    }
    @Test void twoAdminsCannotDemoteEachOtherConcurrently() throws Exception {
        var a=account(Rol.ADMINISTRADOR);var b=account(Rol.ADMINISTRADOR);
        Callable<Boolean> first=()->{try {manageUsers.actualizar(b.idUsuario(),"B",Rol.CLIENTE,true,null,a.idUsuario());return true;}catch(BusinessException e){return false;}};
        Callable<Boolean> second=()->{try {manageUsers.actualizar(a.idUsuario(),"A",Rol.CLIENTE,true,null,b.idUsuario());return true;}catch(BusinessException e){return false;}};
        assertThat(race(first,second)).containsExactlyInAnyOrder(true,false);
    }
    @Test void duplicateUserAndInvalidRolesAreRejected() throws Exception {
        mvc.perform(post("/api/v1/users").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Duplicado\",\"correo\":\"ADMIN@RADIOCAFE.COM\",\"password\":\"RadioCafe2026!\",\"rol\":\"BARISTA\"}")).andExpect(status().isConflict());
        updateUser(adminId,"SUPERADMIN",true,admin,400);
    }
    @Test void openApiIncludesInventoryUsersAndWorkingExplorerAssets() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.info.title").value("RadioCafé API"))
            .andExpect(jsonPath("$.paths['/api/v1/inventory/{id}/adjustments'].post").exists()).andExpect(jsonPath("$.paths['/api/v1/users'].post").exists());
        mvc.perform(get("/api-explorer/index.html")).andExpect(status().isOk()).andExpect(content().string(containsString("requestInterceptor"))).andExpect(content().string(containsString("/api/v1/auth/csrf")));
        mvc.perform(get("/swagger-ui/swagger-ui-bundle.js")).andExpect(status().isOk());
    }
}
