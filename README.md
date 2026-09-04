# Orcamento Microservice

Projeto de estudo e portfolio para um fluxo de orcamentos usando Spring Boot, MongoDB, Amazon SQS, AWS Lambda, SNS e LocalStack.

O objetivo do projeto e separar o fluxo de solicitacao, processamento, calculo e notificacao de orcamentos em modulos pequenos, com comunicacao assincrona entre as etapas que nao precisam responder imediatamente ao usuario.

Estado atual do projeto:

- `budget-api` recebe e consulta orcamentos via REST.
- `budget-api` salva o orcamento no MongoDB e publica uma mensagem na fila `budget-requests`.
- `budget-orchestrator` consome `budget-requests`, atualiza o status no MongoDB e publica uma mensagem para `budget-calculation-requests`.
- `budget-calculator` consome `budget-calculation-requests`, calcula o valor e publica o resultado em `budget-calculation-results`.
- `budget-orchestrator` consome `budget-calculation-results` e atualiza o MongoDB com `CALCULATED` ou `FAILED`.
- As filas principais possuem DLQ configurada no LocalStack.
- `budget-calculator`, `notification-service`, `budget-lambda` e SNS estao preparados como proximas etapas de evolucao.

## Visao geral

```mermaid
flowchart LR
    Client[Cliente / Frontend] --> API[budget-api]
    API --> QueueRequests[(SQS: budget-requests)]
    QueueRequests --> Orchestrator[budget-orchestrator]
    Orchestrator --> QueueCalculation[(SQS: budget-calculation-requests)]
    QueueCalculation --> Calculator[budget-calculator]
    Calculator --> QueueResults[(SQS: budget-calculation-results)]
    QueueResults --> Orchestrator
    Orchestrator -. proxima etapa .-> TopicEvents[(SNS: budget-events)]
    TopicEvents -. fanout .-> QueueNotifications[(SQS: notification-requests)]
    QueueNotifications --> Notification[notification-service]
    TopicEvents -. auditoria .-> Lambda[budget-lambda]
```

## Modulos

### `budget-api`

API REST responsavel por receber solicitacoes de orcamento e iniciar o fluxo.

Responsabilidades previstas:

- Expor endpoints HTTP para cadastro e consulta de solicitacoes de orcamento.
- Validar payloads de entrada com Bean Validation.
- Validar regra de negocio inicial para `amount`, garantindo valor preenchido e maior que zero.
- Persistir o orcamento no MongoDB com status inicial `RECEIVED`.
- Publicar solicitacoes validas na fila `budget-requests`.
- Marcar o orcamento como `PUBLISH_FAILED` quando nao conseguir publicar no SQS.
- Centralizar respostas de erro em um `GlobalExceptionHandler`.
- Expor documentacao OpenAPI/Swagger para facilitar testes.
- Disponibilizar endpoints operacionais via Actuator.

Tecnologias principais:

- Spring Boot Web
- Spring Boot Validation
- Spring Data MongoDB
- Springdoc OpenAPI
- AWS SDK SQS
- Spring Boot Actuator

### `budget-orchestrator`

Servico assincrono responsavel por orquestrar o ciclo de vida do orcamento.

Responsabilidades previstas:

- Consumir mensagens da fila `budget-requests`.
- Converter a mensagem de entrada para o modelo interno de orquestracao.
- Atualizar o status do orcamento no MongoDB de forma idempotente.
- Publicar solicitacoes de calculo na fila `budget-calculation-requests`.
- Controlar estados do processamento, como `RECEIVED`, `PROCESSING`, `CALCULATION_REQUESTED`, `CALCULATED` e `FAILED`.
- Evitar duplicidade: se o orcamento ja estiver `CALCULATION_REQUESTED`, `CALCULATED` ou `FAILED`, a mensagem reprocessada nao gera nova publicacao.
- Nao deletar a mensagem original quando houver erro de leitura, persistencia ou publicacao para o calculator; isso permite retry automatico do SQS.
- Publicar eventos de dominio em SNS quando o fluxo de eventos for implementado.
- Publicar pedidos de notificacao quando o resultado do calculo estiver concluido.

Tecnologias principais:

- Spring Boot
- Spring Data MongoDB
- AWS SDK SQS
- Spring Boot Actuator

### `budget-calculator`

Servico de dominio dedicado as regras de calculo de orcamento.

Responsabilidades previstas:

- Centralizar regras de precificacao, descontos, taxas, adicionais e totais.
- Manter a logica de calculo isolada dos detalhes de transporte HTTP, SQS ou Lambda.
- Consumir solicitacoes de calculo da fila `budget-calculation-requests`.
- Publicar o resultado do calculo na fila `budget-calculation-results`.
- Expor uma interface de calculo que possa ser usada por testes e por adapters de mensageria.
- Validar consistencia dos itens do orcamento antes de calcular totais.
- Facilitar testes unitarios das regras de negocio sem depender da infraestrutura AWS.

Tecnologias principais:

- Spring Boot Web
- Spring Boot Actuator
- JUnit/Spring Boot Test

### `notification-service`

Servico responsavel por notificacoes relacionadas ao ciclo de vida do orcamento.

Responsabilidades previstas:

- Consumir mensagens da fila `notification-requests`.
- Montar mensagens de notificacao com base no status e no resultado do orcamento.
- Encapsular integracoes futuras com e-mail, SMS, WhatsApp, webhooks ou outro canal.
- Evitar que falhas de notificacao bloqueiem o calculo do orcamento.
- Registrar tentativas, falhas e resultados de envio para observabilidade.

Tecnologias principais:

- Spring Boot
- AWS SDK SQS
- Spring Boot Actuator

### `budget-lambda`

Modulo serverless para auditoria e integracoes do fluxo de orcamento via AWS Lambda.

Responsabilidades previstas:

- Expor uma funcao Spring Cloud Function chamada `processBudgetRequest`.
- Ser acionado por eventos publicados no SNS.
- Registrar auditoria tecnica do fluxo, como status final, falhas, tempo de processamento ou integracoes futuras.
- Gerar um artefato empacotado com `maven-shade-plugin`, adequado para deploy em Lambda.
- Reaproveitar contratos e regras do fluxo principal quando o projeto evoluir para bibliotecas compartilhadas.

Tecnologias principais:

- Spring Cloud Function
- Spring Cloud Function AWS Adapter
- AWS Lambda Java Core
- AWS Lambda Java Events
- Maven Shade Plugin

## Fluxo de processamento

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente
    participant A as budget-api
    participant Q1 as SQS budget-requests
    participant P as budget-orchestrator
    participant QC as SQS budget-calculation-requests
    participant Calc as budget-calculator
    participant QR as SQS budget-calculation-results
    participant SNS as SNS budget-events
    participant L as budget-lambda
    participant Q2 as SQS notification-requests
    participant N as notification-service

    C->>A: Envia solicitacao de orcamento
    A->>A: Valida entrada e salva RECEIVED
    A->>Q1: Publica BudgetRequest
    A-->>C: Retorna aceite do processamento
    P->>Q1: Consome BudgetRequest
    P->>P: Atualiza status para PROCESSING
    P->>QC: Publica BudgetCalculationRequest
    P->>P: Atualiza status para CALCULATION_REQUESTED
    Calc->>QC: Consome BudgetCalculationRequest
    Calc->>Calc: Aplica regra de calculo
    Calc->>QR: Publica BudgetCalculationResult
    P->>QR: Consome BudgetCalculationResult
    P->>P: Atualiza status para CALCULATED ou FAILED
    P->>SNS: Publica evento final em etapa futura
    SNS->>L: Aciona auditoria serverless
    SNS->>Q2: Entrega evento para notificacao
    N->>Q2: Consome notificacao
    N-->>C: Envia atualizacao pelo canal configurado
```

## Filas locais

O ambiente local usa LocalStack para simular servicos AWS. O script `localstack/init/01-sqs.sh` cria as filas:

- `budget-requests`: entrada principal das solicitacoes de orcamento.
- `budget-requests-dlq`: mensagens de `budget-requests` que falharam apos as tentativas configuradas.
- `budget-calculation-requests`: entrada das solicitacoes de calculo para o `budget-calculator`.
- `budget-calculation-requests-dlq`: mensagens de calculo que falharam apos as tentativas configuradas.
- `budget-calculation-results`: resultados de calculo enviados pelo `budget-calculator` para o `budget-orchestrator`.
- `budget-calculation-results-dlq`: resultados de calculo que falharam apos as tentativas configuradas.
- `budget-events`: eventos de dominio ou status emitidos durante o processamento.
- `notification-requests`: solicitacoes de notificacao geradas pelo fluxo.

As filas `budget-requests`, `budget-calculation-requests` e `budget-calculation-results` sao criadas com:

- `VisibilityTimeout`: `30` segundos.
- `maxReceiveCount`: `3`.
- DLQ associada para mensagens que falham depois das tentativas configuradas.

```mermaid
flowchart TB
    subgraph LocalStack
        BR[budget-requests]
        BRDLQ[budget-requests-dlq]
        CR[budget-calculation-requests]
        CRDLQ[budget-calculation-requests-dlq]
        RS[budget-calculation-results]
        RSDLQ[budget-calculation-results-dlq]
        BE[budget-events]
        NR[notification-requests]
    end

    API[budget-api] --> BR
    BR --> Orchestrator[budget-orchestrator]
    BR -. falha apos retries .-> BRDLQ
    Orchestrator --> CR
    CR --> Calculator[budget-calculator]
    CR -. falha apos retries .-> CRDLQ
    Calculator --> RS
    RS --> Orchestrator
    RS -. falha apos retries .-> RSDLQ
    Orchestrator -. proxima etapa .-> BE
    BE -. proxima etapa .-> NR
    NR --> Notifications[notification-service]
```

## Estados do orcamento

Estados usados no fluxo:

- `RECEIVED`: orcamento criado pela API e aceito para processamento.
- `PUBLISH_FAILED`: API salvou o orcamento, mas falhou ao publicar no SQS.
- `PROCESSING`: orquestrador recebeu a mensagem e iniciou o processamento.
- `CALCULATION_REQUESTED`: orquestrador publicou a solicitacao para o calculator.
- `CALCULATED`: calculo concluido com sucesso.
- `FAILED`: falha definitiva no processamento do orcamento.

## Estrutura do projeto

```text
.
|-- budget-api
|-- budget-orchestrator
|-- budget-calculator
|-- notification-service
|-- budget-lambda
|-- localstack/init/01-sqs.sh
|-- docker-compose.yml
`-- pom.xml
```

## Requisitos

- Java 17
- Maven 3.9+
- Docker e Docker Compose para infraestrutura local

## Infra local

O `docker-compose.yml` sobe MongoDB e LocalStack. O MongoDB fica exposto na porta `27018` para evitar conflito com outra instancia local em `27017`.

Servicos locais:

- MongoDB: `localhost:27018`
- LocalStack: `localhost:4566`
- SQS no LocalStack: filas criadas por `localstack/init/01-sqs.sh`

```bash
docker compose up -d
```

Para conferir as filas criadas usando o LocalStack:

```bash
docker exec orcamento-localstack awslocal sqs list-queues
```

## Variaveis de ambiente

Os `application.yml` usam variaveis com fallback local. Isso permite rodar sem configuracao extra em desenvolvimento e trocar valores no deploy.

Variaveis comuns:

- `SPRING_APPLICATION_NAME`: nome da aplicacao Spring.
- `SERVER_PORT`: porta HTTP do servico, quando o modulo expuser servidor web.
- `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE`: endpoints do Actuator expostos.
- `AWS_REGION`: regiao AWS.
- `AWS_ENDPOINT`: endpoint customizado para LocalStack. Em ambiente AWS real, a configuracao do client deve evoluir para nao usar endpoint override.
- `AWS_ACCESS_KEY_ID`: access key usada pelo SDK.
- `AWS_SECRET_ACCESS_KEY`: secret key usada pelo SDK.
- `SQS_CONSUMER_ENABLED`: liga ou desliga consumers SQS.
- `SQS_MAX_MESSAGES`: maximo de mensagens lidas por polling.
- `SQS_WAIT_TIME_SECONDS`: tempo de long polling.
- `SQS_POLLING_DELAY_MS`: intervalo entre ciclos de polling.

Variaveis de banco:

- `MONGODB_URI`: URI de conexao com o MongoDB.

Variaveis de filas:

- `BUDGET_REQUESTS_QUEUE`
- `BUDGET_REQUESTS_DLQ`
- `BUDGET_CALCULATION_REQUESTS_QUEUE`
- `BUDGET_CALCULATION_REQUESTS_DLQ`
- `BUDGET_CALCULATION_RESULTS_QUEUE`
- `BUDGET_CALCULATION_RESULTS_DLQ`

Variaveis do `docker-compose.yml`:

- `MONGODB_IMAGE`
- `MONGODB_CONTAINER_NAME`
- `MONGODB_PORT`
- `MONGO_INITDB_DATABASE`
- `MONGODB_VOLUME_NAME`
- `LOCALSTACK_IMAGE`
- `LOCALSTACK_CONTAINER_NAME`
- `LOCALSTACK_PORT`
- `LOCALSTACK_SERVICES`
- `AWS_DEFAULT_REGION`
- `LOCALSTACK_DEBUG`
- `LOCALSTACK_INIT_PATH`
- `DOCKER_SOCKET_PATH`

## Comandos

Executar todos os testes:

```bash
mvn clean test
```

Executar verificacao com relatorio JaCoCo:

```bash
mvn clean verify
```

Subir a API REST:

```bash
mvn -pl budget-api spring-boot:run
```

Subir o orquestrador:

```bash
mvn -pl budget-orchestrator spring-boot:run
```

Empacotar a Lambda:

```bash
mvn -pl budget-lambda package
```

## Testando o fluxo atual

Suba a infraestrutura local:

```bash
docker compose up -d
```

Suba a API:

```bash
mvn -pl budget-api spring-boot:run
```

Em outro terminal, suba o orquestrador:

```bash
mvn -pl budget-orchestrator spring-boot:run
```

Acesse o Swagger:

```text
http://localhost:8081/swagger-ui.html
```

Execute `POST /budgets` com um payload valido:

```json
{
  "customerName": "Ricardo Alves",
  "description": "Orcamento para teste no Swagger",
  "amount": 1500.00
}
```

Se a criacao funcionar, a API retorna `202 Accepted` com status `RECEIVED` e publica uma mensagem na fila `budget-requests`.

Com o `budget-orchestrator` rodando, ele consome a mensagem de `budget-requests`, atualiza o status para `PROCESSING`, publica uma mensagem em `budget-calculation-requests` e atualiza o status para `CALCULATION_REQUESTED`.

Com o `budget-calculator` rodando, ele consome `budget-calculation-requests`, aplica a regra de calculo inicial e publica o resultado em `budget-calculation-results`. A regra atual aplica 10% de desconto para valores acima de `1000`.

Para ver a mensagem na fila sem instalar o AWS CLI localmente:

```bash
docker exec orcamento-localstack awslocal sqs receive-message --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/budget-requests
```

Para ver a mensagem enviada ao calculator:

```bash
docker exec orcamento-localstack awslocal sqs receive-message --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/budget-calculation-requests --visibility-timeout 5
```

Se esse comando nao retornar nada, verifique primeiro se o `budget-orchestrator` esta rodando. A API so publica em `budget-requests`; quem publica em `budget-calculation-requests` e o orquestrador.

Para ver o resultado publicado pelo calculator:

```bash
docker exec orcamento-localstack awslocal sqs receive-message --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/budget-calculation-results --visibility-timeout 5
```

Se quiser ver a mesma mensagem mais de uma vez durante testes, use um visibility timeout curto:

```bash
docker exec orcamento-localstack awslocal sqs receive-message --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/budget-requests --visibility-timeout 5
```

Para listar as filas criadas no LocalStack:

```bash
docker exec orcamento-localstack awslocal sqs list-queues
```

Para inspecionar mensagens que chegaram na DLQ:

```bash
docker exec orcamento-localstack awslocal sqs receive-message --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/budget-requests-dlq
```

Para inspecionar a DLQ do calculator:

```bash
docker exec orcamento-localstack awslocal sqs receive-message --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/budget-calculation-requests-dlq
```

Para inspecionar a DLQ de resultados do calculator:

```bash
docker exec orcamento-localstack awslocal sqs receive-message --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/budget-calculation-results-dlq
```

Para consultar a quantidade aproximada de mensagens na fila do calculator:

```bash
docker exec orcamento-localstack awslocal sqs get-queue-attributes --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/budget-calculation-requests --attribute-names ApproximateNumberOfMessages ApproximateNumberOfMessagesNotVisible
```

## Portas e endpoints

- `budget-api`: `http://localhost:8081`
- Actuator da API: `http://localhost:8081/actuator/health`
- Swagger UI da API, quando houver controllers mapeados: `http://localhost:8081/swagger-ui.html`
- LocalStack: `http://localhost:4566`
- MongoDB local do projeto: `localhost:27018`

## Tratamento de erros e idempotencia

Regras aplicadas ate agora:

- A API valida entrada invalida antes de salvar/publicar.
- A API trata erro de publicacao no SQS, marca o orcamento como `PUBLISH_FAILED` e responde erro controlado.
- O orquestrador so deleta a mensagem de `budget-requests` depois que o processamento local e a publicacao para `budget-calculation-requests` terminam com sucesso.
- O calculator so deleta a mensagem de `budget-calculation-requests` depois que o calculo e a publicacao para `budget-calculation-results` terminam com sucesso.
- O orquestrador so deleta a mensagem de `budget-calculation-results` depois que o MongoDB e atualizado com o resultado final.
- Se a mensagem de entrada estiver invalida, se o MongoDB falhar ou se alguma publicacao falhar, a mensagem nao e deletada; o SQS faz retry e depois move para DLQ.
- O status `CALCULATION_REQUESTED` evita publicacao duplicada para o calculator quando uma mensagem ja processada for entregue novamente.

## Direcionamento de arquitetura

```mermaid
flowchart TD
    Transport[Camada de entrada\nREST, SQS ou Lambda]
    Application[Camada de aplicacao\norquestracao de casos de uso]
    Domain[Camada de dominio\nregras de orcamento]
    Infra[Camada de infraestrutura\nAWS, filas, notificacoes]

    Transport --> Application
    Application --> Domain
    Application --> Infra
    Infra --> Application
```

Principios para evolucao dos modulos:

- Manter regra de negocio fora de controllers, consumidores SQS e handlers Lambda.
- Tratar SQS como contrato de integracao entre modulos, com mensagens versionaveis.
- Preferir processamento assincrono para etapas demoradas ou sujeitas a falhas externas.
- Isolar integracoes externas em adapters para preservar testes de dominio simples.
- Usar logs com identificador de correlacao para acompanhar uma solicitacao de ponta a ponta.

## Proximas etapas planejadas

- Adicionar SNS para publicar eventos finais do orcamento.
- Conectar SNS a `notification-service` via SQS.
- Conectar SNS a `budget-lambda` para auditoria obrigatoria do fluxo.
- Evoluir contratos compartilhados entre modulos para evitar duplicacao de DTOs e enums.
