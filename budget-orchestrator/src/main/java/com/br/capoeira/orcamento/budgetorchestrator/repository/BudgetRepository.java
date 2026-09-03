package com.br.capoeira.orcamento.budgetorchestrator.repository;

import com.br.capoeira.orcamento.budgetorchestrator.model.BudgetDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface BudgetRepository extends MongoRepository<BudgetDocument, UUID> {
}
