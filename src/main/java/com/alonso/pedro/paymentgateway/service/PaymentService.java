package com.alonso.pedro.paymentgateway.service;

import com.alonso.pedro.paymentgateway.model.PaymentDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class PaymentService {

    private final RestTemplate restTemplate = new RestTemplate();

    private final static Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final ConcurrentLinkedQueue<PaymentDTO> paymentsQueue = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean isPaymentProcessorHealthy = new AtomicBoolean(true);

    @Value("${pagamento.processor.default.url}")
    private String defaultUrl;

    private PaymentDTO firstPayment;

    public void sendPayment(PaymentDTO paymentDTO) {
        paymentsQueue.add(paymentDTO);
    }

    @Scheduled(fixedRate = 500)
    public void checkHealth() {
        if (isPaymentProcessorHealthy.get() || firstPayment == null) {
            return;
        }

        log.info("Checking if Payment Processor is healthy");

        var result = processPayment(firstPayment);

        if (result) {
            log.info("Payment is health again");
            isPaymentProcessorHealthy.set(true);
        }

        if (!result) {
            log.info("Payment processor is still not health");
        }
    }

    @Scheduled(initialDelay = 10, fixedDelay = 200)
    public void processJob() {
        if (paymentsQueue.isEmpty() || !isPaymentProcessorHealthy.get()) {
            return;
        }

        PaymentDTO paymentDTO;

        while ((paymentDTO = paymentsQueue.poll()) != null) {
            log.info("Queue size: {}", paymentsQueue.size() + 1);

            var result = processPayment(paymentDTO);

            // stop processing loop if there is an 412 or other error while processing
            if (!result) {
                isPaymentProcessorHealthy.set(false);

                return;
            }

            if (firstPayment == null) {
                firstPayment = paymentDTO;
            }
        }
    }

    public boolean processPayment(PaymentDTO paymentDTO) {
        var request = """
                {
                  "correlationId": "%s",
                  "amount": %f,
                  "requestedAt": "%s"
                }
                """.formatted(paymentDTO.correlationId(), paymentDTO.amount(), Instant.now());

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        var entity = new HttpEntity<>(request, headers);

        try {
            var response = restTemplate.postForEntity(defaultUrl + "/payments", entity, String.class);

            return response.getStatusCode().value() == HttpStatus.OK.value();
        } catch (HttpClientErrorException.UnprocessableEntity e) {
            return true;
        } catch (Exception e) {
            log.error("Error while integrating with payment processor", e);

            return false;
        }
    }
}
