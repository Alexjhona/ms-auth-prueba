package com.example.ms_auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnviarInvitacionRequest {
    @NotBlank(message = "Correo destino obligatorio")
    @Email(message = "Correo destino invalido")
    private String para;

    @NotBlank(message = "Remitente obligatorio")
    @Email(message = "Remitente invalido")
    private String remitente;

    @NotBlank(message = "Nombre obligatorio")
    private String nombre;

    @NotBlank(message = "Rol obligatorio")
    private String rol;

    @NotBlank(message = "Enlace de registro obligatorio")
    private String enlaceRegistro;

    private String asunto;
}
