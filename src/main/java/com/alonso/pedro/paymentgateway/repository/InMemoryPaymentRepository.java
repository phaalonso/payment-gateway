package com.alonso.pedro.paymentgateway.repository;

import com.alonso.pedro.paymentgateway.model.PaymentDTO;
import com.alonso.pedro.paymentgateway.model.ResultsDTO;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    public ResultsDTO getSummary(@Nullable Instant from, @Nullable Instant to) {
        List<PaymentDTO> results = new ArrayList<>(paymentDTOs);

        if (from != null && to != null) {
            results = results.stream()
                    .filter(payment -> !payment.requestedAt().isBefore(from) && !payment.requestedAt().isAfter(to))
                    .toList();
        }

        if (results.isEmpty()) {
            return new ResultsDTO(0, BigDecimal.ZERO);
        }

        var total = results.getFirst().amount().multiply(BigDecimal.valueOf(results.size()));

        return new ResultsDTO(results.size(), total);
    }
}
