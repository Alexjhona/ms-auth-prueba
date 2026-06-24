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
public class ActivarTrabajadorRequest {
    @NotBlank(message = "Correo obligatorio")
    @Email(message = "Correo invalido")
    private String correo;

    @NotBlank(message = "Usuario obligatorio")
    private String userName;

    @NotBlank(message = "Contrasena obligatoria")
    private String password;
}
