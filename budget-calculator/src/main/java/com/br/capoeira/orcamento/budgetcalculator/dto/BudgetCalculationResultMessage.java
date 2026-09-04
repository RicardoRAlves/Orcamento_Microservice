package com.br.capoeira.orcamento.budgetcalculator.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BudgetCalculationResultMessage(
        String eventType,
        String eventVersion,
        UUID correlationId,
        UUID budgetId,
        BigDecimal originalAmount,
        BigDecimal calculatedAmount,
        String status,
        String errorMessage,
        Instant calculatedAt
) {
}
