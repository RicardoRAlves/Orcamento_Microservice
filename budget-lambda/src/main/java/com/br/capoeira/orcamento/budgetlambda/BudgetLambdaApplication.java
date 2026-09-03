package com.br.capoeira.orcamento.budgetlambda;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import java.util.function.Function;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BudgetLambdaApplication {

    @Bean
    public Function<SQSEvent, String> processBudgetRequest() {
        return event -> "processed " + event.getRecords().size() + " message(s)";
    }
}

