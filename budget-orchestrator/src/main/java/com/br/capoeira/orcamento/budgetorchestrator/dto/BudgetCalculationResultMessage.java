package com.br.capoeira.orcamento.budgetorchestrator.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BudgetCalculationResultMessage(
        UUID budgetId,
        BigDecimal originalAmount,
        BigDecimal calculatedAmount,
        String status,
        String errorMessage,
        Instant calculatedAt
) {
}
