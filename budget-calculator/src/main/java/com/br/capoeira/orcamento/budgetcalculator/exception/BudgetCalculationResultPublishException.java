package com.br.capoeira.orcamento.budgetcalculator.exception;

import java.util.UUID;

public class BudgetCalculationResultPublishException extends RuntimeException {

    private final UUID budgetId;

    public BudgetCalculationResultPublishException(UUID budgetId, String message, Throwable cause) {
        super(message, cause);
        this.budgetId = budgetId;
    }

    public UUID getBudgetId() {
        return budgetId;
    }
}
