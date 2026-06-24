package com.example.ms_auth.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserDto {
    @NotBlank(message = "Campo obligatorio")
    @Pattern(regexp = "^(?!\\s*\\.\\s*$).*$", message = "Valor inválido")
    private String userName;

    @NotBlank(message = "Campo obligatorio")
    @Pattern(regexp = "^(?!\\s*\\.\\s*$).*$", message = "Valor inválido")
    private String password;

    private String nombre;
    private String apellido;
    private String dni;
    private String celular;
    private String correo;
    private String rol;
    private Boolean activo;

    public AuthUserDto(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }
}
