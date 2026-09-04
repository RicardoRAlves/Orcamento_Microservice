package com.br.capoeira.orcamento.budgetcalculator.producer;

import com.br.capoeira.orcamento.budgetcalculator.dto.BudgetCalculationResultMessage;
import com.br.capoeira.orcamento.budgetcalculator.exception.BudgetCalculationResultPublishException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.util.Map;

@Component
public class BudgetCalculationResultPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BudgetCalculationResultPublisher.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueName;

    public BudgetCalculationResultPublisher(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            @Value("${aws.sqs.budget-calculation-results-queue}") String queueName) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueName = queueName;
    }

    public void publish(BudgetCalculationResultMessage message) {
        try {
            var queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                    .queueName(queueName)
                    .build())
                    .queueUrl();

            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(objectMapper.writeValueAsString(message))
                    .messageAttributes(messageAttributes(message))
                    .build());

            LOGGER.info(
                    "Budget calculation result published. budgetId={}, correlationId={}, queueName={}",
                    message.budgetId(),
                    message.correlationId(),
                    queueName
            );
        } catch (JsonProcessingException exception) {
            LOGGER.error(
                    "Could not serialize budget calculation result. budgetId={}, correlationId={}, queueName={}",
                    message.budgetId(),
                    message.correlationId(),
                    queueName,
                    exception
            );
            throw new BudgetCalculationResultPublishException(message.budgetId(), "Could not serialize budget calculation result", exception);
        } catch (SqsException | SdkClientException exception) {
            LOGGER.error(
                    "Could not publish budget calculation result. budgetId={}, correlationId={}, queueName={}, error={}",
                    message.budgetId(),
                    message.correlationId(),
                    queueName,
                    exception.getMessage(),
                    exception
            );
            throw new BudgetCalculationResultPublishException(message.budgetId(), "Could not publish budget calculation result", exception);
        }
    }

    private Map<String, MessageAttributeValue> messageAttributes(BudgetCalculationResultMessage message) {
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
