package com.alonso.pedro.paymentgateway.controller;

import com.alonso.pedro.paymentgateway.model.PaymentRequestDTO;
import com.alonso.pedro.paymentgateway.model.ResultsDTO;
import com.alonso.pedro.paymentgateway.model.SummaryDTO;
import com.alonso.pedro.paymentgateway.repository.PaymentRepository;
import com.alonso.pedro.paymentgateway.repository.PostgresPaymentRepository;
import com.alonso.pedro.paymentgateway.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;

@RestController
public class PaymentsController {
    private final Logger logger = LoggerFactory.getLogger(PaymentsController.class);

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    public PaymentsController(PaymentService paymentService, PostgresPaymentRepository paymentRepository) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
    }

    @PostMapping("/payments")
    public ResponseEntity receivePayment(@RequestBody PaymentRequestDTO paymentDTO) {
        paymentService.sendPayment(paymentDTO);

        return ResponseEntity.ok()
                .build();
    }

    @GetMapping("/payments-summary")
    public ResponseEntity getSummary(@RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) {
        logger.info("Obtendo o sumário from={} to={}", from, to);
        var summary = paymentRepository.getSummary(from, to);

        var result = new SummaryDTO(summary, new ResultsDTO(0, BigDecimal.ZERO));

//        var result = """
//                {
//                    "default" : {
//                        "totalRequests": %d,
//                        "totalAmount": %.2f
//                    },
//                    "fallback" : {
//                        "totalRequests": %s,
//                        "totalAmount": %.2f
//                    }
//                }""".formatted(summary.totalRequests(), summary.totalAmount(), 0, 0.0);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }
}
