package com.alonso.pedro.paymentgateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SummaryDTO(
        @JsonProperty("default") ResultsDTO defaultB,
        ResultsDTO fallback
) {
}
