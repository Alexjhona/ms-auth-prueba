package com.example.ms_auth.controller;

import com.example.ms_auth.dto.AuthUserDto;
import com.example.ms_auth.dto.AuthUserResponseDto;
import com.example.ms_auth.entity.AuthUser;
import com.example.ms_auth.entity.TokenDto;
import com.example.ms_auth.service.AuthUserService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthUserControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthUserService authUserService;

    @InjectMocks
    private AuthUserController authUserController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authUserController).build();
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
                .andExpect(status().isBadRequest());

        verify(authUserService).validate("token-invalido");
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

        when(authUserService.save(any(AuthUserDto.class))).thenReturn(null);

        mockMvc.perform(post("/auth/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(authUserService).save(any(AuthUserDto.class));
    }
}