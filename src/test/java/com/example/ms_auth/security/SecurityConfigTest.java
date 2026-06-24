package com.example.ms_auth.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    @DisplayName("CORS permite frontends locales y headers de Authorization")
    void corsConfigurationSource_ReturnsExpectedPolicy() {
        SecurityConfig securityConfig = new SecurityConfig();

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/auth/login"));

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOriginPatterns())
                .contains("http://localhost:*", "http://127.0.0.1:*", "https://*.trycloudflare.com");
        assertThat(configuration.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(configuration.getAllowedHeaders()).contains("*");
        assertThat(configuration.getExposedHeaders()).contains("Authorization");
        assertThat(configuration.getAllowCredentials()).isFalse();
    }
}
