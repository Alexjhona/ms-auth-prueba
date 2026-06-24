package com.example.ms_auth.controller;

import com.example.ms_auth.dto.AuthUserDto;
import com.example.ms_auth.dto.DniConsultaResponseDto;
import com.example.ms_auth.entity.AuthUser;
import com.example.ms_auth.entity.TokenDto;
import com.example.ms_auth.exception.ConflictoRecursoException;
import com.example.ms_auth.exception.GlobalExceptionHandler;
import com.example.ms_auth.exception.RecursoNoEncontradoException;
import com.example.ms_auth.filter.RequestBodyCachingFilter;
import com.example.ms_auth.security.JwtProvider;
import com.example.ms_auth.service.AuthUserService;
import com.example.ms_auth.service.ConsultaDniService;
import com.example.ms_auth.service.EmailService;
import com.example.ms_auth.support.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthUserControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthUserService authUserService;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private ConsultaDniService consultaDniService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthUserController authUserController;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(authUserController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestBodyCachingFilter())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("POST /auth/login - retorna token si credenciales son correctas")
    void login_CredencialesCorrectas_RetornaOk() throws Exception {
        AuthUserDto request = AuthUserDto.builder()
                .userName("admin")
                .password("123456")
                .build();

        TokenDto tokenDto = new TokenDto("token-generado");

        when(authUserService.login(any(AuthUserDto.class))).thenReturn(tokenDto);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-generado"));

        verify(authUserService).login(any(AuthUserDto.class));
    }

    @Test
    @DisplayName("POST /auth/login - retorna Bad Request si credenciales son incorrectas")
    void login_CredencialesIncorrectas_RetornaBadRequest() throws Exception {
        AuthUserDto request = AuthUserDto.builder()
                .userName("admin")
                .password("incorrecto")
                .build();

        when(authUserService.login(any(AuthUserDto.class))).thenReturn(null);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authUserService).login(any(AuthUserDto.class));
    }

    @Test
    @DisplayName("POST /auth/login - retorna Bad Request si usuario esta vacio")
    void login_UsuarioVacio_RetornaBadRequestConValidacion() throws Exception {
        AuthUserDto request = AuthUserDto.builder()
                .userName("   ")
                .password("123456")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensaje").value("Se encontraron errores de validación"))
                .andExpect(jsonPath("$.ruta").value("/auth/login"))
                .andExpect(jsonPath("$.datosRecibidos.userName").value("   "))
                .andExpect(jsonPath("$.datosRecibidos.password").value("123456"))
                .andExpect(jsonPath("$.errores.userName").value("Campo obligatorio"));

        verifyNoInteractions(authUserService);
    }

    @Test
    @DisplayName("POST /auth/login - retorna Bad Request si password es punto")
    void login_PasswordPunto_RetornaBadRequestConValidacion() throws Exception {
        AuthUserDto request = AuthUserDto.builder()
                .userName("admin")
                .password(".")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensaje").value("Se encontraron errores de validación"))
                .andExpect(jsonPath("$.ruta").value("/auth/login"))
                .andExpect(jsonPath("$.datosRecibidos.userName").value("admin"))
                .andExpect(jsonPath("$.datosRecibidos.password").value("."))
                .andExpect(jsonPath("$.errores.password").value("Valor inválido"));

        verifyNoInteractions(authUserService);
    }

    @Test
    @DisplayName("POST /auth/login - retorna todos los errores de validacion")
    void login_CamposInvalidos_RetornaMultiplesErrores() throws Exception {
        AuthUserDto request = AuthUserDto.builder()
                .userName("   ")
                .password(".")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensaje").value("Se encontraron errores de validación"))
                .andExpect(jsonPath("$.ruta").value("/auth/login"))
                .andExpect(jsonPath("$.datosRecibidos.userName").value("   "))
                .andExpect(jsonPath("$.datosRecibidos.password").value("."))
                .andExpect(jsonPath("$.errores.userName").value("Campo obligatorio"))
                .andExpect(jsonPath("$.errores.password").value("Valor inválido"));

        verifyNoInteractions(authUserService);
    }

    @Test
    @DisplayName("POST /auth/validate - retorna token si es válido")
    void validate_TokenValido_RetornaOk() throws Exception {
        TokenDto tokenDto = new TokenDto("token-valido");

        when(authUserService.validate("token-valido")).thenReturn(tokenDto);

        mockMvc.perform(post("/auth/validate")
                        .param("token", "token-valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-valido"));

        verify(authUserService).validate("token-valido");
    }

    @Test
    @DisplayName("POST /auth/validate - retorna Bad Request si token es inválido")
    void validate_TokenInvalido_RetornaBadRequest() throws Exception {
        when(authUserService.validate("token-invalido")).thenReturn(null);

        mockMvc.perform(post("/auth/validate")
                        .param("token", "token-invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensaje").value("Se encontraron errores de validación"))
                .andExpect(jsonPath("$.ruta").value("/auth/validate"))
                .andExpect(jsonPath("$.datosRecibidos.token").value("token-invalido"))
                .andExpect(jsonPath("$.errores.general").value("Token inválido"));

        verify(authUserService).validate("token-invalido");
    }

    @Test
    @DisplayName("POST /auth/validate - retorna Bad Request uniforme si falta token")
    void validate_TokenFaltante_RetornaBadRequestUniforme() throws Exception {
        mockMvc.perform(post("/auth/validate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensaje").value("Se encontraron errores de validación"))
                .andExpect(jsonPath("$.ruta").value("/auth/validate"))
                .andExpect(jsonPath("$.datosRecibidos").exists())
                .andExpect(jsonPath("$.errores.token").value("Parámetro obligatorio"));

        verifyNoInteractions(authUserService);
    }

    @Test
    @DisplayName("POST /auth/create - crea usuario correctamente")
    void create_UsuarioNuevo_RetornaOk() throws Exception {
        AuthUserDto request = AuthUserDto.builder()
                .userName("admin")
                .password("123456")
                .build();

        AuthUser authUser = AuthUser.builder()
                .id(1)
                .userName("admin")
                .password("password-encriptado")
                .build();

        when(authUserService.save(any(AuthUserDto.class))).thenReturn(authUser);

        mockMvc.perform(post("/auth/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userName").value("admin"));

        verify(authUserService).save(any(AuthUserDto.class));
    }

    @Test
    @DisplayName("POST /auth/create - retorna Bad Request si usuario ya existe")
    void create_UsuarioExistente_RetornaBadRequest() throws Exception {
        AuthUserDto request = AuthUserDto.builder()
                .userName("admin")
                .password("123456")
                .build();

        when(authUserService.save(any(AuthUserDto.class))).thenThrow(new ConflictoRecursoException());

        mockMvc.perform(post("/auth/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.mensaje").value("El registro ya existe o genera conflicto"))
                .andExpect(jsonPath("$.ruta").value("/auth/create"));

        verify(authUserService).save(any(AuthUserDto.class));
    }

    @Test
    @DisplayName("POST /auth/create - retorna Bad Request si password es null")
    void create_PasswordNull_RetornaBadRequestConValidacion() throws Exception {
        AuthUserDto request = AuthUserDto.builder()
                .userName("admin")
                .password(null)
                .build();

        mockMvc.perform(post("/auth/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensaje").value("Se encontraron errores de validación"))
                .andExpect(jsonPath("$.ruta").value("/auth/create"))
                .andExpect(jsonPath("$.datosRecibidos.userName").value("admin"))
                .andExpect(jsonPath("$.errores.password").value("Campo obligatorio"));

        verifyNoInteractions(authUserService);
    }

    @Test
    @DisplayName("POST /auth/create - retorna Bad Request uniforme por tipo de dato invalido")
    void create_TipoDatoInvalido_RetornaBadRequestUniforme() throws Exception {
        String request = "{\"userName\":\"admin\",\"password\":\"123456\",\"activo\":\"texto\"}";

        mockMvc.perform(post("/auth/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.mensaje").value("Se encontraron errores de validación"))
                .andExpect(jsonPath("$.ruta").value("/auth/create"))
                .andExpect(jsonPath("$.datosRecibidos.userName").value("admin"))
                .andExpect(jsonPath("$.datosRecibidos.password").value("123456"))
                .andExpect(jsonPath("$.datosRecibidos.activo").value("texto"))
                .andExpect(jsonPath("$.errores.activo").value("Tipo de dato inválido"));

        verifyNoInteractions(authUserService);
    }

    @Test
    @DisplayName("POST /auth/trabajadores/activar - retorna Not Found uniforme")
    void activarTrabajador_NoEncontrado_RetornaNotFoundUniforme() throws Exception {
        String request = "{\"correo\":\"persona@correo.com\",\"userName\":\"persona\",\"password\":\"123456\"}";

        when(authUserService.activarTrabajador("persona@correo.com", "persona", "123456"))
                .thenThrow(new RecursoNoEncontradoException());

        mockMvc.perform(post("/auth/trabajadores/activar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensaje").value("No se encontró el recurso solicitado"))
                .andExpect(jsonPath("$.ruta").value("/auth/trabajadores/activar"));

        verify(authUserService).activarTrabajador("persona@correo.com", "persona", "123456");
    }

    @Test
    @DisplayName("GET /auth/trabajadores - retorna OK con token ADMIN valido")
    void listarTrabajadores_TokenAdminValido_RetornaOk() throws Exception {
        when(authUserService.validate("token-admin")).thenReturn(new TokenDto("token-admin"));
        when(jwtProvider.getRolFromToken("token-admin")).thenReturn("ADMIN");
        AuthUser worker = TestDataFactory.user("ana", "encoded");
        worker.setId(10);
        when(authUserService.listar()).thenReturn(java.util.List.of(worker));

        mockMvc.perform(get("/auth/trabajadores")
                        .header("Authorization", "Bearer token-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userName").value("ana"))
                .andExpect(jsonPath("$[0].rol").value("VENDEDOR"));
    }

    @Test
    @DisplayName("GET /auth/trabajadores - retorna Forbidden sin token")
    void listarTrabajadores_SinToken_RetornaForbidden() throws Exception {
        mockMvc.perform(get("/auth/trabajadores"))
                .andExpect(status().isForbidden());

        verify(authUserService, never()).listar();
    }

    @Test
    @DisplayName("GET /auth/trabajadores - retorna Forbidden con rol no administrador")
    void listarTrabajadores_RolNoAdmin_RetornaForbidden() throws Exception {
        when(authUserService.validate("token-vendedor")).thenReturn(new TokenDto("token-vendedor"));
        when(jwtProvider.getRolFromToken("token-vendedor")).thenReturn("VENDEDOR");

        mockMvc.perform(get("/auth/trabajadores")
                        .header("Authorization", "Bearer token-vendedor"))
                .andExpect(status().isForbidden());

        verify(authUserService, never()).listar();
    }

    @Test
    @DisplayName("POST /auth/trabajadores - retorna Forbidden con token invalido")
    void crearTrabajador_TokenInvalido_RetornaForbidden() throws Exception {
        when(authUserService.validate("bad")).thenReturn(null);

        mockMvc.perform(post("/auth/trabajadores")
                        .header("Authorization", "Bearer bad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestDataFactory.validUserDto("ana", "123456"))))
                .andExpect(status().isForbidden());

        verify(authUserService, never()).save(any(AuthUserDto.class));
    }

    @Test
    @DisplayName("DELETE /auth/trabajadores/{id} - retorna No Content con token OWNER")
    void eliminarTrabajador_TokenOwnerValido_RetornaNoContent() throws Exception {
        when(authUserService.validate("token-owner")).thenReturn(new TokenDto("token-owner"));
        when(jwtProvider.getRolFromToken("token-owner")).thenReturn("OWNER");

        mockMvc.perform(delete("/auth/trabajadores/3")
                        .header("Authorization", "Bearer token-owner"))
                .andExpect(status().isNoContent());

        verify(authUserService).eliminar(3);
    }

    @Test
    @DisplayName("GET /auth/dni/{dni} - retorna datos de consulta DNI")
    void consultarDni_CuandoExiste_RetornaOk() throws Exception {
        DniConsultaResponseDto response = new DniConsultaResponseDto();
        response.setDni("12345678");
        response.setNombres("Ana Maria");
        when(consultaDniService.consultar("12345678")).thenReturn(response);

        mockMvc.perform(get("/auth/dni/12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dni").value("12345678"))
                .andExpect(jsonPath("$.nombres").value("Ana Maria"));
    }

    @Test
    @DisplayName("POST /auth/trabajadores - crea trabajador con token ADMIN")
    void crearTrabajador_TokenAdminValido_RetornaOk() throws Exception {
        AuthUser saved = TestDataFactory.user("ana", "encoded");
        saved.setId(11);
        when(authUserService.validate("token-admin")).thenReturn(new TokenDto("token-admin"));
        when(jwtProvider.getRolFromToken("token-admin")).thenReturn("ADMIN");
        when(authUserService.save(any(AuthUserDto.class))).thenReturn(saved);

        mockMvc.perform(post("/auth/trabajadores")
                        .header("Authorization", "Bearer token-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestDataFactory.validUserDto("ana", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.userName").value("ana"));
    }

    @Test
    @DisplayName("POST /auth/trabajadores/activar - activa trabajador correctamente")
    void activarTrabajador_DatosValidos_RetornaOk() throws Exception {
        AuthUser activated = TestDataFactory.user("trabajador", "encoded");
        activated.setId(12);
        activated.setCorreo("trabajador@test.com");
        when(authUserService.activarTrabajador("trabajador@test.com", "trabajador", "123456"))
                .thenReturn(activated);

        mockMvc.perform(post("/auth/trabajadores/activar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestDataFactory.activarTrabajadorRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    @DisplayName("POST /auth/trabajadores/enviar-invitacion - retorna mensaje de exito")
    void enviarInvitacion_DatosValidos_RetornaOk() throws Exception {
        when(authUserService.validate("token-admin")).thenReturn(new TokenDto("token-admin"));
        when(jwtProvider.getRolFromToken("token-admin")).thenReturn("ADMIN");

        mockMvc.perform(post("/auth/trabajadores/enviar-invitacion")
                        .header("Authorization", "Bearer token-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestDataFactory.invitacionRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Invitacion enviada correctamente"));

        verify(emailService).enviarInvitacionTrabajador(any());
    }

    @Test
    @DisplayName("POST /auth/trabajadores/{id}/impersonar - retorna token")
    void impersonarTrabajador_TokenAdminValido_RetornaToken() throws Exception {
        when(authUserService.validate("token-admin")).thenReturn(new TokenDto("token-admin"));
        when(jwtProvider.getRolFromToken("token-admin")).thenReturn("ADMIN");
        when(authUserService.impersonarTrabajador(8)).thenReturn(new TokenDto("token-impersonado"));

        mockMvc.perform(post("/auth/trabajadores/8/impersonar")
                        .header("Authorization", "Bearer token-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-impersonado"));
    }

    @Test
    @DisplayName("PUT /auth/trabajadores/{id} - actualiza trabajador")
    void actualizarTrabajador_TokenAdminValido_RetornaOk() throws Exception {
        AuthUser updated = TestDataFactory.user("ana2", "encoded");
        updated.setId(9);
        when(authUserService.validate("token-admin")).thenReturn(new TokenDto("token-admin"));
        when(jwtProvider.getRolFromToken("token-admin")).thenReturn("ADMIN");
        when(authUserService.actualizar(eq(9), any(AuthUserDto.class))).thenReturn(updated);

        mockMvc.perform(put("/auth/trabajadores/9")
                        .header("Authorization", "Bearer token-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestDataFactory.validUserDto("ana2", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.userName").value("ana2"));
    }

    @Test
    @DisplayName("PATCH /auth/trabajadores/{id}/estado - cambia estado")
    void cambiarEstadoTrabajador_TokenAdminValido_RetornaOk() throws Exception {
        AuthUser disabled = TestDataFactory.user("ana", "encoded");
        disabled.setId(10);
        disabled.setActivo(false);
        when(authUserService.validate("token-admin")).thenReturn(new TokenDto("token-admin"));
        when(jwtProvider.getRolFromToken("token-admin")).thenReturn("ADMIN");
        when(authUserService.cambiarEstado(10, false)).thenReturn(disabled);

        mockMvc.perform(patch("/auth/trabajadores/10/estado")
                        .header("Authorization", "Bearer token-admin")
                        .param("activo", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.activo").value(false));
    }

    @Test
    @DisplayName("POST /auth/trabajadores/enviar-invitacion - retorna 500 uniforme ante error inesperado")
    void enviarInvitacion_ErrorInesperado_RetornaInternalServerError() throws Exception {
        when(authUserService.validate("token-admin")).thenReturn(new TokenDto("token-admin"));
        when(jwtProvider.getRolFromToken("token-admin")).thenReturn("ADMIN");
        doThrow(new RuntimeException("smtp down")).when(emailService).enviarInvitacionTrabajador(any());

        mockMvc.perform(post("/auth/trabajadores/enviar-invitacion")
                        .header("Authorization", "Bearer token-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TestDataFactory.invitacionRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.mensaje").value("Ocurrió un error inesperado en el servidor"));
    }
}
