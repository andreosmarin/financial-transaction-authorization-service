package com.osmarin.financial.transaction.authorization;

import com.osmarin.financial.transaction.authorization.domain.enums.AccountStatus;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.output.persistence.AccountEntity;
import com.osmarin.financial.transaction.authorization.infrastructure.adapters.output.persistence.AccountJpaRepository;
import io.awspring.cloud.sqs.listener.MessageListenerContainerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "management.tracing.export.otlp.enabled=false",
        "management.otlp.metrics.export.enabled=false"
})
class AccountCreatedFlowE2ETest {

    private static final UUID ACCOUNT_ID = UUID.fromString("a74096e2-d897-4c7d-92c2-59092b79c943");
    private static final UUID OWNER_ID = UUID.fromString("31fb61f8-dde5-456a-9062-5b92af091bd7");
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17-alpine")
    );
    private static final LocalStackContainer LOCALSTACK = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:4.8.1")
    ).withServices("sqs");
    private static final SqsClient SQS;
    private static final String QUEUE_URL;

    static {
        POSTGRES.start();
        LOCALSTACK.start();
        SQS = SqsClient.builder()
                .endpointOverride(LOCALSTACK.getEndpoint())
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey()
                )))
                .build();
        QUEUE_URL = SQS.createQueue(CreateQueueRequest.builder()
                        .queueName("conta-bancaria-criada")
                        .build())
                .queueUrl();
    }

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.cloud.aws.endpoint", LOCALSTACK::getEndpoint);
        registry.add("spring.cloud.aws.credentials.access-key", LOCALSTACK::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", LOCALSTACK::getSecretKey);
        registry.add("spring.cloud.aws.region.static", LOCALSTACK::getRegion);
        registry.add("application.messaging.sqs.account-created-queue", () -> QUEUE_URL);
    }

    @AfterAll
    static void stopContainers() {
        SQS.close();
        LOCALSTACK.stop();
        POSTGRES.stop();
    }

    @Autowired
    private AccountJpaRepository repository;

    @Autowired
    private MessageListenerContainerRegistry listenerContainerRegistry;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @AfterEach
    void stopListenerBeforeInfrastructure() {
        listenerContainerRegistry.stop();
    }

    @Test
    void shouldConsumeAccountCreatedEventAndPersistItIdempotently() {
        String message = """
                {
                  "account": {
                    "id": "%s",
                    "owner": "%s",
                    "created_at": "1783944000",
                    "status": "enabled"
                  }
                }
                """.formatted(ACCOUNT_ID, OWNER_ID);

        send(message);
        send(message);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(repository.count()).isOne();
            AccountEntity account = repository.findById(ACCOUNT_ID).orElseThrow();
            assertThat(account.getOwnerId()).isEqualTo(OWNER_ID);
            assertThat(account.getBalance()).isEqualByComparingTo("0.00");
            assertThat(account.getCurrency()).isEqualTo("BRL");
            assertThat(account.getStatus()).isEqualTo(AccountStatus.ENABLED);
            assertThat(account.getOpenedAt()).isEqualTo(Instant.ofEpochSecond(1783944000));
        });
    }

    private void send(String body) {
        SQS.sendMessage(SendMessageRequest.builder()
                .queueUrl(QUEUE_URL)
                .messageBody(body)
                .build());
    }
}
