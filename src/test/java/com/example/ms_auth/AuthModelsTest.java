package com.example.ms_auth;

import com.example.ms_auth.dto.AuthUserDto;
import com.example.ms_auth.dto.AuthUserResponseDto;
import com.example.ms_auth.entity.AuthUser;
import com.example.ms_auth.entity.TokenDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthModelsTest {

    @Test
    @DisplayName("AuthUserDto - constructores, builder, getters y setters")
    void authUserDto_Test() {
        AuthUserDto vacio = new AuthUserDto();
        vacio.setUserName("admin");
        vacio.setPassword("123456");

        assertThat(vacio.getUserName()).isEqualTo("admin");
        assertThat(vacio.getPassword()).isEqualTo("123456");

        AuthUserDto completo = new AuthUserDto("user", "pass");

        assertThat(completo.getUserName()).isEqualTo("user");
        assertThat(completo.getPassword()).isEqualTo("pass");

        AuthUserDto builder = AuthUserDto.builder()
                .userName("builderUser")
                .password("builderPass")
                .build();

        assertThat(builder.getUserName()).isEqualTo("builderUser");
        assertThat(builder.getPassword()).isEqualTo("builderPass");
        assertThat(builder.toString()).contains("builderUser");
        assertThat(builder).isEqualTo(new AuthUserDto("builderUser", "builderPass"));
        assertThat(builder.hashCode()).isEqualTo(new AuthUserDto("builderUser", "builderPass").hashCode());
        assertThat(AuthUserDto.builder().userName("x").password("y").toString()).contains("AuthUserDto");
    }

    @Test
    @DisplayName("AuthUserResponseDto - constructores, getters y setters")
    void authUserResponseDto_Test() {
        AuthUserResponseDto vacio = new AuthUserResponseDto();
        vacio.setId(1);
        vacio.setUserName("admin");

        assertThat(vacio.getId()).isEqualTo(1);
        assertThat(vacio.getUserName()).isEqualTo("admin");

        AuthUserResponseDto completo = new AuthUserResponseDto(2, "user");

        assertThat(completo.getId()).isEqualTo(2);
        assertThat(completo.getUserName()).isEqualTo("user");
        assertThat(completo.toString()).contains("user");
        assertThat(completo).isEqualTo(new AuthUserResponseDto(2, "user"));
        assertThat(completo.hashCode()).isEqualTo(new AuthUserResponseDto(2, "user").hashCode());
    }

    @Test
    @DisplayName("AuthUser entity - constructores, builder, getters y setters")
    void authUserEntity_Test() {
        AuthUser vacio = new AuthUser();
        vacio.setId(1);
        vacio.setUserName("admin");
        vacio.setPassword("123456");

        assertThat(vacio.getId()).isEqualTo(1);
        assertThat(vacio.getUserName()).isEqualTo("admin");
        assertThat(vacio.getPassword()).isEqualTo("123456");

        AuthUser completo = new AuthUser(2, "user", "pass");

        assertThat(completo.getId()).isEqualTo(2);
        assertThat(completo.getUserName()).isEqualTo("user");
        assertThat(completo.getPassword()).isEqualTo("pass");

        AuthUser.AuthUserBuilder builder = AuthUser.builder()
                .id(3)
                .userName("builderUser")
                .password("builderPass");

        assertThat(builder.toString()).contains("AuthUser");

        AuthUser construido = builder.build();

        assertThat(construido.getId()).isEqualTo(3);
        assertThat(construido.getUserName()).isEqualTo("builderUser");
        assertThat(construido.getPassword()).isEqualTo("builderPass");
        assertThat(construido.toString()).contains("builderUser");
        assertThat(construido).isEqualTo(new AuthUser(3, "builderUser", "builderPass"));
        assertThat(construido.hashCode()).isEqualTo(new AuthUser(3, "builderUser", "builderPass").hashCode());
    }

    @Test
    @DisplayName("TokenDto - constructores, getters y setters")
    void tokenDto_Test() {
        TokenDto vacio = new TokenDto();
        vacio.setToken("token-uno");

        assertThat(vacio.getToken()).isEqualTo("token-uno");

        TokenDto completo = new TokenDto("token-dos");

        assertThat(completo.getToken()).isEqualTo("token-dos");
        assertThat(completo.toString()).contains("token-dos");
        assertThat(completo).isEqualTo(new TokenDto("token-dos"));
        assertThat(completo.hashCode()).isEqualTo(new TokenDto("token-dos").hashCode());
    }
}