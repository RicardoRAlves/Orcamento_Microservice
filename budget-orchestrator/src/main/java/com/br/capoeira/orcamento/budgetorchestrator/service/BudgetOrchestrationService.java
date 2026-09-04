package com.br.capoeira.orcamento.budgetorchestrator.service;

import com.br.capoeira.orcamento.budgetorchestrator.dto.BudgetCalculationRequestMessage;
import com.br.capoeira.orcamento.budgetorchestrator.dto.BudgetCalculationResultMessage;
import com.br.capoeira.orcamento.budgetorchestrator.dto.BudgetEventMessage;
import com.br.capoeira.orcamento.budgetorchestrator.dto.BudgetRequestMessage;
import com.br.capoeira.orcamento.budgetorchestrator.dto.BudgetStatus;
import com.br.capoeira.orcamento.budgetorchestrator.exception.BudgetProcessingException;
import com.br.capoeira.orcamento.budgetorchestrator.model.BudgetDocument;
import com.br.capoeira.orcamento.budgetorchestrator.producer.BudgetCalculationRequestPublisher;
import com.br.capoeira.orcamento.budgetorchestrator.producer.BudgetEventPublisher;
import com.br.capoeira.orcamento.budgetorchestrator.repository.BudgetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class BudgetOrchestrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BudgetOrchestrationService.class);
    private static final String CALCULATION_REQUESTED_EVENT = "BUDGET_CALCULATION_REQUESTED";
    private static final String BUDGET_COMPLETED_EVENT = "BUDGET_COMPLETED";
    private static final String EVENT_VERSION = "1.0";

    private final BudgetRepository budgetRepository;
    private final BudgetCalculationRequestPublisher budgetCalculationRequestPublisher;
    private final BudgetEventPublisher budgetEventPublisher;

    public BudgetOrchestrationService(
            BudgetRepository budgetRepository,
            BudgetCalculationRequestPublisher budgetCalculationRequestPublisher,
            BudgetEventPublisher budgetEventPublisher) {
        this.budgetRepository = budgetRepository;
        this.budgetCalculationRequestPublisher = budgetCalculationRequestPublisher;
        this.budgetEventPublisher = budgetEventPublisher;
    }

    public void process(BudgetRequestMessage message) {
        validate(message);

        var budget = budgetRepository.findById(message.budgetId())
                .orElseThrow(() -> new BudgetProcessingException(
                        "Budget not found for processing. budgetId=%s".formatted(message.budgetId())
                ));

        ensureCorrelationId(budget, message.correlationId());

        if (budget.getStatus() == BudgetStatus.CALCULATED) {
            LOGGER.info("Skipping already calculated budget. budgetId={}, correlationId={}", message.budgetId(), message.correlationId());
            return;
        }

        if (budget.getStatus() == BudgetStatus.FAILED) {
            LOGGER.info("Skipping budget already marked as failed. budgetId={}, correlationId={}", message.budgetId(), message.correlationId());
            return;
        }

        if (budget.getStatus() == BudgetStatus.CALCULATION_REQUESTED) {
            LOGGER.info("Skipping budget already sent to calculator. budgetId={}, correlationId={}", message.budgetId(), message.correlationId());
            return;
        }

        markAsProcessing(budget);

        budgetCalculationRequestPublisher.publish(new BudgetCalculationRequestMessage(
                CALCULATION_REQUESTED_EVENT,
                EVENT_VERSION,
                budget.getCorrelationId(),
                budget.getId(),
                budget.getCustomerName(),
                budget.getDescription(),
                budget.getAmount(),
                Instant.now()
        ));

        markAsCalculationRequested(budget);

        LOGGER.info(
                "Budget request orchestrated. budgetId={}, correlationId={}, customerName={}, amount={}, status={}",
                message.budgetId(),
                budget.getCorrelationId(),
                message.customerName(),
                message.amount(),
                BudgetStatus.CALCULATION_REQUESTED
        );
    }

    public void finishCalculation(BudgetCalculationResultMessage message) {
        validate(message);

        var budget = budgetRepository.findById(message.budgetId())
                .orElseThrow(() -> new BudgetProcessingException(
                        "Budget not found for calculation result. budgetId=%s".formatted(message.budgetId())
                ));

        ensureCorrelationId(budget, message.correlationId());

        if (budget.getStatus() == BudgetStatus.CALCULATED || budget.getStatus() == BudgetStatus.FAILED) {
            publishCompletedEventIfNeeded(budget, message.errorMessage());
            return;
        }

        if ("CALCULATED".equalsIgnoreCase(message.status())) {
            budget.setCalculatedAmount(message.calculatedAmount());
            budget.setStatus(BudgetStatus.CALCULATED);
        } else {
            budget.setStatus(BudgetStatus.FAILED);
        }

        budget.setProcessedAt(message.calculatedAt() == null ? Instant.now() : message.calculatedAt());
        budget.setUpdatedAt(Instant.now());
        budgetRepository.save(budget);

        publishCompletedEventIfNeeded(budget, message.errorMessage());

        LOGGER.info(
                "Budget calculation result applied. budgetId={}, correlationId={}, status={}, calculatedAmount={}",
                message.budgetId(),
                message.correlationId(),
                budget.getStatus(),
                budget.getCalculatedAmount()
        );
    }

    private void validate(BudgetRequestMessage message) {
        if (message == null || message.budgetId() == null) {
            throw new BudgetProcessingException("Budget request message must contain budgetId");
        }
    }

    private void validate(BudgetCalculationResultMessage message) {
        if (message == null || message.budgetId() == null) {
            throw new BudgetProcessingException("Budget calculation result message must contain budgetId");
        }

        if ("CALCULATED".equalsIgnoreCase(message.status()) && message.calculatedAmount() == null) {
            throw new BudgetProcessingException("Budget calculation result message must contain calculatedAmount");
        }
    }

    private void markAsProcessing(BudgetDocument budget) {
        budget.setStatus(BudgetStatus.PROCESSING);
        budget.setUpdatedAt(Instant.now());
        budgetRepository.save(budget);
    }

    private void ensureCorrelationId(BudgetDocument budget, UUID messageCorrelationId) {
        if (budget.getCorrelationId() == null) {
            budget.setCorrelationId(messageCorrelationId == null ? UUID.randomUUID() : messageCorrelationId);
        }
    }

    private void markAsCalculationRequested(BudgetDocument budget) {
        budget.setStatus(BudgetStatus.CALCULATION_REQUESTED);
        budget.setUpdatedAt(Instant.now());
        budgetRepository.save(budget);
    }

    private void publishCompletedEventIfNeeded(BudgetDocument budget, String errorMessage) {
        if (budget.getCompletedEventPublishedAt() != null) {
            LOGGER.info("Skipping already published budget event. budgetId={}, status={}", budget.getId(), budget.getStatus());
            return;
        }

        budgetEventPublisher.publish(new BudgetEventMessage(
                BUDGET_COMPLETED_EVENT,
                EVENT_VERSION,
                budget.getCorrelationId(),
                budget.getId(),
                budget.getCustomerName(),
                budget.getDescription(),
                budget.getAmount(),
                budget.getCalculatedAmount(),
                budget.getStatus(),
                errorMessage,
                Instant.now()
        ));

        budget.setCompletedEventPublishedAt(Instant.now());
        budget.setUpdatedAt(Instant.now());
        budgetRepository.save(budget);
    }
}
