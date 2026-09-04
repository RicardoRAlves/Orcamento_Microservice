#!/usr/bin/env sh

set -e

LAMBDA_NAME="${BUDGET_AUDIT_LAMBDA_NAME:-budget-audit-lambda}"
LAMBDA_JAR="${BUDGET_AUDIT_LAMBDA_JAR:-/opt/lambda-artifacts/budget-lambda-0.0.1-SNAPSHOT.jar}"
LAMBDA_ROLE_NAME="${BUDGET_AUDIT_LAMBDA_ROLE_NAME:-budget-audit-lambda-role}"
LAMBDA_ROLE_ARN="arn:aws:iam::000000000000:role/${LAMBDA_ROLE_NAME}"
TOPIC_ARN="${BUDGET_EVENTS_TOPIC_ARN:-arn:aws:sns:us-east-1:000000000000:budget-events}"
LAMBDA_MAIN_CLASS="${BUDGET_AUDIT_LAMBDA_MAIN_CLASS:-com.br.capoeira.orcamento.budgetlambda.BudgetLambdaApplication}"

if [ ! -f "$LAMBDA_JAR" ]; then
  echo "Lambda artifact not found at $LAMBDA_JAR. Run 'mvn -pl budget-lambda package' and recreate LocalStack to deploy it."
  exit 0
fi

awslocal iam create-role \
  --role-name "$LAMBDA_ROLE_NAME" \
  --assume-role-policy-document '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"lambda.amazonaws.com"},"Action":"sts:AssumeRole"}]}' \
  >/dev/null 2>&1 || true

if awslocal lambda get-function --function-name "$LAMBDA_NAME" >/dev/null 2>&1; then
  awslocal lambda update-function-code \
    --function-name "$LAMBDA_NAME" \
    --zip-file "fileb://${LAMBDA_JAR}" >/dev/null

  awslocal lambda update-function-configuration \
    --function-name "$LAMBDA_NAME" \
    --handler org.springframework.cloud.function.adapter.aws.FunctionInvoker::handleRequest \
    --runtime java17 \
    --role "$LAMBDA_ROLE_ARN" \
    --timeout 30 \
    --memory-size 512 \
    --environment Variables="{SPRING_CLOUD_FUNCTION_DEFINITION=processBudgetEvent,MAIN_CLASS=${LAMBDA_MAIN_CLASS}}" >/dev/null
else
  awslocal lambda create-function \
    --function-name "$LAMBDA_NAME" \
    --runtime java17 \
    --role "$LAMBDA_ROLE_ARN" \
    --handler org.springframework.cloud.function.adapter.aws.FunctionInvoker::handleRequest \
    --zip-file "fileb://${LAMBDA_JAR}" \
    --timeout 30 \
    --memory-size 512 \
    --environment Variables="{SPRING_CLOUD_FUNCTION_DEFINITION=processBudgetEvent,MAIN_CLASS=${LAMBDA_MAIN_CLASS}}" >/dev/null
fi

awslocal lambda add-permission \
  --function-name "$LAMBDA_NAME" \
  --statement-id AllowBudgetEventsSnsInvoke \
  --action lambda:InvokeFunction \
  --principal sns.amazonaws.com \
  --source-arn "$TOPIC_ARN" >/dev/null 2>&1 || true

LAMBDA_ARN=$(awslocal lambda get-function \
  --function-name "$LAMBDA_NAME" \
  --query 'Configuration.FunctionArn' \
  --output text)

awslocal sns subscribe \
  --topic-arn "$TOPIC_ARN" \
  --protocol lambda \
  --notification-endpoint "$LAMBDA_ARN" >/dev/null
