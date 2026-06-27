package com.example.ms_auth.service.impl;

import com.example.ms_auth.dto.DniConsultaResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ConsultaDniServiceImplTest {

    private ConsultaDniServiceImpl service;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        service = new ConsultaDniServiceImpl(new ObjectMapper());
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
        ReflectionTestUtils.setField(service, "factilizaBaseUrl", "https://factiliza.test/");
        ReflectionTestUtils.setField(service, "apiPeruBaseUrl", "https://apiperu.test/");
        ReflectionTestUtils.setField(service, "factilizaToken", "");
        ReflectionTestUtils.setField(service, "apiPeruToken", "api-token");
        ReflectionTestUtils.setField(service, "factilizaEnabled", false);
    }

    @Test
    void rejectsInvalidDni() {
        assertThatThrownBy(() -> service.consultar("12-34"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El DNI debe tener 8 digitos");
    }

    @Test
    void rejectsRequestWithoutConfiguredProvider() {
        ReflectionTestUtils.setField(service, "apiPeruToken", " ");

        assertThatThrownBy(() -> service.consultar("12345678"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APIPERU_TOKEN");
    }

    @Test
    void mapsSuccessfulApiPeruResponse() {
        server.expect(once(), requestTo("https://apiperu.test/dni"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"success\":true,\"data\":{" +
                        "\"numero\":\"12345678\",\"nombres\":\"ANA\"," +
                        "\"apellido_paterno\":\"PEREZ\",\"apellido_materno\":\"LOPEZ\"," +
                        "\"nombre_completo\":\"ANA PEREZ LOPEZ\",\"ciudad\":\"LIMA\"," +
                        "\"fecha_nacimiento\":\"31/12/2000\",\"sexo\":\"F\"}}",
                        MediaType.APPLICATION_JSON));

        DniConsultaResponseDto response = service.consultar("12345678");

        assertThat(response.getDni()).isEqualTo("12345678");
        assertThat(response.getNombres()).isEqualTo("ANA");
        assertThat(response.getFechaNacimiento()).isEqualTo("2000-12-31");
        assertThat(response.getSexo()).isEqualTo("FEMENINO");
        server.verify();
    }

    @Test
    void fallsBackToFactilizaAndMapsAlternativeFields() {
        ReflectionTestUtils.setField(service, "factilizaEnabled", true);
        ReflectionTestUtils.setField(service, "factilizaToken", "fact-token");
        server.expect(once(), requestTo("https://apiperu.test/dni"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"success\":false,\"message\":\"No encontrado\"}",
                        MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo("https://factiliza.test/dni/info/12345678"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"success\":true,\"data\":{" +
                        "\"numero\":\"12345678\",\"nombres\":\"JUAN\"," +
                        "\"direccion\":\"AV UNO\",\"distrito\":\"MIRAFLORES\"," +
                        "\"fechaNacimiento\":\"2001-01-02\",\"genero\":\"masculino\"}}",
                        MediaType.APPLICATION_JSON));

        DniConsultaResponseDto response = service.consultar("12345678");

        assertThat(response.getDireccion()).isEqualTo("AV UNO");
        assertThat(response.getCiudad()).isEqualTo("MIRAFLORES");
        assertThat(response.getFechaNacimiento()).isEqualTo("2001-01-02");
        assertThat(response.getSexo()).isEqualTo("MASCULINO");
        server.verify();
    }

    @Test
    void propagatesProviderBusinessError() {
        server.expect(requestTo("https://apiperu.test/dni"))
                .andRespond(withSuccess("{\"success\":false,\"message\":\"DNI inexistente\"}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.consultar("12345678"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("DNI inexistente");
        server.verify();
    }
}
