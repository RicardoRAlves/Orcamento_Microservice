package com.br.capoeira.orcamento.budgetorchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BudgetOrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(BudgetOrchestratorApplication.class, args);
    }
}
