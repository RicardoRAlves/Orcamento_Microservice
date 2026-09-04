package com.br.capoeira.orcamento.budgetcalculator.service;

import com.br.capoeira.orcamento.budgetcalculator.dto.BudgetCalculationRequestMessage;
import com.br.capoeira.orcamento.budgetcalculator.dto.BudgetCalculationResultMessage;
import com.br.capoeira.orcamento.budgetcalculator.exception.BudgetCalculationException;
import com.br.capoeira.orcamento.budgetcalculator.producer.BudgetCalculationResultPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
public class BudgetCalculationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BudgetCalculationService.class);
    private static final String CALCULATION_FINISHED_EVENT = "BUDGET_CALCULATION_FINISHED";
    private static final String EVENT_VERSION = "1.0";
    private static final BigDecimal DISCOUNT_THRESHOLD = BigDecimal.valueOf(1000);
    private static final BigDecimal DISCOUNT_RATE = BigDecimal.valueOf(0.10);

    private final BudgetCalculationResultPublisher budgetCalculationResultPublisher;

    public BudgetCalculationService(BudgetCalculationResultPublisher budgetCalculationResultPublisher) {
        this.budgetCalculationResultPublisher = budgetCalculationResultPublisher;
    }

    public void calculate(BudgetCalculationRequestMessage message) {
        validate(message);

        var calculatedAmount = applyBusinessRules(message.amount());
        var result = new BudgetCalculationResultMessage(
                CALCULATION_FINISHED_EVENT,
                EVENT_VERSION,
                message.correlationId(),
                message.budgetId(),
                message.amount(),
                calculatedAmount,
                "CALCULATED",
                null,
                Instant.now()
        );

        budgetCalculationResultPublisher.publish(result);

        LOGGER.info(
                "Budget calculated. budgetId={}, correlationId={}, originalAmount={}, calculatedAmount={}",
                message.budgetId(),
                message.correlationId(),
                message.amount(),
                calculatedAmount
        );
    }

    private void validate(BudgetCalculationRequestMessage message) {
        if (message == null || message.budgetId() == null) {
            throw new BudgetCalculationException("Budget calculation request must contain budgetId");
        }

        if (message.amount() == null || message.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BudgetCalculationException("Budget calculation request must contain amount greater than zero");
        }
    }

    private BigDecimal applyBusinessRules(BigDecimal amount) {
        if (amount.compareTo(DISCOUNT_THRESHOLD) <= 0) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }

        var discount = amount.multiply(DISCOUNT_RATE);
        return amount.subtract(discount).setScale(2, RoundingMode.HALF_UP);
    }
}
