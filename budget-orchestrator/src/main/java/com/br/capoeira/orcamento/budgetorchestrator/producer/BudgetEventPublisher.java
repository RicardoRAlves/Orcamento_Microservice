package com.br.capoeira.orcamento.budgetorchestrator.producer;

import com.br.capoeira.orcamento.budgetorchestrator.dto.BudgetEventMessage;
import com.br.capoeira.orcamento.budgetorchestrator.exception.BudgetEventPublishException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.util.Map;

@Component
public class BudgetEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BudgetEventPublisher.class);

    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;
    private final String topicArn;

    public BudgetEventPublisher(
            SnsClient snsClient,
            ObjectMapper objectMapper,
            @Value("${aws.sns.budget-events-topic-arn}") String topicArn) {
        this.snsClient = snsClient;
        this.objectMapper = objectMapper;
        this.topicArn = topicArn;
    }

    public void publish(BudgetEventMessage message) {
        try {
            snsClient.publish(PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(objectMapper.writeValueAsString(message))
                    .messageAttributes(messageAttributes(message))
                    .build());

            LOGGER.info(
                    "Budget event published. budgetId={}, correlationId={}, status={}, topicArn={}",
                    message.budgetId(),
                    message.correlationId(),
                    message.status(),
                    topicArn
            );
        } catch (JsonProcessingException exception) {
            LOGGER.error(
                    "Could not serialize budget event. budgetId={}, correlationId={}, topicArn={}",
                    message.budgetId(),
                    message.correlationId(),
                    topicArn,
                    exception
            );
            throw new BudgetEventPublishException(message.budgetId(), "Could not serialize budget event", exception);
        } catch (SnsException | SdkClientException exception) {
            LOGGER.error(
                    "Could not publish budget event. budgetId={}, correlationId={}, topicArn={}, error={}",
                    message.budgetId(),
                    message.correlationId(),
                    topicArn,
                    exception.getMessage(),
                    exception
            );
            throw new BudgetEventPublishException(message.budgetId(), "Could not publish budget event", exception);
        }
    }

    private Map<String, MessageAttributeValue> messageAttributes(BudgetEventMessage message) {
        return Map.of(
                "eventType", stringAttribute(message.eventType()),
                "eventVersion", stringAttribute(message.eventVersion()),
                "correlationId", stringAttribute(message.correlationId().toString())
        );
    }

    private MessageAttributeValue stringAttribute(String value) {
        return MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(value)
                .build();
    }
}
