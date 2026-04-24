package com.example.ms_auth.security;

import com.example.ms_auth.entity.AuthUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    @Test
    @DisplayName("Crear y validar token JWT correctamente")
    void createToken_Validate_GetUserName() {
        JwtProvider jwtProvider = new JwtProvider();

        ReflectionTestUtils.setField(jwtProvider, "secret", "secret-test-auth-service");
        ReflectionTestUtils.setField(jwtProvider, "expiration", 3600000L);
        jwtProvider.init();

        AuthUser authUser = AuthUser.builder()
                .id(1)
                .userName("admin")
                .password("123456")
                .build();

        String token = jwtProvider.createToken(authUser);

        assertThat(token).isNotBlank();
        assertThat(jwtProvider.validate(token)).isTrue();
        assertThat(jwtProvider.getUserNameFromToken(token)).isEqualTo("admin");
    }

    @Test
    @DisplayName("Validar token inválido retorna false")
    void validate_TokenInvalido_RetornaFalse() {
        JwtProvider jwtProvider = new JwtProvider();

        ReflectionTestUtils.setField(jwtProvider, "secret", "secret-test-auth-service");
        ReflectionTestUtils.setField(jwtProvider, "expiration", 3600000L);
        jwtProvider.init();

        boolean resultado = jwtProvider.validate("token-invalido");

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("Obtener usuario desde token inválido retorna bad token")
    void getUserNameFromToken_TokenInvalido_RetornaBadToken() {
        JwtProvider jwtProvider = new JwtProvider();

        ReflectionTestUtils.setField(jwtProvider, "secret", "secret-test-auth-service");
        ReflectionTestUtils.setField(jwtProvider, "expiration", 3600000L);
        jwtProvider.init();

        String resultado = jwtProvider.getUserNameFromToken("token-invalido");

        assertThat(resultado).isEqualTo("bad token");
    }
}