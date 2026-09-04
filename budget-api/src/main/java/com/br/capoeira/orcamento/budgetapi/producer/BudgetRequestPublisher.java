package com.br.capoeira.orcamento.budgetapi.producer;

import com.br.capoeira.orcamento.budgetapi.exception.BudgetMessagePublishException;
import com.br.capoeira.orcamento.budgetapi.message.BudgetRequestMessage;
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
public class BudgetRequestPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BudgetRequestPublisher.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueName;

    public BudgetRequestPublisher(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            @Value("${aws.sqs.budget-requests-queue}") String queueName) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueName = queueName;
    }

    public void publish(BudgetRequestMessage message) {
        try {
            var messageBody = toJson(message);

            var queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                    .queueName(queueName)
                    .build());

            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl.queueUrl())
                    .messageBody(messageBody)
                    .messageAttributes(messageAttributes(message))
                    .build());
        } catch (JsonProcessingException exception) {
            LOGGER.error(
                    "Could not serialize budget request message. budgetId={}, correlationId={}, queueName={}",
                    message.budgetId(),
                    message.correlationId(),
                    queueName,
                    exception
            );

            throw new BudgetMessagePublishException(
                    message.budgetId(),
                    "Could not serialize budget request message",
                    exception
            );
        } catch (SqsException | SdkClientException exception) {
            LOGGER.error(
                    "Could not publish budget request message. budgetId={}, correlationId={}, queueName={}, error={}",
                    message.budgetId(),
                    message.correlationId(),
                    queueName,
                    exception.getMessage(),
                    exception
            );

            throw new BudgetMessagePublishException(
                    message.budgetId(),
                    "Could not publish budget request message",
                    exception
            );
        }
    }

    private String toJson(BudgetRequestMessage message) throws JsonProcessingException {
        return objectMapper.writeValueAsString(message);
    }

    private Map<String, MessageAttributeValue> messageAttributes(BudgetRequestMessage message) {
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
