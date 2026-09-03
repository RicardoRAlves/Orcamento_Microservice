package com.br.capoeira.orcamento.budgetapi.dto;

import com.br.capoeira.orcamento.budgetapi.model.BudgetStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Resposta com os dados da solicitacao de orcamento")
public record BudgetResponse(
        @Schema(description = "Identificador UUID do orcamento", example = "7f0b0f0d-1d72-4e63-b4b1-2f50f958f527")
        UUID id,

        @Schema(description = "Nome do cliente solicitante", example = "Ricardo Alves")
        String customerName,

        @Schema(description = "Descricao resumida do servico ou produto do orcamento", example = "Orcamento para evento de capoeira")
        String description,

        @Schema(description = "Valor inicial informado para o orcamento", example = "1500.00")
        BigDecimal amount,

        @Schema(description = "Status atual da solicitacao", example = "RECEIVED")
        BudgetStatus status,

        @Schema(description = "Data de criacao da solicitacao", example = "2026-09-03T17:40:00Z")
        Instant createdAt,

        @Schema(description = "Data da ultima atualizacao da solicitacao", example = "2026-09-03T17:40:00Z")
        Instant updatedAt
) {
}
