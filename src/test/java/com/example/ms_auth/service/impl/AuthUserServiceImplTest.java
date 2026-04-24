package com.example.ms_auth.service.impl;

import com.example.ms_auth.dto.AuthUserDto;
import com.example.ms_auth.entity.AuthUser;
import com.example.ms_auth.entity.TokenDto;
import com.example.ms_auth.repository.AuthUserRepository;
import com.example.ms_auth.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthUserServiceImplTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthUserServiceImpl authUserService;

    @Test
    @DisplayName("Guardar usuario - registra usuario nuevo correctamente")
    void save_CuandoUsuarioNoExiste_GuardaUsuario() {
        AuthUserDto dto = AuthUserDto.builder()
                .userName("admin")
                .password("123456")
                .build();

        AuthUser usuarioGuardado = AuthUser.builder()
                .id(1)
                .userName("admin")
                .password("password-encriptado")
                .build();

        when(authUserRepository.findByUserName("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("password-encriptado");
        when(authUserRepository.save(any(AuthUser.class))).thenReturn(usuarioGuardado);

        AuthUser resultado = authUserService.save(dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1);
        assertThat(resultado.getUserName()).isEqualTo("admin");
        assertThat(resultado.getPassword()).isEqualTo("password-encriptado");

        verify(authUserRepository).findByUserName("admin");
        verify(passwordEncoder).encode("123456");
        verify(authUserRepository).save(any(AuthUser.class));
    }

    @Test
    @DisplayName("Guardar usuario - retorna null si usuario ya existe")
    void save_CuandoUsuarioYaExiste_RetornaNull() {
        AuthUserDto dto = AuthUserDto.builder()
                .userName("admin")
                .password("123456")
                .build();

        AuthUser existente = AuthUser.builder()
                .id(1)
                .userName("admin")
                .password("password-encriptado")
                .build();

        when(authUserRepository.findByUserName("admin")).thenReturn(Optional.of(existente));

        AuthUser resultado = authUserService.save(dto);

        assertThat(resultado).isNull();

        verify(authUserRepository).findByUserName("admin");
        verify(passwordEncoder, never()).encode(anyString());
        verify(authUserRepository, never()).save(any(AuthUser.class));
    }

    @Test
    @DisplayName("Login - retorna token si credenciales son correctas")
    void login_CredencialesCorrectas_RetornaToken() {
        AuthUserDto dto = AuthUserDto.builder()
                .userName("admin")
                .password("123456")
                .build();

        AuthUser usuario = AuthUser.builder()
                .id(1)
                .userName("admin")
                .password("password-encriptado")
                .build();

        when(authUserRepository.findByUserName("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("123456", "password-encriptado")).thenReturn(true);
        when(jwtProvider.createToken(usuario)).thenReturn("token-generado");

        TokenDto resultado = authUserService.login(dto);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getToken()).isEqualTo("token-generado");

        verify(authUserRepository).findByUserName("admin");
        verify(passwordEncoder).matches("123456", "password-encriptado");
        verify(jwtProvider).createToken(usuario);
    }

    @Test
    @DisplayName("Login - retorna null si usuario no existe")
    void login_UsuarioNoExiste_RetornaNull() {
        AuthUserDto dto = AuthUserDto.builder()
                .userName("noexiste")
                .password("123456")
                .build();

        when(authUserRepository.findByUserName("noexiste")).thenReturn(Optional.empty());

        TokenDto resultado = authUserService.login(dto);

        assertThat(resultado).isNull();

        verify(authUserRepository).findByUserName("noexiste");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtProvider, never()).createToken(any(AuthUser.class));
    }

    @Test
    @DisplayName("Login - retorna null si contraseña es incorrecta")
    void login_PasswordIncorrecto_RetornaNull() {
        AuthUserDto dto = AuthUserDto.builder()
                .userName("admin")
                .password("incorrecto")
                .build();

        AuthUser usuario = AuthUser.builder()
                .id(1)
                .userName("admin")
                .password("password-encriptado")
                .build();

        when(authUserRepository.findByUserName("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecto", "password-encriptado")).thenReturn(false);

        TokenDto resultado = authUserService.login(dto);

        assertThat(resultado).isNull();

        verify(authUserRepository).findByUserName("admin");
        verify(passwordEncoder).matches("incorrecto", "password-encriptado");
        verify(jwtProvider, never()).createToken(any(AuthUser.class));
    }

    @Test
    @DisplayName("Validate - retorna token si token es válido y usuario existe")
    void validate_TokenValidoYUsuarioExiste_RetornaToken() {
        String token = "token-valido";

        AuthUser usuario = AuthUser.builder()
                .id(1)
                .userName("admin")
                .password("password-encriptado")
                .build();

        when(jwtProvider.validate(token)).thenReturn(true);
        when(jwtProvider.getUserNameFromToken(token)).thenReturn("admin");
        when(authUserRepository.findByUserName("admin")).thenReturn(Optional.of(usuario));

        TokenDto resultado = authUserService.validate(token);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getToken()).isEqualTo(token);

        verify(jwtProvider).validate(token);
        verify(jwtProvider).getUserNameFromToken(token);
        verify(authUserRepository).findByUserName("admin");
    }

    @Test
    @DisplayName("Validate - retorna null si token es inválido")
    void validate_TokenInvalido_RetornaNull() {
        String token = "token-invalido";

        when(jwtProvider.validate(token)).thenReturn(false);

        TokenDto resultado = authUserService.validate(token);

        assertThat(resultado).isNull();

        verify(jwtProvider).validate(token);
        verify(jwtProvider, never()).getUserNameFromToken(anyString());
        verify(authUserRepository, never()).findByUserName(anyString());
    }

    @Test
    @DisplayName("Validate - retorna null si usuario del token no existe")
    void validate_TokenValidoPeroUsuarioNoExiste_RetornaNull() {
        String token = "token-valido";

        when(jwtProvider.validate(token)).thenReturn(true);
        when(jwtProvider.getUserNameFromToken(token)).thenReturn("admin");
        when(authUserRepository.findByUserName("admin")).thenReturn(Optional.empty());

        TokenDto resultado = authUserService.validate(token);

        assertThat(resultado).isNull();

        verify(jwtProvider).validate(token);
        verify(jwtProvider).getUserNameFromToken(token);
        verify(authUserRepository).findByUserName("admin");
    }
}