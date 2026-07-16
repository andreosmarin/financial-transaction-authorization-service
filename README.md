# Financial Transaction Authorization Service

Serviço de autorização de crédito e débito construído com Java 21, Spring Boot, PostgreSQL e AWS SQS, organizado em DDD
e arquitetura hexagonal. Créditos incrementam o saldo; débitos sem fundos são registrados como `FAILED` sem alterar a
conta.

## Pré-requisitos

- Java 21;
- Docker com Docker Compose;
- portas locais `3000`, `4317`, `4318`, `4566` e `5432` disponíveis.

## Executar localmente

Suba PostgreSQL, LocalStack/SQS e a stack de observabilidade:

```bash
docker compose up -d postgres localstack grafana-lgtm
docker compose ps
```

Inicie a aplicação:

```bash
./mvnw spring-boot:run
```

Endpoints úteis:

- API: `http://localhost:8080/api/transaction-authorization/authorize`;
- Swagger UI: `http://localhost:8080/swagger-ui.html`;
- readiness: `http://localhost:8080/actuator/health/readiness`;
- Prometheus: `http://localhost:8080/actuator/prometheus`;
- Grafana local: `http://localhost:3000`.

Crie uma conta enviando o evento de abertura:

```bash
docker compose exec localstack awslocal sqs send-message \
  --queue-url http://sqs.sa-east-1.localhost.localstack.cloud:4566/000000000000/conta-bancaria-criada \
  --message-body '{"account":{"id":"5b19c8b6-0cc4-4c72-a989-0c2ee15fa975","owner":"31fb61f8-dde5-456a-9062-5b92af091bd7","created_at":"1751972275","status":"ENABLED"}}'
```

Autorize um crédito:

```bash
curl --fail-with-body \
  -H 'Content-Type: application/json' \
  -d '{"accountId":"5b19c8b6-0cc4-4c72-a989-0c2ee15fa975","type":"CREDIT","amount":{"value":97.07,"currency":"BRL"}}' \
  http://localhost:8080/api/transaction-authorization/authorize
```

Para encerrar e remover os volumes locais:

```bash
docker compose down -v
```

O gerador opcional de 100 mil contas fica isolado no profile `load` para não tornar o bootstrap comum pesado:

```bash
docker compose --profile load up message-generator
```

## Testes e build

```bash
./mvnw verify
docker build -t financial-transaction-authorization-service:local .
```

Os testes incluem domínio, aplicação, REST, mensageria, persistência com PostgreSQL real e fluxos E2E com PostgreSQL e
LocalStack. O `verify` também gera `target/site/jacoco/jacoco.xml` para o SonarQube.

## Configuração

O profile default é `local`. Em produção, defina `SPRING_PROFILES_ACTIVE=production` para usar a cadeia de credenciais
da workload em vez de credenciais estáticas do LocalStack. As principais variáveis são:

| Variável                                                 | Finalidade                     |
|----------------------------------------------------------|--------------------------------|
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | conexão PostgreSQL             |
| `DATABASE_MAXIMUM_POOL_SIZE`                             | limite do pool por instância   |
| `AWS_REGION`, `ACCOUNT_CREATED_QUEUE`                    | região e URL da fila SQS       |
| `OTEL_TRACES_ENDPOINT`, `OTEL_METRICS_ENDPOINT`          | exportação OTLP                |
| `TRACING_SAMPLING_PROBABILITY`                           | amostragem de traces           |
| `SERVER_MAX_THREADS`, `SQS_MAX_INFLIGHT_MESSAGES`        | limites locais de concorrência |

Segredos devem vir do secret manager da plataforma. O número de threads, conexões e mensagens em voo precisa ser
dimensionado com teste de carga e com os limites globais do banco, nunca multiplicado livremente pelo autoscaling.

## Documentação

- [Arquitetura, decisões e diagrama cloud](docs/architecture.md)
- [Production readiness e resiliência](docs/production-readiness.md)
- [CI/CD](docs/ci-cd.md)
- [Canary Release](docs/canary-deployment-proposal.md)

## Limitações relevantes antes de produção

A API ainda precisa de idempotência de requisição para impedir crédito duplicado quando o cliente repetir uma chamada
após timeout. Autenticação/autorização, infraestrutura como código e testes de carga também dependem do ambiente de
destino. Esses itens estão priorizados e detalhados na documentação de production readiness; não devem ser tratados como
concluídos apenas pela presença da pipeline e do container.
