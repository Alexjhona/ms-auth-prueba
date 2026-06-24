package com.example.ms_auth.integration;

import com.example.ms_auth.dto.AuthUserDto;
import com.example.ms_auth.entity.AuthUser;
import com.example.ms_auth.repository.AuthUserRepository;
import com.example.ms_auth.service.EmailService;
import com.example.ms_auth.support.MySqlTestContainerSupport;
import com.example.ms_auth.support.TestDataFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthUserIntegrationTest extends MySqlTestContainerSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        authUserRepository.deleteAll();
    }

    @Test
    @DisplayName("Registro completo persiste usuario y retorna DTO sin password")
    void create_WhenRequestIsValid_PersistsUser() throws Exception {
        AuthUserDto request = TestDataFactory.validUserDto("registro", "123456");

        mockMvc.perform(post("/auth/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.userName").value("registro"))
                .andExpect(jsonPath("$.rol").value("VENDEDOR"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("Login completo devuelve JWT valido para usuario activo")
    void login_WhenCredentialsAreValid_ReturnsJwt() throws Exception {
        persistUser("login", "123456", "VENDEDOR");

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthUserDto("login", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        mockMvc.perform(post("/auth/validate").param("token", json.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("Login con password incorrecto responde 400")
    void login_WhenPasswordIsInvalid_ReturnsBadRequest() throws Exception {
        persistUser("login", "123456", "VENDEDOR");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthUserDto("login", "bad"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Registro duplicado responde 409")
    void create_WhenUserNameIsDuplicated_ReturnsConflict() throws Exception {
        AuthUserDto request = TestDataFactory.validUserDto("duplicado", "123456");

        mockMvc.perform(post("/auth/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        request.setDni("22222222");
        request.setCorreo("otro@test.com");

        mockMvc.perform(post("/auth/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Acceso protegido con token ADMIN valido responde 200")
    void protectedEndpoint_WhenAdminTokenIsValid_ReturnsOk() throws Exception {
        String token = loginAndGetToken("admin", "123456", "ADMIN");

        mockMvc.perform(get("/auth/trabajadores")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Acceso protegido sin token responde 403")
    void protectedEndpoint_WhenTokenIsMissing_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/auth/trabajadores"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Acceso protegido con token invalido responde 403")
    void protectedEndpoint_WhenTokenIsInvalid_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/auth/trabajadores")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Validaciones de entrada responden 400")
    void create_WhenBodyIsInvalid_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/auth/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\" \",\"password\":\".\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores.userName").exists())
                .andExpect(jsonPath("$.errores.password").exists());
    }

    @Test
    @DisplayName("Recurso inexistente responde 404")
    void delete_WhenWorkerDoesNotExist_ReturnsNotFound() throws Exception {
        String token = loginAndGetToken("admin", "123456", "ADMIN");

        mockMvc.perform(delete("/auth/trabajadores/999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Error inesperado responde 500 uniforme")
    void invitation_WhenUnexpectedExceptionOccurs_ReturnsInternalServerError() throws Exception {
        String token = loginAndGetToken("admin", "123456", "ADMIN");
        doThrow(new RuntimeException("smtp down")).when(emailService).enviarInvitacionTrabajador(any());

        mockMvc.perform(post("/auth/trabajadores/enviar-invitacion")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestDataFactory.invitacionRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    private String loginAndGetToken(String username, String password, String rol) throws Exception {
        persistUser(username, password, rol);
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthUserDto(username, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private void persistUser(String username, String rawPassword, String rol) {
        AuthUser user = TestDataFactory.user(username, passwordEncoder.encode(rawPassword));
        user.setRol(rol);
        user.setDni(username.equals("admin") ? "99999999" : "11111111");
        user.setCorreo(username + "@integration.test");
        authUserRepository.saveAndFlush(user);
    }
}
