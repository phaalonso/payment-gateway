package com.alonso.pedro.paymentgateway.model;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentDTO(String correlationId, BigDecimal amount, Instant requestedAt) {
    public static PaymentDTO of(PaymentRequestDTO paymentRequestDTO) {
        return new PaymentDTO(paymentRequestDTO.correlationId(), paymentRequestDTO.amount(), Instant.now());
    }
}
