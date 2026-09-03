#!/usr/bin/env sh
awslocal sqs create-queue --queue-name budget-requests
awslocal sqs create-queue --queue-name budget-events
awslocal sqs create-queue --queue-name notification-requests

