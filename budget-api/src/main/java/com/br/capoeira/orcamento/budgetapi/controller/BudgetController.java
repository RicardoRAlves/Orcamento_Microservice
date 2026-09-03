package com.br.capoeira.orcamento.budgetapi.controller;

import com.br.capoeira.orcamento.budgetapi.dto.BudgetResponse;
import com.br.capoeira.orcamento.budgetapi.dto.CreateBudgetRequest;
import com.br.capoeira.orcamento.budgetapi.dto.ErrorResponse;
import com.br.capoeira.orcamento.budgetapi.service.BudgetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/budgets")
@Tag(name = "Budgets", description = "Operacoes para solicitacoes de orcamento")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    @Operation(
            summary = "Criar solicitacao de orcamento",
            description = "Recebe os dados iniciais do orcamento, valida o valor informado e registra a solicitacao com status RECEIVED.",
            responses = {
                    @ApiResponse(
                            responseCode = "202",
                            description = "Solicitacao aceita",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = BudgetResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Dados invalidos",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = "{\"status\":400,\"error\":\"Bad Request\",\"message\":\"Budget amount must be greater than zero\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "503",
                            description = "MongoDB indisponivel ou sem autenticacao",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<BudgetResponse> create(
            @Valid @RequestBody CreateBudgetRequest request) {

        var response = budgetService.create(request);

        return ResponseEntity
                .accepted()
                .body(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar orcamento por ID",
            description = "Retorna uma solicitacao de orcamento ja registrada no MongoDB.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Orcamento encontrado",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = BudgetResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "UUID invalido",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Orcamento nao encontrado",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "503",
                            description = "MongoDB indisponivel ou sem autenticacao",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    public BudgetResponse findById(
            @Parameter(
                    description = "Identificador UUID do orcamento",
                    example = "7f0b0f0d-1d72-4e63-b4b1-2f50f958f527"
            )
            @PathVariable UUID id) {

        return budgetService.findById(id);
    }
}
