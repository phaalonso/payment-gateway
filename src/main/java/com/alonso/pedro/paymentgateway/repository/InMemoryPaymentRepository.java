package com.alonso.pedro.paymentgateway.repository;

import com.alonso.pedro.paymentgateway.model.PaymentDTO;
import com.alonso.pedro.paymentgateway.model.ResultsDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class InMemoryPaymentRepository implements PaymentRepository {
    private final static InMemoryPaymentRepository INSTANCE = new InMemoryPaymentRepository();

    private final ConcurrentLinkedQueue<PaymentDTO> paymentDTOs = new ConcurrentLinkedQueue<>();

    public static InMemoryPaymentRepository getInstance() {
        return INSTANCE;
    }

    @Override
    public void save(PaymentDTO paymentDTO) {
        paymentDTOs.add(paymentDTO);
    }

    @Override
    public ResultsDTO getSummary(Instant from, Instant to) {
        var resultadoAtual = new ArrayList<PaymentDTO>();

        if (resultadoAtual.isEmpty()) {
            return new ResultsDTO(0, BigDecimal.ZERO);
        }

        var results = resultadoAtual.stream()
                .filter(payment -> !payment.requestedAt().isBefore(from) && !payment.requestedAt().isAfter(to))
                .collect(Collectors.toList());

        var total = results.getFirst().amount().multiply(BigDecimal.valueOf(results.size()));

        return new ResultsDTO(results.size(), total);
    }
}
