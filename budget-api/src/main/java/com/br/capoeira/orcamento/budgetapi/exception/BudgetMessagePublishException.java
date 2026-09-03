package com.br.capoeira.orcamento.budgetapi.exception;

import java.util.UUID;

public class BudgetMessagePublishException extends RuntimeException {

    private final UUID budgetId;

    public BudgetMessagePublishException(UUID budgetId, String message, Throwable cause) {
        super(message, cause);
        this.budgetId = budgetId;
    }

    public UUID getBudgetId() {
        return budgetId;
    }
}
