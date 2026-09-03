package com.br.capoeira.orcamento.budgetlambda;

import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BudgetLambdaApplicationTests {

    @Autowired
    private Function<SQSEvent, String> processBudgetRequest;

    @Test
    void contextLoads() {
        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(new SQSEvent.SQSMessage()));

        assertThat(processBudgetRequest.apply(event)).isEqualTo("processed 1 message(s)");
    }
}

