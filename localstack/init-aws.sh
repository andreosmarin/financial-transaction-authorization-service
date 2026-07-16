#!/bin/sh
set -eu

echo "Creating LocalStack SQS queues"

awslocal sqs create-queue \
  --queue-name conta-bancaria-criada-dlq \
  --attributes '{"MessageRetentionPeriod":"1209600"}' >/dev/null

awslocal sqs create-queue \
  --queue-name conta-bancaria-criada \
  --attributes '{"RedrivePolicy":"{\"deadLetterTargetArn\":\"arn:aws:sqs:sa-east-1:000000000000:conta-bancaria-criada-dlq\",\"maxReceiveCount\":\"5\"}","VisibilityTimeout":"30","ReceiveMessageWaitTimeSeconds":"10"}' >/dev/null

echo "LocalStack SQS queues created"
