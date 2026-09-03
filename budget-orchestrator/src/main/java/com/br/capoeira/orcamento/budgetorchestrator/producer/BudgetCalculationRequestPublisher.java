package com.br.capoeira.orcamento.budgetorchestrator.producer;

import com.br.capoeira.orcamento.budgetorchestrator.dto.BudgetCalculationRequestMessage;
import com.br.capoeira.orcamento.budgetorchestrator.exception.BudgetCalculationPublishException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

@Component
public class BudgetCalculationRequestPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BudgetCalculationRequestPublisher.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueName;

    public BudgetCalculationRequestPublisher(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            @Value("${aws.sqs.budget-calculation-requests-queue}") String queueName) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueName = queueName;
    }

    public void publish(BudgetCalculationRequestMessage message) {
        try {
            var messageBody = objectMapper.writeValueAsString(message);
            var queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                    .queueName(queueName)
                    .build())
                    .queueUrl();

            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build());

            LOGGER.info(
                    "Budget calculation request published. budgetId={}, queueName={}",
                    message.budgetId(),
                    queueName
            );
        } catch (JsonProcessingException exception) {
            LOGGER.error(
                    "Could not serialize budget calculation request. budgetId={}, queueName={}",
                    message.budgetId(),
                    queueName,
                    exception
            );

            throw new BudgetCalculationPublishException(
                    message.budgetId(),
                    "Could not serialize budget calculation request",
                    exception
            );
        } catch (SqsException | SdkClientException exception) {
            LOGGER.error(
                    "Could not publish budget calculation request. budgetId={}, queueName={}, error={}",
                    message.budgetId(),
                    queueName,
                    exception.getMessage(),
                    exception
            );

            throw new BudgetCalculationPublishException(
                    message.budgetId(),
                    "Could not publish budget calculation request",
                    exception
            );
        }
    }
}
