package com.br.capoeira.orcamento.budgetapi.repository;

import com.br.capoeira.orcamento.budgetapi.model.BudgetDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface BudgetRepository extends MongoRepository<BudgetDocument, UUID> {
}
