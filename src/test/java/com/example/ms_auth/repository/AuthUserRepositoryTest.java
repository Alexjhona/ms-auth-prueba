package com.example.ms_auth.repository;

import com.example.ms_auth.entity.AuthUser;
import com.example.ms_auth.support.MySqlTestContainerSupport;
import com.example.ms_auth.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthUserRepositoryTest extends MySqlTestContainerSupport {

    @Autowired
    private AuthUserRepository authUserRepository;

    @Test
    @DisplayName("findByUserName retorna usuario existente")
    void findByUserName_WhenUserExists_ReturnsUser() {
        AuthUser saved = authUserRepository.saveAndFlush(TestDataFactory.user("ana", "secret"));

        assertThat(authUserRepository.findByUserName("ana"))
                .isPresent()
                .get()
                .extracting(AuthUser::getId, AuthUser::getUserName, AuthUser::getRol, AuthUser::getActivo)
                .containsExactly(saved.getId(), "ana", "VENDEDOR", true);
    }

    @Test
    @DisplayName("findByCorreoIgnoreCase busca correo sin importar mayusculas")
    void findByCorreoIgnoreCase_WhenEmailCaseDiffers_ReturnsUser() {
        authUserRepository.saveAndFlush(TestDataFactory.user("ana", "secret"));

        assertThat(authUserRepository.findByCorreoIgnoreCase("ANA@TEST.COM"))
                .isPresent()
                .get()
                .extracting(AuthUser::getUserName)
                .isEqualTo("ana");
    }

    @Test
    @DisplayName("existsByDni y existsByCorreo reflejan datos persistidos")
    void existsQueries_WhenDataExists_ReturnTrue() {
        authUserRepository.saveAndFlush(TestDataFactory.user("ana", "secret"));

        assertThat(authUserRepository.existsByDni("12345678")).isTrue();
        assertThat(authUserRepository.existsByCorreo("ana@test.com")).isTrue();
        assertThat(authUserRepository.existsByDni("00000000")).isFalse();
        assertThat(authUserRepository.existsByCorreo("missing@test.com")).isFalse();
    }

    @Test
    @DisplayName("restricciones unicas evitan userName duplicado")
    void save_WhenUserNameDuplicated_ThrowsDataIntegrityViolation() {
        authUserRepository.saveAndFlush(TestDataFactory.user("ana", "secret"));
        AuthUser duplicated = TestDataFactory.user("ana", "another");
        duplicated.setDni("22222222");
        duplicated.setCorreo("ana2@test.com");

        assertThatThrownBy(() -> authUserRepository.saveAndFlush(duplicated))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
