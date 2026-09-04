package com.br.capoeira.orcamento.budgetlambda.service;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.br.capoeira.orcamento.budgetlambda.dto.BudgetEventMessage;
import com.br.capoeira.orcamento.budgetlambda.exception.BudgetAuditException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BudgetAuditService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BudgetAuditService.class);

    private final ObjectMapper objectMapper;

    public BudgetAuditService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public int audit(SNSEvent event) {
        if (event == null || event.getRecords() == null) {
            return 0;
        }

        event.getRecords().forEach(this::auditRecord);
        return event.getRecords().size();
    }

    private void auditRecord(SNSEvent.SNSRecord record) {
        try {
            var message = objectMapper.readValue(record.getSNS().getMessage(), BudgetEventMessage.class);
            LOGGER.info(
                    "Budget audit event received. eventType={}, eventVersion={}, budgetId={}, correlationId={}, status={}, originalAmount={}, calculatedAmount={}, occurredAt={}",
                    message.eventType(),
                    message.eventVersion(),
                    message.budgetId(),
                    message.correlationId(),
                    message.status(),
                    message.originalAmount(),
                    message.calculatedAmount(),
                    message.occurredAt()
            );
        } catch (JsonProcessingException exception) {
            throw new BudgetAuditException("Could not parse budget event from SNS", exception);
        }
    }
}
