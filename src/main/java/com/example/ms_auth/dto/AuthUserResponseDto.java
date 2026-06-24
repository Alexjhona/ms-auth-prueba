package com.example.ms_auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserResponseDto {
    private int id;
    private String userName;
    private String nombre;
    private String apellido;
    private String dni;
    private String celular;
    private String correo;
    private String rol;
    private Boolean activo;

    public AuthUserResponseDto(int id, String userName) {
        this.id = id;
        this.userName = userName;
    }
}
