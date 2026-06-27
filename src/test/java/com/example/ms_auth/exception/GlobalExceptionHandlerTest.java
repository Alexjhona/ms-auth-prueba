package com.example.ms_auth.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void validationErrors_IncludesFirstFieldErrorAndTarget() {
        Map<String, Object> target = Map.of("userName", "");
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(target, "authUserDto");
        binding.addError(new FieldError("authUserDto", "userName", "Campo obligatorio"));
        binding.addError(new FieldError("authUserDto", "userName", "Repetido"));

        ResponseEntity<Map<String, Object>> response = handler.handleValidationErrors(
                new MethodArgumentNotValidException(null, binding),
                request("/auth/create")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsEntry("datosRecibidos", target)
                .containsEntry("ruta", "/auth/create");
        assertThat(errors(response)).containsEntry("userName", "Campo obligatorio");
    }

    @Test
    void invalidJson_ReportsMappedFieldAndRawBody() throws Exception {
        JsonMappingException mapping = mock(JsonMappingException.class);
        when(mapping.getPath()).thenReturn(List.of(
                new JsonMappingException.Reference(new Object(), "activo")
        ));
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
        when(exception.getCause()).thenReturn(mapping);

        ResponseEntity<Map<String, Object>> response = handler.handleInvalidJson(
                exception,
                cachedRequest("/auth/trabajadores/1/estado", "{\"activo\":x}")
        );

        assertThat(errors(response)).containsEntry("activo", "Tipo de dato inválido");
        assertThat(data(response)).containsEntry("body", "{\"activo\":x}");
    }

    @Test
    void invalidJson_WithoutMapping_ReportsMalformedJson() {
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);

        ResponseEntity<Map<String, Object>> response =
                handler.handleInvalidJson(exception, request("/auth/create"));

        assertThat(errors(response)).containsEntry("json", "JSON inválido o mal formado");
        assertThat(data(response)).isEmpty();
    }

    @Test
    void badRequest_HandlesMissingAndMismatchedParameters() {
        ResponseEntity<Map<String, Object>> missing = handler.handleBadRequest(
                new MissingServletRequestParameterException("token", "String"),
                request("/auth/validate")
        );
        MethodArgumentTypeMismatchException mismatch = mock(MethodArgumentTypeMismatchException.class);
        when(mismatch.getName()).thenReturn("id");

        ResponseEntity<Map<String, Object>> invalidType =
                handler.handleBadRequest(mismatch, request("/auth/trabajadores/x"));

        assertThat(errors(missing)).containsEntry("token", "Parámetro obligatorio");
        assertThat(errors(invalidType)).containsEntry("id", "Tipo de dato inválido");
    }

    @Test
    void badRequest_UsesParametersAndGenericMessages() {
        MockHttpServletRequest request = request("/auth/test");
        request.addParameter("rol", "ADMIN", "OWNER");

        ResponseEntity<Map<String, Object>> withMessage =
                handler.handleBadRequest(new IllegalArgumentException("Inválido"), request);
        ResponseEntity<Map<String, Object>> withoutMessage =
                handler.handleBadRequest(new IllegalArgumentException(), request("/auth/test"));

        assertThat(data(withMessage)).containsEntry("rol", List.of("ADMIN", "OWNER"));
        assertThat(errors(withMessage)).containsEntry("general", "Inválido");
        assertThat(errors(withoutMessage)).containsEntry("general", "Solicitud inválida");
    }

    @Test
    void specificHandlers_ReturnExpectedStatuses() {
        MockHttpServletRequest request = request("/auth/test");

        assertThat(handler.handleNotFound(new RecursoNoEncontradoException(), request).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleNoHandlerFound(
                new NoHandlerFoundException("GET", "/auth/test", null), request).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleConflict(new ConflictoRecursoException(), request).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(handler.handleMethodNotAllowed(
                new HttpRequestMethodNotSupportedException("TRACE"), request).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleUnexpected(new RuntimeException(), request).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }

    private ContentCachingRequestWrapper cachedRequest(String uri, String body) throws Exception {
        MockHttpServletRequest request = request(uri);
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request);
        wrapper.getInputStream().readAllBytes();
        return wrapper;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> errors(ResponseEntity<Map<String, Object>> response) {
        return (Map<String, String>) response.getBody().get("errores");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(ResponseEntity<Map<String, Object>> response) {
        return (Map<String, Object>) response.getBody().get("datosRecibidos");
    }
}
