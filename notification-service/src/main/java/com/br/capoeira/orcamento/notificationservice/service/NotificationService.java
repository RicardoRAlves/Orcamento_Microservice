package com.br.capoeira.orcamento.notificationservice.service;

import com.br.capoeira.orcamento.notificationservice.dto.BudgetEventMessage;
import com.br.capoeira.orcamento.notificationservice.exception.NotificationProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    public void notifyBudgetFinished(BudgetEventMessage message) {
        validate(message);

        LOGGER.info(
                "Budget notification processed. eventType={}, eventVersion={}, budgetId={}, correlationId={}, customerName={}, status={}, originalAmount={}, calculatedAmount={}, errorMessage={}",
                message.eventType(),
                message.eventVersion(),
                message.budgetId(),
                message.correlationId(),
                message.customerName(),
                message.status(),
                message.originalAmount(),
                message.calculatedAmount(),
                message.errorMessage()
        );
    }

    private void validate(BudgetEventMessage message) {
        if (message == null || message.budgetId() == null) {
            throw new NotificationProcessingException("Budget event message must contain budgetId");
        }

        if (message.status() == null || message.status().isBlank()) {
            throw new NotificationProcessingException("Budget event message must contain status");
        }
    }
}
