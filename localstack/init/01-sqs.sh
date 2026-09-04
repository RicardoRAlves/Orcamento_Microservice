#!/usr/bin/env sh
create_queue_with_dlq() {
  QUEUE_NAME="$1"
  DLQ_NAME="${QUEUE_NAME}-dlq"

  DLQ_URL=$(awslocal sqs create-queue --queue-name "$DLQ_NAME" --query QueueUrl --output text)
  DLQ_ARN=$(awslocal sqs get-queue-attributes \
    --queue-url "$DLQ_URL" \
    --attribute-names QueueArn \
    --query 'Attributes.QueueArn' \
    --output text)

  awslocal sqs create-queue \
    --queue-name "$QUEUE_NAME" \
    --attributes "{\"VisibilityTimeout\":\"30\",\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"$DLQ_ARN\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"}"
}

create_queue_with_dlq budget-requests
create_queue_with_dlq budget-calculation-requests
create_queue_with_dlq budget-calculation-results

BUDGET_EVENTS_TOPIC_ARN=$(awslocal sns create-topic \
  --name budget-events \
  --query TopicArn \
  --output text)

create_queue_with_dlq notification-requests

NOTIFICATION_QUEUE_URL=$(awslocal sqs get-queue-url --queue-name notification-requests --query QueueUrl --output text)
NOTIFICATION_QUEUE_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$NOTIFICATION_QUEUE_URL" \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' \
  --output text)

awslocal sns subscribe \
  --topic-arn "$BUDGET_EVENTS_TOPIC_ARN" \
  --protocol sqs \
  --notification-endpoint "$NOTIFICATION_QUEUE_ARN" \
  --attributes RawMessageDelivery=true
