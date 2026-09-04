package com.br.capoeira.orcamento.budgetcalculator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BudgetCalculatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(BudgetCalculatorApplication.class, args);
    }
}
