package com.example.ms_auth.service;

import com.example.ms_auth.dto.EnviarInvitacionRequest;

public interface EmailService {
    void enviarInvitacionTrabajador(EnviarInvitacionRequest request);
}
