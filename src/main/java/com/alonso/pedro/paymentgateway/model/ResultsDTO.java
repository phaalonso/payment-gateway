package com.alonso.pedro.paymentgateway.model;

import java.math.BigDecimal;

public record ResultsDTO(Integer totalRequests, BigDecimal totalAmount) {
}
