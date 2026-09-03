package com.br.capoeira.orcamento.budgetorchestrator.exception;

public class BudgetProcessingException extends RuntimeException {

    public BudgetProcessingException(String message) {
        super(message);
    }

    public BudgetProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
