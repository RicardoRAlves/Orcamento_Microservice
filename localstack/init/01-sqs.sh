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

awslocal sqs create-queue --queue-name budget-events
awslocal sqs create-queue --queue-name notification-requests
