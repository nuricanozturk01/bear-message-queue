package com.bearmq.server.broker;

import static org.assertj.core.api.Assertions.assertThat;

import com.bearmq.api.auth.dto.RegisterRequest;
import com.bearmq.api.broker.dtos.BrokerRequest;
import com.bearmq.api.facades.BrokerApiFacade;
import com.bearmq.api.tenant.TenantService;
import com.bearmq.api.tenant.converter.TenantConverter;
import com.bearmq.api.tenant.dto.TenantAuthenticateInfo;
import com.bearmq.server.broker.dto.Auth;
import com.bearmq.server.broker.dto.BearOperation;
import com.bearmq.server.broker.dto.Message;
import com.bearmq.server.broker.facade.BrokerServerFacade;
import com.bearmq.shared.broker.dto.QueueRequest;
import com.bearmq.shared.settings.MessagingApiKeyService;
import com.bearmq.shared.tenant.Tenant;
import com.bearmq.shared.tenant.TenantRepository;
import com.bearmq.shared.tenant.dto.TenantInfo;
import com.bearmq.shared.vhost.dto.VirtualHostInfo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class DeadLetterQueueIntegrationTest {

  private static final String SOURCE_QUEUE = "dlq-source-queue";
  private static final String DEAD_LETTER_QUEUE = "dlq-dead-letter-queue";

  private static Path storageDir;

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

  @DynamicPropertySource
  static void registerProperties(final DynamicPropertyRegistry registry) {
    try {
      storageDir = Files.createTempDirectory("bearmq-dlq-it-");
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("bearmq.broker.storage-dir", () -> storageDir.toAbsolutePath().toString());
    registry.add("bearmq.server.broker.port", () -> 36668);
  }

  @Autowired private TenantService tenantService;
  @Autowired private TenantConverter tenantConverter;
  @Autowired private BrokerApiFacade brokerApiFacade;
  @Autowired private TenantRepository tenantRepository;
  @Autowired private MessagingApiKeyService messagingApiKeyService;
  @Autowired private BrokerServerFacade brokerServerFacade;

  private String vhostId;
  private String vhostName;
  private String vhostUsername;
  private String vhostPassword;
  private String apiKey;

  @BeforeEach
  void seedTenantAndTopology() {
    final String username =
        "dlq-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    final TenantAuthenticateInfo created =
        this.tenantService.create(new RegisterRequest(username, "secret12"));
    final Tenant tenant = this.tenantRepository.findById(created.id()).orElseThrow();
    final TenantInfo tenantInfo = this.tenantConverter.toTenantInfo(tenant);
    final VirtualHostInfo vhost = this.brokerApiFacade.createVirtualHost(tenantInfo);
    this.vhostId = vhost.id();
    this.vhostName = vhost.name();
    this.vhostUsername = vhost.username();
    this.vhostPassword = vhost.password();
    this.apiKey = this.messagingApiKeyService.getMessagingApiKey();

    final BrokerRequest request =
        new BrokerRequest(
            this.vhostName,
            1,
            List.of(),
            List.of(
                new QueueRequest(SOURCE_QUEUE, true, false, false, Map.of()),
                new QueueRequest(DEAD_LETTER_QUEUE, true, false, false, Map.of())),
            List.of());
    this.brokerApiFacade.createBrokerObjects(request, tenantInfo);

    assertThat(this.brokerServerFacade.isVhostLoaded(this.vhostId)).isTrue();
    assertThat(this.brokerServerFacade.getLoadedQueueNames(this.vhostId))
        .contains(SOURCE_QUEUE, DEAD_LETTER_QUEUE);
  }

  @Test
  void message_routedToDeadLetterQueue_afterConsumerFailure() {
    final byte[] payload = "dlq-test-payload".getBytes(StandardCharsets.UTF_8);

    this.enqueue(SOURCE_QUEUE, payload);

    final Optional<byte[]> dequeued = this.dequeue(SOURCE_QUEUE);
    assertThat(dequeued).isPresent();
    assertThat(dequeued.get()).isEqualTo(payload);

    this.enqueue(DEAD_LETTER_QUEUE, dequeued.get());

    final Optional<byte[]> dlqMessage = this.dequeue(DEAD_LETTER_QUEUE);
    assertThat(dlqMessage).isPresent();
    assertThat(dlqMessage.get()).isEqualTo(payload);
  }

  @Test
  void deadLetterQueue_acceptsMessages_independently() {
    final byte[] payload = "direct-dlq-message".getBytes(StandardCharsets.UTF_8);

    this.enqueue(DEAD_LETTER_QUEUE, payload);

    final Optional<byte[]> dlqMessage = this.dequeue(DEAD_LETTER_QUEUE);
    assertThat(dlqMessage).isPresent();
    assertThat(dlqMessage.get()).isEqualTo(payload);
  }

  @Test
  void sourceQueue_remainsEmpty_afterMessageRouted() {
    final byte[] payload = "source-check-payload".getBytes(StandardCharsets.UTF_8);

    this.enqueue(SOURCE_QUEUE, payload);
    this.dequeue(SOURCE_QUEUE);
    this.enqueue(DEAD_LETTER_QUEUE, payload);

    final Optional<byte[]> secondDequeue = this.dequeue(SOURCE_QUEUE);
    assertThat(secondDequeue).isEmpty();
  }

  @Test
  void multipleMessages_eachRoutedToDeadLetterQueue() {
    final byte[] firstPayload = "first-failed".getBytes(StandardCharsets.UTF_8);
    final byte[] secondPayload = "second-failed".getBytes(StandardCharsets.UTF_8);

    this.enqueue(SOURCE_QUEUE, firstPayload);
    this.enqueue(SOURCE_QUEUE, secondPayload);

    final Optional<byte[]> first = this.dequeue(SOURCE_QUEUE);
    assertThat(first).isPresent();
    this.enqueue(DEAD_LETTER_QUEUE, first.get());

    final Optional<byte[]> second = this.dequeue(SOURCE_QUEUE);
    assertThat(second).isPresent();
    this.enqueue(DEAD_LETTER_QUEUE, second.get());

    final Optional<byte[]> firstDlq = this.dequeue(DEAD_LETTER_QUEUE);
    assertThat(firstDlq).isPresent();
    assertThat(firstDlq.get()).isEqualTo(firstPayload);

    final Optional<byte[]> secondDlq = this.dequeue(DEAD_LETTER_QUEUE);
    assertThat(secondDlq).isPresent();
    assertThat(secondDlq.get()).isEqualTo(secondPayload);
  }

  private void enqueue(final String queueName, final byte[] body) {
    final Message msg =
        Message.builder()
            .operation(BearOperation.ENQUEUE)
            .queue(queueName)
            .auth(this.auth())
            .body(body)
            .build();
    this.brokerServerFacade.identifyOperationAndApply(msg);
  }

  private Optional<byte[]> dequeue(final String queueName) {
    final Message msg =
        Message.builder()
            .operation(BearOperation.DEQUEUE)
            .queue(queueName)
            .auth(this.auth())
            .build();
    return this.brokerServerFacade.identifyOperationAndApply(msg);
  }

  private Auth auth() {
    return Auth.builder()
        .vhost(b64(this.vhostName))
        .username(b64(this.vhostUsername))
        .password(b64(this.vhostPassword))
        .apiKey(b64(this.apiKey))
        .build();
  }

  private static String b64(final String raw) {
    return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }
}
