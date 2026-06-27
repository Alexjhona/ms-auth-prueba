package com.example.ms_auth.service.impl;


import com.example.ms_auth.dto.AuthUserDto;
import com.example.ms_auth.entity.AuthUser;
import com.example.ms_auth.entity.TokenDto;
import com.example.ms_auth.exception.ConflictoRecursoException;
import com.example.ms_auth.exception.RecursoNoEncontradoException;
import com.example.ms_auth.repository.AuthUserRepository;
import com.example.ms_auth.security.JwtProvider;
import com.example.ms_auth.service.AuthUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;




import java.util.List;
import java.util.Optional;




@Service
public class AuthUserServiceImpl implements AuthUserService {
    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthUserServiceImpl(AuthUserRepository authUserRepository,
                               PasswordEncoder passwordEncoder,
                               JwtProvider jwtProvider) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }


    @Override
    public AuthUser save(AuthUserDto authUserDto) {
        Optional<AuthUser> user = authUserRepository.findByUserName(authUserDto.getUserName());
        if (user.isPresent())
            throw new ConflictoRecursoException();
        validarUnicos(authUserDto, null);
        String password = passwordEncoder.encode(authUserDto.getPassword());
        AuthUser authUser = AuthUser.builder()
                .userName(authUserDto.getUserName())
                .password(password)
                .nombre(limpiar(authUserDto.getNombre()))
                .apellido(limpiar(authUserDto.getApellido()))
                .dni(limpiar(authUserDto.getDni()))
                .celular(limpiar(authUserDto.getCelular()))
                .correo(limpiar(authUserDto.getCorreo()))
                .rol(normalizarRol(authUserDto.getRol()))
                .activo(authUserDto.getActivo() == null || authUserDto.getActivo())
                .build();




        return authUserRepository.save(authUser);
    }




    @Override
    public TokenDto login(AuthUserDto authUserDto) {
        String credencial = limpiar(authUserDto.getUserName());
        if (credencial == null || credencial.isEmpty()) {
            credencial = limpiar(authUserDto.getCorreo());
        }
        if (credencial == null || credencial.isEmpty()) {
            return null;
        }

        Optional<AuthUser> user = authUserRepository.findByUserName(credencial);
        if (user.isEmpty()) {
            user = authUserRepository.findByCorreoIgnoreCase(credencial);
        }
        if (user.isEmpty())
            return null;
        AuthUser authUser = user.get();
        if (Boolean.FALSE.equals(authUser.getActivo()) && tieneRolConfigurado(authUser))
            return null;
        if (passwordEncoder.matches(authUserDto.getPassword(), authUser.getPassword()))
            return new TokenDto(jwtProvider.createToken(authUser));
        return null;
    }




    @Override
    public TokenDto validate(String token) {
        if (!jwtProvider.validate(token))
            return null;
        String username = jwtProvider.getUserNameFromToken(token);
        if (authUserRepository.findByUserName(username).isEmpty())
            return null;
        return new TokenDto(token);
    }

    @Override
    public TokenDto impersonarTrabajador(Integer id) {
        AuthUser trabajador = authUserRepository.findById(id).orElse(null);
        if (trabajador == null || Boolean.FALSE.equals(trabajador.getActivo())) {
            throw new RecursoNoEncontradoException();
        }

        return new TokenDto(jwtProvider.createToken(trabajador));
    }

    @Override
    public List<AuthUser> listar() {
        return authUserRepository.findAll();
    }

    @Override
    public AuthUser actualizar(Integer id, AuthUserDto authUserDto) {
        AuthUser existente = authUserRepository.findById(id).orElse(null);
        if (existente == null)
            throw new RecursoNoEncontradoException();

        Optional<AuthUser> usuarioConMismoNombre = authUserRepository.findByUserName(authUserDto.getUserName());
        if (usuarioConMismoNombre.isPresent() && !usuarioConMismoNombre.get().getId().equals(id))
            throw new ConflictoRecursoException();

        validarUnicos(authUserDto, id);

        existente.setUserName(authUserDto.getUserName());
        if (authUserDto.getPassword() != null && !authUserDto.getPassword().trim().isEmpty()) {
            existente.setPassword(passwordEncoder.encode(authUserDto.getPassword()));
        }
        existente.setNombre(limpiar(authUserDto.getNombre()));
        existente.setApellido(limpiar(authUserDto.getApellido()));
        existente.setDni(limpiar(authUserDto.getDni()));
        existente.setCelular(limpiar(authUserDto.getCelular()));
        existente.setCorreo(limpiar(authUserDto.getCorreo()));
        existente.setRol(normalizarRol(authUserDto.getRol()));
        existente.setActivo(authUserDto.getActivo() == null ? existente.getActivo() : authUserDto.getActivo());

        return authUserRepository.save(existente);
    }

    @Override
    public AuthUser cambiarEstado(Integer id, boolean activo) {
        AuthUser existente = authUserRepository.findById(id).orElse(null);
        if (existente == null)
            throw new RecursoNoEncontradoException();

        existente.setActivo(activo);
        return authUserRepository.save(existente);
    }


    @Override
    public AuthUser activarTrabajador(String correo, String userName, String password) {
        String correoLimpio = limpiar(correo);
        String usuarioLimpio = limpiar(userName);
        String passwordLimpio = limpiar(password);
        if (correoLimpio == null || correoLimpio.isEmpty()
                || usuarioLimpio == null || usuarioLimpio.isEmpty()
                || passwordLimpio == null || passwordLimpio.isEmpty()) {
            throw new IllegalArgumentException("Solicitud inválida");
        }

        AuthUser existente = authUserRepository.findByCorreoIgnoreCase(correoLimpio)
                .orElseGet(() -> authUserRepository.findByUserName(correoLimpio).orElse(null));
        if (existente == null) {
            throw new RecursoNoEncontradoException();
        }

        Optional<AuthUser> usuarioConMismoNombre = authUserRepository.findByUserName(usuarioLimpio);
        if (usuarioConMismoNombre.isPresent() && !usuarioConMismoNombre.get().getId().equals(existente.getId())) {
            throw new ConflictoRecursoException();
        }

        existente.setPassword(passwordEncoder.encode(passwordLimpio));
        existente.setUserName(usuarioLimpio);
        existente.setCorreo(correoLimpio);
        existente.setActivo(true);
        return authUserRepository.save(existente);
    }

    @Override
    public void eliminar(Integer id) {
        if (!authUserRepository.existsById(id)) {
            throw new RecursoNoEncontradoException();
        }
        authUserRepository.deleteById(id);
    }

    private void validarUnicos(AuthUserDto dto, Integer idActual) {
        String dni = limpiar(dto.getDni());
        if (dni != null && !dni.isEmpty()) {
            Optional<AuthUser> existente = authUserRepository.findAll().stream()
                    .filter(u -> dni.equals(u.getDni()))
                    .findFirst();
            if (existente.isPresent() && !existente.get().getId().equals(idActual)) {
                throw new ConflictoRecursoException();
            }
        }

        String correo = limpiar(dto.getCorreo());
        if (correo != null && !correo.isEmpty()) {
            Optional<AuthUser> existente = authUserRepository.findAll().stream()
                    .filter(u -> correo.equalsIgnoreCase(u.getCorreo()))
                    .findFirst();
            if (existente.isPresent() && !existente.get().getId().equals(idActual)) {
                throw new ConflictoRecursoException();
            }
        }
    }

    private String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }

    private String normalizarRol(String rol) {
        String rolNormalizado = rol == null || rol.trim().isEmpty()
                ? "VENDEDOR"
                : rol.trim().toUpperCase().replaceAll("[\\s-]+", "_");

        if ("SUBADMIN".equals(rolNormalizado)) {
            rolNormalizado = "SUB_ADMIN";
        }

        switch (rolNormalizado) {
            case "ADMIN", "SUB_ADMIN", "VENDEDOR", "ALMACENERO", "COMPRAS", "CAJERO":
                return rolNormalizado;
            default:
                throw new IllegalArgumentException("Rol no permitido");
        }
    }

    private boolean tieneRolConfigurado(AuthUser authUser) {
        return authUser.getRol() != null && !authUser.getRol().trim().isEmpty();
    }
}
