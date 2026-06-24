package com.example.ms_auth.exception;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;
import org.springframework.http.converter.HttpMessageNotReadableException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException exception,
                                                                      HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(errorBadRequest(
                "Se encontraron errores de validación",
                request,
                obtenerDatosRecibidos(request, exception.getBindingResult().getTarget()),
                errores
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidJson(HttpMessageNotReadableException exception,
                                                                 HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        String campo = obtenerCampoConTipoInvalido(exception);
        errores.put(campo == null ? "json" : campo, campo == null ? "JSON inválido o mal formado" : "Tipo de dato inválido");

        return ResponseEntity.badRequest().body(errorBadRequest(
                "Se encontraron errores de validación",
                request,
                obtenerDatosRecibidos(request, null),
                errores
        ));
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            MissingPathVariableException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception exception, HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();

        if (exception instanceof MissingServletRequestParameterException) {
            errores.put(((MissingServletRequestParameterException) exception).getParameterName(), "Parámetro obligatorio");
        } else if (exception instanceof MethodArgumentTypeMismatchException) {
            errores.put(((MethodArgumentTypeMismatchException) exception).getName(), "Tipo de dato inválido");
        } else if (exception instanceof MissingPathVariableException) {
            errores.put(((MissingPathVariableException) exception).getVariableName(), "Parámetro obligatorio");
        } else if (exception instanceof ConstraintViolationException) {
            ((ConstraintViolationException) exception).getConstraintViolations().forEach(violation ->
                    errores.putIfAbsent(obtenerUltimoSegmento(violation.getPropertyPath().toString()), violation.getMessage()));
        } else {
            errores.put("general", exception.getMessage() == null ? "Solicitud inválida" : exception.getMessage());
        }

        return ResponseEntity.badRequest().body(errorBadRequest(
                "Se encontraron errores de validación",
                request,
                obtenerDatosRecibidos(request, null),
                errores
        ));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(RecursoNoEncontradoException exception,
                                                              HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBasico(HttpStatus.NOT_FOUND, "No se encontró el recurso solicitado", request));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFound(NoHandlerFoundException exception,
                                                                    HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBasico(HttpStatus.NOT_FOUND, "No se encontró el recurso solicitado", request));
    }

    @ExceptionHandler({ConflictoRecursoException.class, DataIntegrityViolationException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBasico(HttpStatus.CONFLICT, "El registro ya existe o genera conflicto", request));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception,
                                                                      HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorBasico(HttpStatus.NOT_FOUND, "No se encontró el recurso solicitado", request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBasico(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado en el servidor", request));
    }

    private Map<String, Object> errorBadRequest(String mensaje,
                                                HttpServletRequest request,
                                                Object datosRecibidos,
                                                Map<String, String> errores) {
        Map<String, Object> response = errorBasico(HttpStatus.BAD_REQUEST, mensaje, request);
        response.put("datosRecibidos", datosRecibidos);
        response.put("errores", errores);
        return response;
    }

    private Map<String, Object> errorBasico(HttpStatus status, String mensaje, HttpServletRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("mensaje", mensaje);
        response.put("ruta", request.getRequestURI());
        return response;
    }

    private Object obtenerDatosRecibidos(HttpServletRequest request, Object target) {
        String body = obtenerBody(request);
        if (!body.isEmpty()) {
            try {
                return objectMapper.readValue(body, new TypeReference<LinkedHashMap<String, Object>>() {
                });
            } catch (Exception ignored) {
                Map<String, Object> datos = new LinkedHashMap<>();
                datos.put("body", body);
                return datos;
            }
        }

        Map<String, Object> parametros = obtenerParametros(request);
        if (!parametros.isEmpty()) {
            return parametros;
        }

        if (target != null) {
            return objectMapper.convertValue(target, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        }

        return Collections.emptyMap();
    }

    private String obtenerBody(HttpServletRequest request) {
        ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
        if (wrapper == null || wrapper.getContentAsByteArray().length == 0) {
            return "";
        }

        Charset charset = Charset.forName(wrapper.getCharacterEncoding() == null ? "UTF-8" : wrapper.getCharacterEncoding());
        return new String(wrapper.getContentAsByteArray(), charset).trim();
    }

    private Map<String, Object> obtenerParametros(HttpServletRequest request) {
        Map<String, Object> parametros = new LinkedHashMap<>();
        request.getParameterMap().forEach((nombre, valores) -> parametros.put(nombre, convertirValores(valores)));
        return parametros;
    }

    private Object convertirValores(String[] valores) {
        if (valores == null || valores.length == 0) {
            return "";
        }
        if (valores.length == 1) {
            return valores[0];
        }
        return Arrays.asList(valores);
    }

    private String obtenerCampoConTipoInvalido(Throwable exception) {
        Throwable actual = exception;
        while (actual != null) {
            if (actual instanceof JsonMappingException) {
                List<JsonMappingException.Reference> path = ((JsonMappingException) actual).getPath();
                if (!path.isEmpty()) {
                    String campo = path.stream()
                            .map(JsonMappingException.Reference::getFieldName)
                            .filter(nombre -> nombre != null && !nombre.isEmpty())
                            .collect(Collectors.joining("."));
                    return campo.isEmpty() ? null : campo;
                }
            }
            actual = actual.getCause();
        }
        return null;
    }

    private String obtenerUltimoSegmento(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "general";
        }
        int index = path.lastIndexOf('.');
        return index >= 0 ? path.substring(index + 1) : path;
    }
}
