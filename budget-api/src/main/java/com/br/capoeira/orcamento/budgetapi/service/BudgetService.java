package com.br.capoeira.orcamento.budgetapi.service;

import com.br.capoeira.orcamento.budgetapi.dto.BudgetResponse;
import com.br.capoeira.orcamento.budgetapi.dto.CreateBudgetRequest;
import com.br.capoeira.orcamento.budgetapi.model.BudgetDocument;
import com.br.capoeira.orcamento.budgetapi.repository.BudgetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class BudgetService {

    private static final String INITIAL_STATUS = "RECEIVED";

    private final BudgetRepository budgetRepository;

    public BudgetService(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    public BudgetResponse create(CreateBudgetRequest request) {
        validateAmount(request);

        var now = Instant.now();

        var budget = new BudgetDocument();
        budget.setId(UUID.randomUUID());
        budget.setCustomerName(request.customerName());
        budget.setDescription(request.description());
        budget.setAmount(request.amount());
        budget.setStatus(INITIAL_STATUS);
        budget.setCreatedAt(now);
        budget.setUpdatedAt(now);

        return toResponse(budgetRepository.save(budget));
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

    private BudgetResponse toResponse(BudgetDocument budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getCustomerName(),
                budget.getDescription(),
                budget.getAmount(),
                budget.getStatus(),
                budget.getCreatedAt(),
                budget.getUpdatedAt()
        );
    }
}
