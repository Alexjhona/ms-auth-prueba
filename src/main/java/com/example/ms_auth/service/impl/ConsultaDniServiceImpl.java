package com.example.ms_auth.service.impl;

import com.example.ms_auth.dto.DniConsultaResponseDto;
import com.example.ms_auth.service.ConsultaDniService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@Service
public class ConsultaDniServiceImpl implements ConsultaDniService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${factiliza.api.base-url:https://api.factiliza.com/v1}")
    private String factilizaBaseUrl;

    @Value("${factiliza.api.token:}")
    private String factilizaToken;

    @Value("${factiliza.api.enabled:false}")
    private boolean factilizaEnabled;

    @Value("${apiperu.api.base-url:https://apiperu.dev/api}")
    private String apiPeruBaseUrl;

    @Value("${apiperu.api.token:}")
    private String apiPeruToken;

    public ConsultaDniServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public DniConsultaResponseDto consultar(String dni) {
        String dniLimpio = dni == null ? "" : dni.replaceAll("\\D", "");
        if (dniLimpio.length() != 8) {
            throw new IllegalArgumentException("El DNI debe tener 8 digitos");
        }

        String tokenFactiliza = obtenerToken(factilizaToken, "FACTILIZA_TOKEN");
        String tokenApiPeru = obtenerToken(apiPeruToken, "APIPERU_TOKEN");
        boolean tieneFactiliza = factilizaEnabled && tieneTexto(tokenFactiliza);
        boolean tieneApiPeru = tieneTexto(tokenApiPeru);

        if (!tieneFactiliza && !tieneApiPeru) {
            throw new IllegalStateException("Configura APIPERU_TOKEN para consultar DNI gratis");
        }

        Exception ultimoError = null;

        if (tieneApiPeru) {
            try {
                return consultarApiPeru(dniLimpio, tokenApiPeru);
            } catch (Exception ex) {
                ultimoError = ex;
            }
        }

        if (tieneFactiliza) {
            try {
                return consultarFactiliza(dniLimpio, tokenFactiliza);
            } catch (Exception ex) {
                ultimoError = ex;
            }
        }

        if (ultimoError instanceof IllegalArgumentException) {
            throw (IllegalArgumentException) ultimoError;
        }

        throw new IllegalArgumentException("No se pudo consultar el DNI en las APIs gratuitas configuradas");
    }

    private DniConsultaResponseDto consultarFactiliza(String dniLimpio, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token.trim());
            HttpEntity<Void> request = new HttpEntity<>(headers);
            String url = limpiarBaseUrl(factilizaBaseUrl, "https://api.factiliza.com/v1") + "/dni/info/" + dniLimpio;
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            if (!root.path("success").asBoolean(false)) {
                throw new IllegalArgumentException(root.path("message").asText("No se encontro el DNI"));
            }

            JsonNode data = root.path("data");
            return new DniConsultaResponseDto(
                    data.path("numero").asText(dniLimpio),
                    data.path("nombres").asText(""),
                    data.path("apellido_paterno").asText(""),
                    data.path("apellido_materno").asText(""),
                    data.path("nombre_completo").asText(""),
                    primerTexto(data, "direccion_completa", "direccion"),
                    data.path("distrito").asText(""),
                    data.path("provincia").asText(""),
                    data.path("departamento").asText(""),
                    primerTexto(data, "ciudad", "distrito", "provincia", "departamento"),
                    normalizarFecha(primerTexto(data, "fecha_nacimiento", "fechaNacimiento", "nacimiento", "fec_nacimiento")),
                    normalizarSexo(primerTexto(data, "sexo", "genero"))
            );
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo consultar el DNI en Factiliza");
        }
    }

    private DniConsultaResponseDto consultarApiPeru(String dniLimpio, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token.trim());
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

            Map<String, String> body = new HashMap<>();
            body.put("dni", dniLimpio);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            String url = limpiarBaseUrl(apiPeruBaseUrl, "https://apiperu.dev/api") + "/dni";
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            if (!root.path("success").asBoolean(false)) {
                throw new IllegalArgumentException(root.path("message").asText("No se encontro el DNI"));
            }

            JsonNode data = root.path("data");
            return new DniConsultaResponseDto(
                    data.path("numero").asText(dniLimpio),
                    data.path("nombres").asText(""),
                    data.path("apellido_paterno").asText(""),
                    data.path("apellido_materno").asText(""),
                    data.path("nombre_completo").asText(""),
                    "",
                    "",
                    "",
                    "",
                    primerTexto(data, "ciudad", "distrito", "provincia", "departamento"),
                    normalizarFecha(primerTexto(data, "fecha_nacimiento", "fechaNacimiento", "nacimiento", "fec_nacimiento")),
                    normalizarSexo(primerTexto(data, "sexo", "genero"))
            );
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo consultar el DNI en ApiPeruDev");
        }
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    private String obtenerToken(String tokenConfigurado, String variableEntorno) {
        if (tieneTexto(tokenConfigurado)) {
            return tokenConfigurado.trim();
        }
        String tokenEntorno = System.getenv(variableEntorno);
        return tokenEntorno == null ? "" : tokenEntorno.trim();
    }

    private String limpiarBaseUrl(String valor, String porDefecto) {
        return valor == null ? porDefecto : valor.replaceAll("/+$", "");
    }

    private String primerTexto(JsonNode data, String... campos) {
        for (String campo : campos) {
            String valor = data.path(campo).asText("");
            if (valor != null && !valor.trim().isEmpty() && !"null".equalsIgnoreCase(valor.trim())) {
                return valor.trim();
            }
        }
        return "";
    }

    private String normalizarFecha(String valor) {
        if (!tieneTexto(valor)) {
            return "";
        }

        String texto = valor.trim();
        DateTimeFormatter[] formatos = new DateTimeFormatter[] {
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd")
        };

        for (DateTimeFormatter formato : formatos) {
            try {
                return LocalDate.parse(texto, formato).format(DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException ex) {
                // Probamos el siguiente formato conocido.
            }
        }

        return texto;
    }

    private String normalizarSexo(String valor) {
        if (!tieneTexto(valor)) {
            return "";
        }

        String texto = valor.trim().toUpperCase();
        if ("M".equals(texto) || texto.startsWith("MASC")) {
            return "MASCULINO";
        }
        if ("F".equals(texto) || texto.startsWith("FEM")) {
            return "FEMENINO";
        }

        return texto;
    }
}
