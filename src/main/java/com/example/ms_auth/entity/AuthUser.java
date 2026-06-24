package com.example.ms_auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String userName;

    @Column(nullable = false)
    private String password;

    private String nombre;

    private String apellido;

    @Column(unique = true)
    private String dni;

    private String celular;

    @Column(unique = true)
    private String correo;

    @Builder.Default
    @Column(nullable = false)
    private String rol = "ADMIN";

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    public AuthUser(Integer id, String userName, String password) {
        this.id = id;
        this.userName = userName;
        this.password = password;
        this.rol = "ADMIN";
        this.activo = true;
    }
}
