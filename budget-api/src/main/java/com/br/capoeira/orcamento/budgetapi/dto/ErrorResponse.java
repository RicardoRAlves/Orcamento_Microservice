package com.br.capoeira.orcamento.budgetapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Resposta padronizada para erros da API")
public record ErrorResponse(
        @Schema(description = "Data e hora do erro", example = "2026-09-03T17:40:00Z")
        Instant timestamp,

        @Schema(description = "Codigo HTTP", example = "400")
        int status,

        @Schema(description = "Nome do erro HTTP", example = "Bad Request")
        String error,

        @Schema(description = "Mensagem resumida do erro", example = "Budget amount must be greater than zero")
        String message,

        @Schema(description = "Caminho da requisicao", example = "/budgets/7f0b0f0d-1d72-4e63-b4b1-2f50f958f527")
        String path,

        @Schema(description = "Erros por campo, quando houver validacao de payload")
        Map<String, String> fields
) {
}
