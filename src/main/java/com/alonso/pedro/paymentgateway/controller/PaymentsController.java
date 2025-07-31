package com.alonso.pedro.paymentgateway.controller;

import com.alonso.pedro.paymentgateway.model.PaymentRequestDTO;
import com.alonso.pedro.paymentgateway.repository.InMemoryPaymentRepository;
import com.alonso.pedro.paymentgateway.repository.PaymentRepository;
import com.alonso.pedro.paymentgateway.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
public class PaymentsController {
    private final PaymentService paymentService;

    private final PaymentRepository paymentRepository = InMemoryPaymentRepository.getInstance();

    public PaymentsController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payments")
    public ResponseEntity receivePayment(@RequestBody PaymentRequestDTO paymentDTO) {
        paymentService.sendPayment(paymentDTO);

        return ResponseEntity.ok()
                .build();
    }

    @GetMapping("/payments-summary")
    public String getSummary(@RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) {
        var summary = paymentRepository.getSummary(from, to);

        return """
                {
                    "default" : {
                        "totalRequests": %d,
                        "totalAmount": %.2f
                    },
                    "fallback" : {
                        "totalRequests": %s,
                        "totalAmount": %s
                    }
                }
                """.formatted(summary.totalRequests(), summary.totalAmount(), 0, 0.0);
    }
}
