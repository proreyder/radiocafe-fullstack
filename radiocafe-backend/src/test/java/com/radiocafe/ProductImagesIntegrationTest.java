package com.radiocafe;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.hasItem;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ProductImagesIntegrationTest {
    @TempDir static Path storage;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) { registry.add("app.images.directory", () -> storage.toString()); }
    @Autowired MockMvc mvc;
    private MockHttpSession admin, client;

    @BeforeEach void setup() throws Exception { admin = login("admin@radiocafe.com"); client = login("cliente@radiocafe.com"); }
    private MockHttpSession login(String email) throws Exception {
        return (MockHttpSession)mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"correo\":\"" + email + "\",\"password\":\"RadioCafe2026!\"}"))
            .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }
    private byte[] image(String format) throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(20, 12, BufferedImage.TYPE_INT_RGB), format, out);
        return out.toByteArray();
    }
    private MockMultipartFile file(byte[] bytes) { return new MockMultipartFile("file", "../../cafe.png", "image/png", bytes); }
    private String upload(byte[] bytes) throws Exception {
        var result = mvc.perform(multipart("/api/v1/images").file(file(bytes)).session(admin).with(csrf()))
            .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.url");
    }

    @Test void uploadPublishAndReadAsClientWithPersistentStorage() throws Exception {
        String url = upload(image("png"));
        assertThat(url).matches("/api/v1/images/files/[a-f0-9-]{36}\\.png");
        assertThat(storage.resolve(url.substring(url.lastIndexOf('/') + 1))).isRegularFile();
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        String body = "{\"nombre\":\"Pan con foto\",\"categoria\":\"PASTELERIA\",\"precio\":4.5,\"imagenUrl\":\"" + url + "\"}";
        var product = mvc.perform(post("/api/v1/products").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.imagenUrl").value(url)).andReturn();
        Number id = JsonPath.read(product.getResponse().getContentAsString(), "$.idProducto");
        mvc.perform(get("/api/v1/products").session(client)).andExpect(status().isOk()).andExpect(jsonPath("$[*].imagenUrl", hasItem(url)));
        mvc.perform(get("/api/v1/images").session(admin)).andExpect(status().isOk()).andExpect(jsonPath("$[*].url", hasItem(url)));
        mvc.perform(put("/api/v1/products/" + id).session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body.replace("Pan con foto", "Pan actualizado")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.imagenUrl").value(url));
    }
    @Test void onlyAdministratorsCanUploadOrBrowseLibraryAndCsrfIsRequired() throws Exception {
        for (var session : List.of(client, login("barista@radiocafe.com"), login("repartidor@radiocafe.com"))) {
            mvc.perform(multipart("/api/v1/images").file(file(image("png"))).session(session).with(csrf())).andExpect(status().isForbidden());
            mvc.perform(get("/api/v1/images").session(session)).andExpect(status().isForbidden());
        }
        mvc.perform(multipart("/api/v1/images").file(file(image("png"))).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/v1/images").file(file(image("png"))).session(admin)).andExpect(status().isForbidden());
    }
    @Test void rejectsFakeImageSvgEmptyOversizedAndMissingFile() throws Exception {
        for (byte[] bytes : List.of("fake image".getBytes(), "<svg xmlns='http://www.w3.org/2000/svg'/>".getBytes(), new byte[0], new byte[5 * 1024 * 1024 + 1]))
            mvc.perform(multipart("/api/v1/images").file(file(bytes)).session(admin).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/images").session(admin).with(csrf())).andExpect(status().isBadRequest());
    }
    @Test void detectsJpegFromBytesAndRejectsExcessiveDimensions() throws Exception {
        String url = upload(image("jpeg"));
        assertThat(url).endsWith(".jpg");
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().contentType(MediaType.IMAGE_JPEG));
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4000, 3001, BufferedImage.TYPE_BYTE_GRAY), "png", output);
        mvc.perform(multipart("/api/v1/images").file(file(output.toByteArray())).session(admin).with(csrf())).andExpect(status().isBadRequest());
    }
    @Test void unknownFileAndInvalidProductImagePathAreRejected() throws Exception {
        mvc.perform(get("/api/v1/images/files/not-a-file.png")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/images/files/00000000-0000-0000-0000-000000000000.png")).andExpect(status().isNotFound());
        for (String url : List.of("/api/v1/images/files/../secret.png", "javascript:alert(1)", "http://insecure.example/a.png"))
            mvc.perform(post("/api/v1/products").session(admin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Foto inválida\",\"categoria\":\"CAFE\",\"precio\":4,\"imagenUrl\":\"" + url + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
