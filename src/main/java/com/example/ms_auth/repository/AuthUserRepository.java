package com.example.ms_auth.repository;


import com.example.ms_auth.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface AuthUserRepository extends JpaRepository<AuthUser,Integer> {
    AuthUser findByuserName(String username);

    Optional<AuthUser> findByUserName(String userName);

    Optional<AuthUser> findByCorreoIgnoreCase(String correo);

    boolean existsByDni(String dni);

    boolean existsByCorreo(String correo);
}
