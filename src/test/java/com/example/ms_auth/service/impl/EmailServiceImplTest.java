package com.example.ms_auth.service.impl;

import com.example.ms_auth.dto.EnviarInvitacionRequest;
import com.example.ms_auth.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceImplTest {

    @Test
    void sendsInvitationWithDefaultSubjectAndExpectedContent() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailServiceImpl service = service(sender);
        EnviarInvitacionRequest request = TestDataFactory.invitacionRequest();
        request.setAsunto(" ");
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        service.enviarInvitacionTrabajador(request);

        verify(sender).send(captor.capture());
        assertThat(captor.getValue().getSubject())
                .isEqualTo("Invitacion de registro - Chinito Importaciones");
        assertThat(captor.getValue().getText())
                .contains(request.getNombre().trim(), request.getRol().trim().toUpperCase(),
                        request.getEnlaceRegistro().trim());
    }

    @Test
    void sendsPasswordResetContentForCustomSubject() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailServiceImpl service = service(sender);
        EnviarInvitacionRequest request = TestDataFactory.invitacionRequest();
        request.setAsunto(" Restablecer contrasena ");
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        service.enviarInvitacionTrabajador(request);

        verify(sender).send(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("Restablecer contrasena");
        assertThat(captor.getValue().getText()).contains("restablecer tu contrasena");
    }

    @Test
    void failsClearlyWhenMailSenderIsUnavailable() {
        EmailServiceImpl service = service(null);
        EnviarInvitacionRequest request = TestDataFactory.invitacionRequest();

        assertThatThrownBy(() -> service.enviarInvitacionTrabajador(request))
                .isInstanceOf(MailSendException.class)
                .hasMessageContaining("JavaMailSender");
    }

    @SuppressWarnings("unchecked")
    private EmailServiceImpl service(JavaMailSender sender) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        EmailServiceImpl service = new EmailServiceImpl(provider);
        ReflectionTestUtils.setField(service, "remitenteConfigurado", "sistema@test.com");
        return service;
    }
}
