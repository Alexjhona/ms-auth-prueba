package com.example.ms_auth.security;

import com.example.ms_auth.entity.AuthUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider(long expiration) {
        JwtProvider jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "secret", "secret-test-auth-service");
        ReflectionTestUtils.setField(jwtProvider, "expiration", expiration);
        jwtProvider.init();
        return jwtProvider;
    }

    @Test
    @DisplayName("Crear y validar token JWT correctamente")
    void createToken_Validate_GetUserName() {
        JwtProvider jwtProvider = jwtProvider(3600000L);

        AuthUser authUser = AuthUser.builder()
                .id(1)
                .userName("admin")
                .password("123456")
                .rol("admin")
                .nombre("Ana")
                .apellido("Lopez")
                .build();

        String token = jwtProvider.createToken(authUser);

        assertThat(token).isNotBlank();
        assertThat(jwtProvider.validate(token)).isTrue();
        assertThat(jwtProvider.getUserNameFromToken(token)).isEqualTo("admin");
        assertThat(jwtProvider.getRolFromToken(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Validar token inválido retorna false")
    void validate_TokenInvalido_RetornaFalse() {
        JwtProvider jwtProvider = jwtProvider(3600000L);

        boolean resultado = jwtProvider.validate("token-invalido");

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("Obtener usuario desde token inválido retorna bad token")
    void getUserNameFromToken_TokenInvalido_RetornaBadToken() {
        JwtProvider jwtProvider = jwtProvider(3600000L);

        String resultado = jwtProvider.getUserNameFromToken("token-invalido");

        assertThat(resultado).isEqualTo("bad token");
    }

    @Test
    @DisplayName("Obtener rol desde token inválido retorna vacío")
    void getRolFromToken_TokenInvalido_RetornaEmpty() {
        JwtProvider jwtProvider = jwtProvider(3600000L);

        assertThat(jwtProvider.getRolFromToken("token-invalido")).isEmpty();
    }

    @Test
    @DisplayName("Crear token usa ADMIN para rol legacy vacío")
    void createToken_WhenRoleIsBlank_UsesAdminRole() {
        JwtProvider jwtProvider = jwtProvider(3600000L);
        AuthUser authUser = AuthUser.builder()
                .id(1)
                .userName("legacy")
                .password("123456")
                .rol(" ")
                .build();

        String token = jwtProvider.createToken(authUser);

        assertThat(jwtProvider.getRolFromToken(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Token expirado no valida")
    void validate_WhenTokenIsExpired_ReturnsFalse() {
        JwtProvider jwtProvider = jwtProvider(-1L);
        AuthUser authUser = AuthUser.builder()
                .id(1)
                .userName("admin")
                .password("123456")
                .build();

        String token = jwtProvider.createToken(authUser);

        assertThat(jwtProvider.validate(token)).isFalse();
    }
}
