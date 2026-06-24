package com.example.ms_auth.controller;

import com.example.ms_auth.dto.AuthUserDto;
import com.example.ms_auth.dto.AuthUserResponseDto;
import com.example.ms_auth.dto.ActivarTrabajadorRequest;
import com.example.ms_auth.dto.DniConsultaResponseDto;
import com.example.ms_auth.dto.EnviarInvitacionRequest;
import com.example.ms_auth.entity.AuthUser;
import com.example.ms_auth.entity.TokenDto;
import com.example.ms_auth.service.ConsultaDniService;
import com.example.ms_auth.service.EmailService;
import com.example.ms_auth.service.AuthUserService;
import com.example.ms_auth.security.JwtProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticacion", description = "Endpoints para iniciar sesion, crear usuarios y validar tokens JWT.")
public class AuthUserController {

    @Autowired
    AuthUserService authUserService;

    @Autowired
    JwtProvider jwtProvider;

    @Autowired
    ConsultaDniService consultaDniService;

    @Autowired
    EmailService emailService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion", description = "Autentica las credenciales de un usuario y devuelve un token JWT temporal para consumir rutas protegidas del sistema. El body debe incluir userName y password.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login correcto, devuelve token JWT"),
            @ApiResponse(responseCode = "400", description = "Credenciales invalidas")
    })
    public ResponseEntity<TokenDto> login(@Valid @RequestBody AuthUserDto authUserDto) {
        TokenDto tokenDto = authUserService.login(authUserDto);
        if (tokenDto == null) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        return ResponseEntity.ok(tokenDto);
    }

    @PostMapping("/validate")
    @Operation(summary = "Validar token JWT", description = "Verifica si un token JWT enviado por el cliente es valido para autorizar solicitudes protegidas. Este endpoint es consumido por el API Gateway para proteger rutas /api/**.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token valido"),
            @ApiResponse(responseCode = "400", description = "Token invalido o expirado")
    })
    public ResponseEntity<TokenDto> validate(@Parameter(description = "Token JWT temporal que se validara para autorizar solicitudes protegidas. No compartir ni registrar tokens reales.", example = "JWT_TEMPORAL") @RequestParam String token) {
        TokenDto tokenDto = authUserService.validate(token);
        if (tokenDto == null) {
            throw new IllegalArgumentException("Token inválido");
        }
        return ResponseEntity.ok(tokenDto);
    }

    @PostMapping("/create")
    @Operation(summary = "Crear usuario", description = "Registra un nuevo usuario del sistema con sus credenciales de acceso. Usar credenciales de prueba en ambientes de validacion y no registrar datos sensibles en documentacion.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos de usuario invalidos")
    })
    public ResponseEntity<AuthUserResponseDto> create(@Valid @RequestBody AuthUserDto authUserDto) {
        authUserDto.setRol("VENDEDOR");
        authUserDto.setActivo(true);
        AuthUser authUser = authUserService.save(authUserDto);

        AuthUserResponseDto response = toResponse(authUser);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/dni/{dni}")
    public ResponseEntity<?> consultarDni(@PathVariable String dni) {
        DniConsultaResponseDto response = consultaDniService.consultar(dni);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trabajadores")
    public ResponseEntity<?> listarTrabajadores(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!esAdmin(authorization)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(authUserService.listar().stream().map(this::toResponse).toList());
    }

    @PostMapping("/trabajadores")
    public ResponseEntity<?> crearTrabajador(@RequestHeader(value = "Authorization", required = false) String authorization,
                                             @Valid @RequestBody AuthUserDto authUserDto) {
        if (!esAdmin(authorization)) {
            return ResponseEntity.status(403).build();
        }

        AuthUser authUser = authUserService.save(authUserDto);
        return ResponseEntity.ok(toResponse(authUser));
    }

    @PostMapping("/trabajadores/activar")
    public ResponseEntity<?> activarTrabajador(@Valid @RequestBody ActivarTrabajadorRequest request) {
        AuthUser authUser = authUserService.activarTrabajador(request.getCorreo(), request.getUserName(), request.getPassword());
        return ResponseEntity.ok(toResponse(authUser));
    }

    @PostMapping("/trabajadores/enviar-invitacion")
    public ResponseEntity<?> enviarInvitacionTrabajador(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                        @Valid @RequestBody EnviarInvitacionRequest request) {
        if (!esAdmin(authorization)) {
            return ResponseEntity.status(403).build();
        }

        emailService.enviarInvitacionTrabajador(request);
        return ResponseEntity.ok(Map.of("mensaje", "Invitacion enviada correctamente"));
    }

    @PostMapping("/trabajadores/{id}/impersonar")
    public ResponseEntity<?> impersonarTrabajador(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                  @PathVariable Integer id) {
        if (!esAdmin(authorization)) {
            return ResponseEntity.status(403).build();
        }

        TokenDto tokenDto = authUserService.impersonarTrabajador(id);
        return ResponseEntity.ok(tokenDto);
    }

    @PutMapping("/trabajadores/{id}")
    public ResponseEntity<?> actualizarTrabajador(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                  @PathVariable Integer id,
                                                  @Valid @RequestBody AuthUserDto authUserDto) {
        if (!esAdmin(authorization)) {
            return ResponseEntity.status(403).build();
        }

        AuthUser authUser = authUserService.actualizar(id, authUserDto);
        return ResponseEntity.ok(toResponse(authUser));
    }

    @PatchMapping("/trabajadores/{id}/estado")
    public ResponseEntity<?> cambiarEstadoTrabajador(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                     @PathVariable Integer id,
                                                     @RequestParam boolean activo) {
        if (!esAdmin(authorization)) {
            return ResponseEntity.status(403).build();
        }

        AuthUser authUser = authUserService.cambiarEstado(id, activo);
        return ResponseEntity.ok(toResponse(authUser));
    }

    @DeleteMapping("/trabajadores/{id}")
    public ResponseEntity<?> eliminarTrabajador(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                @PathVariable Integer id) {
        if (!esAdmin(authorization)) {
            return ResponseEntity.status(403).build();
        }

        authUserService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private boolean esAdmin(String authorization) {
        if (!esTokenValido(authorization)) {
            return false;
        }

        String token = authorization.substring(7);
        String rol = jwtProvider.getRolFromToken(token);
        return "ADMIN".equals(rol) || "OWNER".equals(rol);
    }

    private boolean esTokenValido(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }

        String token = authorization.substring(7);
        return authUserService.validate(token) != null;
    }

    private AuthUserResponseDto toResponse(AuthUser authUser) {
        boolean usuarioLegacy = authUser.getRol() == null || authUser.getRol().trim().isEmpty();
        String rol = usuarioLegacy ? "ADMIN" : authUser.getRol().trim().toUpperCase();
        Boolean activo = authUser.getActivo() == null ? true : authUser.getActivo();

        return new AuthUserResponseDto(
                authUser.getId(),
                authUser.getUserName(),
                authUser.getNombre(),
                authUser.getApellido(),
                authUser.getDni(),
                authUser.getCelular(),
                authUser.getCorreo(),
                rol,
                activo
        );
    }
}
