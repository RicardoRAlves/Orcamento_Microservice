package com.br.capoeira.orcamento.budgetapi.message;

import com.br.capoeira.orcamento.budgetapi.model.BudgetStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BudgetRequestMessage(
        UUID budgetId,
        String customerName,
        String description,
        BigDecimal amount,
        BudgetStatus status,
        Instant createdAt
) {
}
