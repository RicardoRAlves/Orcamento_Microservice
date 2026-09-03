package com.br.capoeira.orcamento.budgetorchestrator.exception;

import java.util.UUID;

public class BudgetCalculationPublishException extends RuntimeException {

    private final UUID budgetId;

    public BudgetCalculationPublishException(UUID budgetId, String message, Throwable cause) {
        super(message, cause);
        this.budgetId = budgetId;
    }

    public UUID getBudgetId() {
        return budgetId;
    }
}
