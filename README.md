# Orcamento Microservice

Base inicial para o projeto de orcamentos usando Spring Boot, AWS Lambda e SQS.

## Modulos

- `budget-api`: API REST para receber solicitacoes de orcamento.
- `budget-processor`: consumidor SQS para processar solicitacoes assincronas.
- `budget-calculator`: servico de dominio para regras de calculo de orcamento.
- `notification-service`: servico para notificacoes relacionadas ao fluxo.
- `budget-lambda`: entrada Lambda usando Spring Cloud Function.

## Requisitos

- Java 17
- Maven 3.9+

## Comandos

```bash
mvn clean test
mvn -pl budget-api spring-boot:run
```

## Infra local

O `docker-compose.yml` sobe LocalStack com SQS habilitado para desenvolvimento local.

```bash
docker compose up -d
```

