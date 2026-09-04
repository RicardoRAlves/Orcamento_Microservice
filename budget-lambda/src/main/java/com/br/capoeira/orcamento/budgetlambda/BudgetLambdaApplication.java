package com.br.capoeira.orcamento.budgetlambda;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.br.capoeira.orcamento.budgetlambda.service.BudgetAuditService;
import java.util.function.Function;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BudgetLambdaApplication {

    @Bean
    public Function<SNSEvent, String> processBudgetEvent(BudgetAuditService budgetAuditService) {
        return event -> {
            int processedMessages = budgetAuditService.audit(event);
            return "audited " + processedMessages + " budget event(s)";
        };
    }
}
