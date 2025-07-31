package com.alonso.pedro.paymentgateway.model;

import java.math.BigDecimal;

public record PaymentDTO(String correlationId, BigDecimal amount) {
}
