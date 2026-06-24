package com.example.ms_auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DniConsultaResponseDto {
    private String dni;
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String nombreCompleto;
    private String direccion;
    private String distrito;
    private String provincia;
    private String departamento;
    private String ciudad;
    private String fechaNacimiento;
    private String sexo;
}
