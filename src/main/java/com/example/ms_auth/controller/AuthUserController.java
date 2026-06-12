package com.example.ms_auth.controller;

import com.example.ms_auth.dto.AuthUserDto;
import com.example.ms_auth.dto.AuthUserResponseDto;
import com.example.ms_auth.entity.AuthUser;
import com.example.ms_auth.entity.TokenDto;
import com.example.ms_auth.service.AuthUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticacion", description = "Endpoints para iniciar sesion, crear usuarios y validar tokens JWT.")
public class AuthUserController {

    private final AuthUserService authUserService;

    public AuthUserController(AuthUserService authUserService) {
        this.authUserService = authUserService;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion y obtener token JWT de acceso", description = "Valida el userName y password proporcionados, y devuelve un token JWT que permite consumir de forma autorizada las rutas protegidas del sistema.")
    @ApiResponse(responseCode = "200", description = "Login correcto, devuelve token JWT")
    @ApiResponse(responseCode = "400", description = "Credenciales invalidas")
    public ResponseEntity<TokenDto> login(@Valid @RequestBody AuthUserDto authUserDto) {
        TokenDto tokenDto = authUserService.login(authUserDto);
        if (tokenDto == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(tokenDto);
    }

    @PostMapping("/validate")
    @Operation(summary = "Validar token JWT para autorizar solicitudes protegidas", description = "Recibe un token JWT, comprueba su validez y devuelve el mismo token cuando puede utilizarse para autorizar solicitudes dirigidas a rutas protegidas.")
    @ApiResponse(responseCode = "200", description = "Token valido")
    @ApiResponse(responseCode = "400", description = "Token invalido o expirado")
    public ResponseEntity<TokenDto> validate(@Parameter(description = "Token JWT temporal que se validara para autorizar solicitudes protegidas. No compartir ni registrar tokens reales.", example = "JWT_TEMPORAL") @RequestParam String token) {
        TokenDto tokenDto = authUserService.validate(token);
        if (tokenDto == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(tokenDto);
    }

    @PostMapping("/create")
    @Operation(summary = "Registrar nuevo usuario con credenciales de acceso", description = "Registra un usuario con userName y password cifrado, y devuelve su identificador y nombre para confirmar la creacion sin exponer la contrasena almacenada.")
    @ApiResponse(responseCode = "200", description = "Usuario creado correctamente")
    @ApiResponse(responseCode = "400", description = "Datos de usuario invalidos")
    public ResponseEntity<AuthUserResponseDto> create(@Valid @RequestBody AuthUserDto authUserDto) {
        AuthUser authUser = authUserService.save(authUserDto);
        if (authUser == null) {
            return ResponseEntity.badRequest().build();
        }

        AuthUserResponseDto response = new AuthUserResponseDto(
                authUser.getId(),
                authUser.getUserName()
        );

        return ResponseEntity.ok(response);
    }
}
