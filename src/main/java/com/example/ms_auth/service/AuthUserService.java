package com.example.ms_auth.service;


import com.example.ms_auth.dto.AuthUserDto;
import com.example.ms_auth.entity.AuthUser;
import com.example.ms_auth.entity.TokenDto;

import java.util.List;

public interface AuthUserService {
    public AuthUser save(AuthUserDto authUserDto);


    public TokenDto login(AuthUserDto authUserDto);


    public TokenDto validate(String token);

    TokenDto impersonarTrabajador(Integer id);

    List<AuthUser> listar();

    AuthUser actualizar(Integer id, AuthUserDto authUserDto);

    AuthUser cambiarEstado(Integer id, boolean activo);

    AuthUser activarTrabajador(String correo, String userName, String password);

    void eliminar(Integer id);
}

