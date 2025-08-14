package com.alonso.pedro.paymentgateway.service;

import com.alonso.pedro.paymentgateway.model.PaymentDTO;
import com.alonso.pedro.paymentgateway.model.PaymentRequestDTO;
import com.alonso.pedro.paymentgateway.repository.InMemoryPaymentRepository;
import com.alonso.pedro.paymentgateway.repository.PaymentRepository;
import com.alonso.pedro.paymentgateway.repository.PostgresPaymentRepository;
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
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class PaymentService {

    private final RestTemplate restTemplate = new RestTemplate();

    private final static Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final ConcurrentLinkedQueue<PaymentDTO> paymentsQueue = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<PaymentDTO> databaseQueue = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean isPaymentProcessorHealthy = new AtomicBoolean(true);

    //    private final PaymentRepository paymentRepository = InMemoryPaymentRepository.getInstance();
    private final PostgresPaymentRepository paymentRepository;

    @Value("${pagamento.processor.default.url}")
    private String defaultUrl;

    @Value("${MAX_PAYMENTS_THREADS}")
    private Integer maxConcurrentPayments;

    private PaymentDTO firstPayment;

    public PaymentService(PostgresPaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public void sendPayment(PaymentRequestDTO requestDTO) {
        var paymentDTO = PaymentDTO.of(requestDTO);

        paymentsQueue.offer(paymentDTO);
    }

    // fixedDelay runs after the last execution was finished
    // fixedRate runs even if the previous execution is running
    @Scheduled(fixedDelay = 100)
    public void saveInDatabase() {
        if (databaseQueue.isEmpty())
            return;

        var listSize = databaseQueue.size();

        log.info("Storing {} itens on the database", listSize);

        var payments = new ArrayList<PaymentDTO>();

        // insert all elements accumulated in the list to the database
        // uses batch update to do multiple inserts of at most 1000 elements
        for (int j = 0; j < listSize; j++) {
            var element = databaseQueue.poll();

            if (element == null) {
                break;
            }

            payments.add(element);
        }

        paymentRepository.save(payments);
    }

    @Scheduled(initialDelay = 300, fixedRate = 300)
    public void checkHealth() {
        if (isPaymentProcessorHealthy.get() || firstPayment == null) {
            return;
        }

        log.info("Checking if Payment Processor is healthy");

        var result = sendPayment(firstPayment);

        if (result) {
            log.info("Payment is health again");
            isPaymentProcessorHealthy.set(true);
        }

        if (!result) {
            log.info("Payment processor is still not health");
        }
    }

    @Scheduled(initialDelay = 10, fixedDelay = 50)
    public void processJob() {
        if (paymentsQueue.isEmpty() || !isPaymentProcessorHealthy.get()) {
            return;
        }

        var size = paymentsQueue.size();
        var processSize = Math.min(size, maxConcurrentPayments);

        log.info("Processing {} of {} itens", processSize, size);

        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < processSize; i++) {
                executorService.submit(this::processPayment);
            }
        }
    }

    public void processPayment() {
        if (!isPaymentProcessorHealthy.get()) {
            return;
        }

        var payment = paymentsQueue.poll();

        if (payment == null) {
            return;
        }

        var result = sendPayment(payment);

        // stop processing loop if there is an 412 or other error while processing
        if (!result) {
            isPaymentProcessorHealthy.set(false);

            // requeue payment item
            paymentsQueue.offer(payment);

            return;
        }

        if (firstPayment == null) {
            firstPayment = payment;
        }

        databaseQueue.offer(payment);
    }

    public boolean sendPayment(PaymentDTO paymentDTO) {
        var request = """
                {
                  "correlationId": "%s",
                  "amount": %.2f,
                  "requestedAt": "%s"
                }
                """.formatted(paymentDTO.correlationId(), paymentDTO.amount(), paymentDTO.requestedAt());

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        var entity = new HttpEntity<>(request, headers);

        try {
            var response = restTemplate.postForEntity(defaultUrl + "/payments", entity, String.class);

            return response.getStatusCode().value() == HttpStatus.OK.value();
        } catch (HttpClientErrorException.UnprocessableEntity e) {
            return true;
        } catch (HttpServerErrorException.InternalServerError e) {
            log.error("Internal server error");
            return false;
        } catch (Exception e) {
            log.error("Error while integrating with payment processor", e);

            return false;
        }
    }
}
