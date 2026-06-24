package com.example.ms_auth.dto;

import com.example.ms_auth.support.TestDataFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DtoValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("AuthUserDto válido no genera violaciones")
    void authUserDto_WhenValid_HasNoViolations() {
        assertThat(validator.validate(TestDataFactory.validUserDto("ana", "123456"))).isEmpty();
    }

    @Test
    @DisplayName("AuthUserDto inválido valida userName y password")
    void authUserDto_WhenInvalid_HasViolations() {
        AuthUserDto dto = AuthUserDto.builder()
                .userName(" ")
                .password(".")
                .build();

        Set<ConstraintViolation<AuthUserDto>> violations = validator.validate(dto);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("userName", "password");
    }

    @Test
    @DisplayName("ActivarTrabajadorRequest valida correo, usuario y password")
    void activarTrabajadorRequest_WhenInvalid_HasViolations() {
        ActivarTrabajadorRequest request = ActivarTrabajadorRequest.builder()
                .correo("correo-invalido")
                .userName("")
                .password(" ")
                .build();

        Set<ConstraintViolation<ActivarTrabajadorRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("correo", "userName", "password");
    }

    @Test
    @DisplayName("EnviarInvitacionRequest válido no genera violaciones")
    void enviarInvitacionRequest_WhenValid_HasNoViolations() {
        assertThat(validator.validate(TestDataFactory.invitacionRequest())).isEmpty();
    }

    @Test
    @DisplayName("EnviarInvitacionRequest inválido valida emails y obligatorios")
    void enviarInvitacionRequest_WhenInvalid_HasViolations() {
        EnviarInvitacionRequest request = EnviarInvitacionRequest.builder()
                .para("bad")
                .remitente("bad")
                .nombre("")
                .rol("")
                .enlaceRegistro("")
                .build();

        Set<ConstraintViolation<EnviarInvitacionRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("para", "remitente", "nombre", "rol", "enlaceRegistro");
    }
}
