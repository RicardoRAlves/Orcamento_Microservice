package com.br.capoeira.orcamento.budgetlambda;

import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BudgetLambdaApplicationTests {

    @Autowired
    private Function<SNSEvent, String> processBudgetEvent;

    @Test
    void contextLoads() {
        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setMessage("""
                {
                  "budgetId": "11111111-1111-1111-1111-111111111111",
                  "customerName": "Ricardo Alves",
                  "description": "Teste auditoria",
                  "originalAmount": 1500,
                  "calculatedAmount": 1350.00,
                  "status": "CALCULATED",
                  "errorMessage": null,
                  "occurredAt": "2026-09-04T12:00:00Z"
                }
                """);

        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        record.setSns(sns);

        SNSEvent event = new SNSEvent();
        event.setRecords(List.of(record));

        assertThat(processBudgetEvent.apply(event)).isEqualTo("audited 1 budget event(s)");
    }
}
