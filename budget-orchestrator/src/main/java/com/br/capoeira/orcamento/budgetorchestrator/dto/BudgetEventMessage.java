package com.br.capoeira.orcamento.budgetorchestrator.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BudgetEventMessage(
        String eventType,
        String eventVersion,
        UUID correlationId,
        UUID budgetId,
        String customerName,
        String description,
        BigDecimal originalAmount,
        BigDecimal calculatedAmount,
        BudgetStatus status,
        String errorMessage,
        Instant occurredAt
) {
}
