package com.br.capoeira.orcamento.notificationservice.consumer;

import com.br.capoeira.orcamento.notificationservice.dto.BudgetEventMessage;
import com.br.capoeira.orcamento.notificationservice.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

@Component
@ConditionalOnProperty(name = "aws.sqs.consumer.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationRequestConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationRequestConsumer.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final String queueName;
    private final int maxMessages;
    private final int waitTimeSeconds;

    private String queueUrl;

    public NotificationRequestConsumer(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            NotificationService notificationService,
            @Value("${aws.sqs.notification-requests-queue}") String queueName,
            @Value("${aws.sqs.max-messages}") int maxMessages,
            @Value("${aws.sqs.wait-time-seconds}") int waitTimeSeconds) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.queueName = queueName;
        this.maxMessages = maxMessages;
        this.waitTimeSeconds = waitTimeSeconds;
    }

    @PostConstruct
    public void initializeQueueUrl() {
        queueUrl = sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build())
                .queueUrl();

        LOGGER.info("Notification request consumer connected to queue. queueName={}, queueUrl={}", queueName, queueUrl);
    }

    @Scheduled(fixedDelayString = "${aws.sqs.polling-delay-ms}")
    public void poll() {
        try {
            var response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(maxMessages)
                    .waitTimeSeconds(waitTimeSeconds)
                    .build());

            response.messages().forEach(this::handleMessage);
        } catch (SqsException | SdkClientException exception) {
            LOGGER.error("Could not poll notification request queue. queueName={}, error={}", queueName, exception.getMessage(), exception);
        }
    }

    private void handleMessage(Message sqsMessage) {
        try {
            var eventMessage = objectMapper.readValue(sqsMessage.body(), BudgetEventMessage.class);
            notificationService.notifyBudgetFinished(eventMessage);
            deleteMessage(sqsMessage);
        } catch (JsonProcessingException exception) {
            LOGGER.error(
                    "Invalid notification request message. It will not be deleted and should be moved to DLQ after retries. messageId={}, body={}",
                    sqsMessage.messageId(),
                    sqsMessage.body(),
                    exception
            );
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Could not process notification request message. It will not be deleted and should be retried by SQS. messageId={}, error={}",
                    sqsMessage.messageId(),
                    exception.getMessage(),
                    exception
            );
        }
    }

    private void deleteMessage(Message sqsMessage) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(sqsMessage.receiptHandle())
                .build());
    }
}
