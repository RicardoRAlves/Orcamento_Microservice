package com.br.capoeira.orcamento.budgetorchestrator.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BudgetCalculationRequestMessage(
        UUID budgetId,
        String customerName,
        String description,
        BigDecimal amount,
        Instant requestedAt
) {
}
