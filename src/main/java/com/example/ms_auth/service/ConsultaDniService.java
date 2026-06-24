package com.example.ms_auth.service;

import com.example.ms_auth.dto.DniConsultaResponseDto;

public interface ConsultaDniService {
    DniConsultaResponseDto consultar(String dni);
}
