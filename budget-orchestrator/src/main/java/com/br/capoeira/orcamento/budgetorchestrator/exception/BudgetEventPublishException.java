package com.br.capoeira.orcamento.budgetorchestrator.exception;

import java.util.UUID;

public class BudgetEventPublishException extends RuntimeException {

    private final UUID budgetId;

    public BudgetEventPublishException(UUID budgetId, String message, Throwable cause) {
        super(message, cause);
        this.budgetId = budgetId;
    }

    public UUID getBudgetId() {
        return budgetId;
    }
}
