package com.br.capoeira.orcamento.budgetapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Dados para criacao de uma solicitacao de orcamento")
public record CreateBudgetRequest(
        @NotBlank(message = "Customer name is required")
        @Schema(description = "Nome do cliente solicitante", example = "Ricardo Alves")
        String customerName,

        @NotBlank(message = "Description is required")
        @Schema(description = "Descricao resumida do servico ou produto do orcamento", example = "Orcamento para evento de capoeira")
        String description,

        @NotNull(message = "Budget amount is required")
        @DecimalMin(value = "0.01", message = "Budget amount must be greater than zero")
        @Schema(description = "Valor inicial informado para o orcamento", example = "1500.00")
        BigDecimal amount
) {
}
