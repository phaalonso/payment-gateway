package com.alonso.pedro.paymentgateway.model;

import java.math.BigDecimal;

public record PaymentRequestDTO(String correlationId, BigDecimal amount) {
}
