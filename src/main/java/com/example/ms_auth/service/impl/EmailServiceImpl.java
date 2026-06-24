package com.example.ms_auth.service.impl;

import com.example.ms_auth.dto.EnviarInvitacionRequest;
import com.example.ms_auth.service.EmailService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:guerrycastillo9@gmail.com}")
    private String remitenteConfigurado;

    public EmailServiceImpl(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    @Override
    public void enviarInvitacionTrabajador(EnviarInvitacionRequest request) {
        if (mailSender == null) {
            throw new MailSendException("No hay JavaMailSender configurado para enviar correos");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(remitenteConfigurado);
        message.setReplyTo(request.getRemitente());
        message.setTo(request.getPara());
        message.setSubject(obtenerAsunto(request));
        message.setText(crearContenido(request));
        mailSender.send(message);
    }

    private String obtenerAsunto(EnviarInvitacionRequest request) {
        if (request.getAsunto() != null && !request.getAsunto().trim().isEmpty()) {
            return request.getAsunto().trim();
        }
        return "Invitacion de registro - Chinito Importaciones";
    }

    private String crearContenido(EnviarInvitacionRequest request) {
        String asunto = obtenerAsunto(request).toLowerCase();
        if (asunto.contains("restablecer")) {
            return "Hola " + request.getNombre().trim() + ",\n\n"
                    + "Se solicito restablecer tu contrasena en Chinito Importaciones.\n\n"
                    + "Para crear una nueva contrasena, abre este enlace local desde la computadora donde se ejecuta el sistema:\n"
                    + request.getEnlaceRegistro().trim() + "\n\n"
                    + "Luego podras iniciar sesion con tu usuario o correo y tu nueva contrasena.\n\n"
                    + "Remitente: " + request.getRemitente().trim();
        }

        return "Hola " + request.getNombre().trim() + ",\n\n"
                + "Se creo tu acceso al sistema de Chinito Importaciones con el rol "
                + request.getRol().trim().toUpperCase() + ".\n\n"
                + "Para registrar tu contrasena, abre este enlace local desde la computadora donde se ejecuta el sistema:\n"
                + request.getEnlaceRegistro().trim() + "\n\n"
                + "Luego podras iniciar sesion con tu correo y la contrasena que registres.\n\n"
                + "Remitente: " + request.getRemitente().trim();
    }
}
