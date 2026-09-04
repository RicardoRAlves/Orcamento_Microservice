package com.br.capoeira.orcamento.budgetapi.service;

import com.br.capoeira.orcamento.budgetapi.dto.BudgetResponse;
import com.br.capoeira.orcamento.budgetapi.dto.CreateBudgetRequest;
import com.br.capoeira.orcamento.budgetapi.exception.BudgetMessagePublishException;
import com.br.capoeira.orcamento.budgetapi.message.BudgetRequestMessage;
import com.br.capoeira.orcamento.budgetapi.model.BudgetDocument;
import com.br.capoeira.orcamento.budgetapi.model.BudgetStatus;
import com.br.capoeira.orcamento.budgetapi.producer.BudgetRequestPublisher;
import com.br.capoeira.orcamento.budgetapi.repository.BudgetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class BudgetService {

    private static final String BUDGET_REQUESTED_EVENT = "BUDGET_REQUESTED";
    private static final String EVENT_VERSION = "1.0";

    private final BudgetRepository budgetRepository;
    private final BudgetRequestPublisher budgetRequestPublisher;

    public BudgetService(
            BudgetRepository budgetRepository,
            BudgetRequestPublisher budgetRequestPublisher) {
        this.budgetRepository = budgetRepository;
        this.budgetRequestPublisher = budgetRequestPublisher;
    }

    public BudgetResponse create(CreateBudgetRequest request) {
        validateAmount(request);

        var now = Instant.now();

        var budget = new BudgetDocument();
        budget.setId(UUID.randomUUID());
        budget.setCorrelationId(UUID.randomUUID());
        budget.setCustomerName(request.customerName());
        budget.setDescription(request.description());
        budget.setAmount(request.amount());
        budget.setStatus(BudgetStatus.RECEIVED);
        budget.setCreatedAt(now);
        budget.setUpdatedAt(now);

        var savedBudget = budgetRepository.save(budget);

        try {
            budgetRequestPublisher.publish(toMessage(savedBudget));
        } catch (BudgetMessagePublishException exception) {
            savedBudget.setStatus(BudgetStatus.PUBLISH_FAILED);
            savedBudget.setUpdatedAt(Instant.now());
            try {
                budgetRepository.save(savedBudget);
            } catch (RuntimeException statusUpdateException) {
                exception.addSuppressed(statusUpdateException);
            }
            throw exception;
        }

        return toResponse(savedBudget);
    }

    public BudgetResponse findById(UUID id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget id is required");
        }

        return budgetRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
    }

    private void validateAmount(CreateBudgetRequest request) {
        if (request == null || request.amount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget amount is required");
        }

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget amount must be greater than zero");
        }
    }

    private BudgetRequestMessage toMessage(BudgetDocument budget) {
        return new BudgetRequestMessage(
                BUDGET_REQUESTED_EVENT,
                EVENT_VERSION,
                budget.getCorrelationId(),
                budget.getId(),
                budget.getCustomerName(),
                budget.getDescription(),
                budget.getAmount(),
                budget.getStatus(),
                budget.getCreatedAt()
        );
    }

    private BudgetResponse toResponse(BudgetDocument budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getCorrelationId(),
                budget.getCustomerName(),
                budget.getDescription(),
                budget.getAmount(),
                budget.getStatus(),
                budget.getCreatedAt(),
                budget.getUpdatedAt()
        );
    }
}
