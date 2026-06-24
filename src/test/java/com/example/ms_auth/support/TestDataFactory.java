package com.example.ms_auth.support;

import com.example.ms_auth.dto.ActivarTrabajadorRequest;
import com.example.ms_auth.dto.AuthUserDto;
import com.example.ms_auth.dto.EnviarInvitacionRequest;
import com.example.ms_auth.entity.AuthUser;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static AuthUserDto validUserDto(String userName, String password) {
        return AuthUserDto.builder()
                .userName(userName)
                .password(password)
                .nombre("Ana")
                .apellido("Lopez")
                .dni("12345678")
                .celular("999888777")
                .correo(userName + "@test.com")
                .rol("VENDEDOR")
                .activo(true)
                .build();
    }

    public static AuthUser user(String userName, String password) {
        return AuthUser.builder()
                .userName(userName)
                .password(password)
                .nombre("Ana")
                .apellido("Lopez")
                .dni("12345678")
                .celular("999888777")
                .correo(userName + "@test.com")
                .rol("VENDEDOR")
                .activo(true)
                .build();
    }

    public static AuthUser admin(String userName, String password) {
        AuthUser admin = user(userName, password);
        admin.setRol("ADMIN");
        admin.setCorreo(userName + "@admin.test");
        admin.setDni("87654321");
        return admin;
    }

    public static ActivarTrabajadorRequest activarTrabajadorRequest() {
        return ActivarTrabajadorRequest.builder()
                .correo("trabajador@test.com")
                .userName("trabajador")
                .password("123456")
                .build();
    }

    public static EnviarInvitacionRequest invitacionRequest() {
        return EnviarInvitacionRequest.builder()
                .para("trabajador@test.com")
                .remitente("admin@test.com")
                .nombre("Trabajador")
                .rol("VENDEDOR")
                .enlaceRegistro("https://app.test/activar")
                .asunto("Invitacion")
                .build();
    }
}
