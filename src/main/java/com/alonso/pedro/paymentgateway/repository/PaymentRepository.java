package com.alonso.pedro.paymentgateway.repository;

import com.alonso.pedro.paymentgateway.model.PaymentDTO;
import com.alonso.pedro.paymentgateway.model.ResultsDTO;

import java.time.Instant;

public interface PaymentRepository {
    void save(PaymentDTO paymentDTO);

    ResultsDTO getSummary(Instant from, Instant to);
}
