package com.alonso.pedro.paymentgateway.repository;

import com.alonso.pedro.paymentgateway.model.PaymentDTO;
import com.alonso.pedro.paymentgateway.model.ResultsDTO;

import java.time.Instant;
import java.util.List;

public interface PaymentRepository {
    void save(List<PaymentDTO> paymentDTOList);

    ResultsDTO getSummary(Instant from, Instant to);
}
