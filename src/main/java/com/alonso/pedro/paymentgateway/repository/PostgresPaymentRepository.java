package com.alonso.pedro.paymentgateway.repository;


import com.alonso.pedro.paymentgateway.model.PaymentDTO;
import com.alonso.pedro.paymentgateway.model.ResultsDTO;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;

@Repository
public class PostgresPaymentRepository implements PaymentRepository {
    private static final Logger log = LoggerFactory.getLogger(PostgresPaymentRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public PostgresPaymentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String QUERY_INSERT = "INSERT INTO payment(correlationId, amount, requestedAt) VALUES (?, ?, ?)";

    @Override
    public void save(List<PaymentDTO> paymentDTOList) {
        jdbcTemplate.batchUpdate(
                QUERY_INSERT,
                paymentDTOList,
                1000,
                (ps, paymentDTO) -> {
                    ps.setObject(1, paymentDTO.correlationId(), Types.OTHER);
                    ps.setBigDecimal(2, paymentDTO.amount());
                    ps.setTimestamp(3, Timestamp.from(paymentDTO.requestedAt()));
                });
    }

    private static final String QUERY_ALL_SUMMARY = "SELECT count(*), sum(amount) FROM payment";

    private static final String QUERY_SUMMARY = QUERY_ALL_SUMMARY + " WHERE requestedAt > ? AND requestedAt < ?";

    @Override
    public ResultsDTO getSummary(Instant from, Instant to) {
        // TODO tratar caso so um dos campos for enviado
        if (from == null && to == null) {
            return jdbcTemplate.queryForObject(
                    QUERY_ALL_SUMMARY,
                    (rs, i) -> new ResultsDTO(
                            rs.getInt(1),
                            rs.getBigDecimal(2)));
        }

        return jdbcTemplate.queryForObject(
                QUERY_SUMMARY,
                (rs, i) -> new ResultsDTO(
                        rs.getInt(1),
                        rs.getBigDecimal(2)),
                Timestamp.from(from),
                Timestamp.from(to)
        );
    }
}
