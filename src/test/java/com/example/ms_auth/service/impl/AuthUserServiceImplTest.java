package com.example.ms_auth.service.impl;

import com.example.ms_auth.dto.AuthUserDto;
import com.example.ms_auth.entity.AuthUser;
import com.example.ms_auth.entity.TokenDto;
import com.example.ms_auth.exception.ConflictoRecursoException;
import com.example.ms_auth.exception.RecursoNoEncontradoException;
import com.example.ms_auth.repository.AuthUserRepository;
import com.example.ms_auth.security.JwtProvider;
import com.example.ms_auth.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @DisplayName("Guardar usuario - lanza conflicto si usuario ya existe")
    void save_CuandoUsuarioYaExiste_LanzaConflicto() {
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

        assertThatThrownBy(() -> authUserService.save(dto))
                .isInstanceOf(ConflictoRecursoException.class);

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

    @Test
    @DisplayName("Guardar usuario - normaliza rol subadmin y limpia textos")
    void save_WhenRoleHasSpaces_NormalizesRoleAndTrimsFields() {
        AuthUserDto dto = TestDataFactory.validUserDto("  supervisor  ", "123456");
        dto.setRol("sub-admin");
        dto.setNombre(" Ana ");
        dto.setCorreo(" ana@test.com ");

        when(authUserRepository.findByUserName("  supervisor  ")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("encoded");
        when(authUserRepository.save(any(AuthUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthUser result = authUserService.save(dto);

        assertThat(result.getUserName()).isEqualTo("  supervisor  ");
        assertThat(result.getNombre()).isEqualTo("Ana");
        assertThat(result.getCorreo()).isEqualTo("ana@test.com");
        assertThat(result.getRol()).isEqualTo("SUB_ADMIN");
        assertThat(result.getActivo()).isTrue();
    }

    @Test
    @DisplayName("Guardar usuario - lanza Bad Request si rol no existe")
    void save_WhenRoleIsUnknown_ThrowsIllegalArgumentException() {
        AuthUserDto dto = TestDataFactory.validUserDto("admin", "123456");
        dto.setRol("GERENTE");

        when(authUserRepository.findByUserName("admin")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authUserService.save(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rol no permitido");

        verify(authUserRepository, never()).save(any(AuthUser.class));
    }

    @Test
    @DisplayName("Guardar usuario - lanza conflicto si DNI ya existe")
    void save_WhenDniAlreadyExists_ThrowsConflict() {
        AuthUserDto dto = TestDataFactory.validUserDto("nuevo", "123456");
        AuthUser existing = TestDataFactory.user("otro", "encoded");
        existing.setId(10);

        when(authUserRepository.findByUserName("nuevo")).thenReturn(Optional.empty());
        when(authUserRepository.findAll()).thenReturn(java.util.List.of(existing));

        assertThatThrownBy(() -> authUserService.save(dto))
                .isInstanceOf(ConflictoRecursoException.class);

        verify(authUserRepository, never()).save(any(AuthUser.class));
    }

    @Test
    @DisplayName("Login - permite credencial por correo ignorando mayusculas")
    void login_WhenCredentialIsEmail_ReturnsToken() {
        AuthUserDto dto = AuthUserDto.builder()
                .correo("ana@test.com")
                .password("123456")
                .build();
        AuthUser user = TestDataFactory.user("ana", "encoded");

        when(authUserRepository.findByUserName("ana@test.com")).thenReturn(Optional.empty());
        when(authUserRepository.findByCorreoIgnoreCase("ana@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "encoded")).thenReturn(true);
        when(jwtProvider.createToken(user)).thenReturn("token");

        TokenDto result = authUserService.login(dto);

        assertThat(result.getToken()).isEqualTo("token");
    }

    @Test
    @DisplayName("Login - trabajador inactivo con rol configurado no recibe token")
    void login_WhenUserIsInactive_ReturnsNull() {
        AuthUserDto dto = new AuthUserDto("ana", "123456");
        AuthUser user = TestDataFactory.user("ana", "encoded");
        user.setActivo(false);

        when(authUserRepository.findByUserName("ana")).thenReturn(Optional.of(user));

        assertThat(authUserService.login(dto)).isNull();

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Activar trabajador - actualiza usuario, password y estado")
    void activarTrabajador_WhenDataIsValid_ActivatesUser() {
        AuthUser existing = TestDataFactory.user("temporal", "old");
        existing.setId(5);
        existing.setActivo(false);

        when(authUserRepository.findByCorreoIgnoreCase("ana@test.com")).thenReturn(Optional.of(existing));
        when(authUserRepository.findByUserName("ana")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("encoded");
        when(authUserRepository.save(existing)).thenReturn(existing);

        AuthUser result = authUserService.activarTrabajador(" ana@test.com ", " ana ", " 123456 ");

        assertThat(result.getUserName()).isEqualTo("ana");
        assertThat(result.getCorreo()).isEqualTo("ana@test.com");
        assertThat(result.getPassword()).isEqualTo("encoded");
        assertThat(result.getActivo()).isTrue();
    }

    @Test
    @DisplayName("Activar trabajador - lanza 404 si correo no existe")
    void activarTrabajador_WhenUserDoesNotExist_ThrowsNotFound() {
        when(authUserRepository.findByCorreoIgnoreCase("missing@test.com")).thenReturn(Optional.empty());
        when(authUserRepository.findByUserName("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authUserService.activarTrabajador("missing@test.com", "ana", "123456"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Actualizar trabajador - cambia datos sin sobrescribir password vacio")
    void actualizar_WhenPasswordIsBlank_DoesNotEncodePassword() {
        AuthUser existing = TestDataFactory.user("ana", "encoded-old");
        existing.setId(7);
        AuthUserDto dto = TestDataFactory.validUserDto("ana2", " ");
        dto.setDni("22222222");
        dto.setCorreo("ana2@test.com");

        when(authUserRepository.findById(7)).thenReturn(Optional.of(existing));
        when(authUserRepository.findByUserName("ana2")).thenReturn(Optional.empty());
        when(authUserRepository.save(existing)).thenReturn(existing);

        AuthUser result = authUserService.actualizar(7, dto);

        assertThat(result.getUserName()).isEqualTo("ana2");
        assertThat(result.getPassword()).isEqualTo("encoded-old");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Eliminar trabajador - lanza 404 si no existe")
    void eliminar_WhenUserDoesNotExist_ThrowsNotFound() {
        when(authUserRepository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> authUserService.eliminar(99))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(authUserRepository, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("Impersonar trabajador - genera token para usuario activo")
    void impersonarTrabajador_WhenUserIsActive_ReturnsToken() {
        AuthUser user = TestDataFactory.user("ana", "encoded");
        user.setId(2);

        when(authUserRepository.findById(2)).thenReturn(Optional.of(user));
        when(jwtProvider.createToken(user)).thenReturn("impersonated");

        TokenDto result = authUserService.impersonarTrabajador(2);

        assertThat(result.getToken()).isEqualTo("impersonated");
    }
}
