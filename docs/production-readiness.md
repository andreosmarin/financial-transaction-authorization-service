# Production readiness

## Estado atual

| Capacidade | Estado | Evidência / próximo passo |
|---|---|---|
| Regra de saldo e atomicidade | implementado | `Money`, transação única, lock pessimista por conta e teste concorrente real |
| Idempotência do evento de conta | implementado | `INSERT ... ON CONFLICT DO NOTHING` |
| Idempotência da API | **P0 pendente** | exigir `Idempotency-Key`, persistir request hash e resposta |
| Observabilidade técnica | implementado | Actuator, Prometheus, OTLP metrics/traces e correlação de logs |
| Métrica de negócio | implementado | `financial.transaction.authorizations` por tipo/status, sem IDs |
| Container não-root | implementado | multi-stage JRE, UID 10001 e healthcheck |
| Dependências locais | implementado | Compose com PostgreSQL, LocalStack/SQS+DLQ e Grafana LGTM |
| CI e segurança | implementado | testes, JaCoCo, SonarQube, Dependency Review e CodeQL |
| Deploy progressivo | projetado | ECS/CodeDeploy canary; requer IaC e credenciais OIDC |
| Autenticação/autorização | **P0 pendente** | JWT/OAuth2 no API Gateway e autorização por cliente/conta |
| Teste de carga/soak | pendente | definir SLO/TPS e validar limites de threads, pool e lock |
| Disaster recovery | pendente | IaC, PITR, restore drill, RTO/RPO e runbook |

## Idempotência da autorização

Esse é o principal gap funcional para produção. A solução proposta:

1. exigir `Idempotency-Key` opaco por tentativa lógica;
2. armazenar key, hash canônico do request, estado `IN_PROGRESS/COMPLETED`, transaction ID e resposta;
3. criar constraint única por cliente + key;
4. mesma key e mesmo payload retorna exatamente a resposta persistida;
5. mesma key com payload diferente retorna `409 Conflict`;
6. concorrência na mesma key aguarda de forma limitada ou retorna `409/425` com `Retry-After`;
7. expirar registros apenas após a janela contratual, nunca antes do retry máximo do cliente.

Implementar isso antes de retries de API ou de banco. O UUID gerado pelo servidor não protege uma requisição cujo resultado foi perdido depois do commit.

## Observabilidade

### Logs

Logs são estruturados como `event=...`, incluem trace/span quando existentes e usam IDs somente para investigação. Valores monetários, owner ID, payloads, tokens e credenciais não são registrados. Eventos mínimos:

- início em `DEBUG` e conclusão em `INFO` da autorização;
- recusa de negócio com reason de baixa cardinalidade;
- rejeições HTTP sem ecoar payload;
- consumo concluído, duplicado e falho do SQS;
- mudanças de deployment e rollback ficam no plano de controle.

Em produção, prefira encoder JSON para eliminar parsing de texto e propague `traceparent` no HTTP e atributos do SQS.

### Métricas e alertas

- `financial.transaction.authorizations{type,status}`;
- `http.server.requests` por rota/status, p50/p95/p99;
- Hikari active/pending/timeout e tempo de aquisição;
- lock wait/deadlocks no PostgreSQL;
- SQS age of oldest message, visible messages e DLQ depth;
- tasks healthy, restart count, CPU, memória e GC;
- erro de exportação de telemetria.

IDs de conta/transação nunca são labels. Alertas devem observar burn rate do SLO em janelas curta e longa, além dos gates específicos do canary.

## SLOs iniciais para validação

Valores abaixo são hipóteses a validar com produto e teste de carga:

- disponibilidade mensal da autorização: 99,95%;
- latência p95 < 300 ms e p99 < 750 ms no serviço;
- erro técnico < 0,1%;
- lag p95 do evento de conta < 30 s;
- RPO do banco <= 5 min e RTO <= 30 min.

Recusa por saldo insuficiente não entra no erro técnico.

## Backpressure e capacidade

- limitar pool JDBC e threads por task; a soma dos pools deve ficar abaixo do limite efetivo do banco/RDS Proxy;
- limitar fila de aceitação do servidor e retornar erro rápido quando saturado;
- autoscaling da API por latência/RPS e do consumidor por idade/backlog, com teto explícito;
- ajustar `SQS_MAX_INFLIGHT_MESSAGES` ao tempo de processamento e ao pool JDBC;
- medir contas quentes separadamente em tracing/logs, sem criar labels de alta cardinalidade;
- testar pico, soak, falha de uma AZ, failover do writer, atraso do SQS e degradação do collector.

## Segurança

- TLS ponta a ponta; banco e SQS apenas em rede privada/VPC endpoints;
- JWT/OAuth2, escopos e vínculo entre cliente e conta;
- Secrets Manager + KMS e rotação, sem secrets no repositório;
- role IAM por workload e GitHub OIDC para deploy;
- WAF, rate limit e quotas por consumidor;
- imagem por digest, SBOM, scan, assinatura/attestation e política de admissão;
- detalhes do health endpoint ocultos em produção;
- retenção e acesso aos dados definidos por política regulatória.

## Estratégia de testes

Já coberto: crédito, débito, saldo insuficiente, saldo exato, conta inexistente/desabilitada, moeda divergente no domínio, precisão decimal, ausência de arredondamento silencioso, dois débitos concorrentes na mesma conta, valores inválidos, duplicidade do evento, timestamps, REST, SQS, JPA e E2E.

Antes do go-live, adicionar:

- teste de carga concorrente com muitas autorizações e distribuição realista de hot accounts;
- idempotência concorrente e replay após timeout/commit;
- property-based tests para sequências aleatórias de crédito/débito;
- contract tests do evento e OpenAPI;
- chaos tests de PostgreSQL, SQS, rede e OTLP;
- carga, stress e soak com distribuição realista, incluindo hot accounts;
- restore de backup e rollback de aplicação com migration expand/contract.

## Runbooks mínimos

Criar runbooks acionáveis para: aumento de 5xx/latência, pool JDBC esgotado, lock contention, DLQ crescendo, failover do banco, rollback de canary, telemetria ausente e reconciliação financeira. Cada runbook deve ter dashboard, limiar, comandos seguros, responsável, escalonamento e critério de encerramento.
