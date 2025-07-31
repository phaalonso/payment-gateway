package com.alonso.pedro.paymentgateway.controller;

import com.alonso.pedro.paymentgateway.model.PaymentDTO;
import com.alonso.pedro.paymentgateway.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
public class PaymentsController {
    private final PaymentService paymentService;

    public PaymentsController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payments")
    public ResponseEntity receivePayment(@RequestBody PaymentDTO paymentDTO) {
        paymentService.sendPayment(paymentDTO);

        return ResponseEntity.ok()
                .build();
    }

    @GetMapping("/payments-summary")
    public String getSummary(@RequestParam LocalDateTime from, @RequestParam LocalDateTime to) {
        return """
                {
                    "default" : {
                        "totalRequests": %s,
                        "totalAmount": %s
                    },
                    "fallback" : {
                        "totalRequests": %s,
                        "totalAmount": %s
                    }
                }
                """.formatted(0, 0.0, 0, 0.0);
    }
}
