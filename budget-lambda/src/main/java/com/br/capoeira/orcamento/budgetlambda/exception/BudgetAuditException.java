package com.br.capoeira.orcamento.budgetlambda.exception;

public class BudgetAuditException extends RuntimeException {

    public BudgetAuditException(String message, Throwable cause) {
        super(message, cause);
    }
}
