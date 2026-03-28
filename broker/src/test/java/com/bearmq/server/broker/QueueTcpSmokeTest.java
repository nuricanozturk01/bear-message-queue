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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
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
import org.springframework.beans.factory.annotation.Value;
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
class QueueTcpSmokeTest {

  private static final int CHUNK = 4096;
  private static final String QUEUE_NAME = "smoke-queue";

  private static Path storageDir;

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

  @DynamicPropertySource
  static void registerProperties(final DynamicPropertyRegistry registry) {
    try {
      storageDir = Files.createTempDirectory("bearmq-smoke-");
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("bearmq.broker.storage-dir", () -> storageDir.toAbsolutePath().toString());
    registry.add("bearmq.server.broker.port", () -> 36667);
  }

  @Autowired private TenantService tenantService;
  @Autowired private TenantConverter tenantConverter;
  @Autowired private BrokerApiFacade brokerApiFacade;
  @Autowired private TenantRepository tenantRepository;
  @Autowired private MessagingApiKeyService messagingApiKeyService;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private BrokerServerFacade brokerServerFacade;

  @Value("${bearmq.server.broker.port}")
  private int brokerTcpPort;

  private TenantInfo tenantInfo;
  private String vhostId;
  private String vhostName;
  private String vhostUsername;
  private String vhostPassword;
  private String apiKey;

  @BeforeEach
  void seedTenantAndTopology() {
    final String username =
        "smoke-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    final TenantAuthenticateInfo created =
        this.tenantService.create(new RegisterRequest(username, "secret12"));
    final Tenant tenant = this.tenantRepository.findById(created.id()).orElseThrow();
    this.tenantInfo = this.tenantConverter.toTenantInfo(tenant);
    final VirtualHostInfo vhost = this.brokerApiFacade.createVirtualHost(this.tenantInfo);
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
            List.of(new QueueRequest(QUEUE_NAME, true, false, false, Map.of())),
            List.of());
    this.brokerApiFacade.createBrokerObjects(request, this.tenantInfo);
    assertThat(this.brokerServerFacade.isVhostLoaded(this.vhostId))
        .as("TCP broker runtime should expose the queue after topology apply")
        .isTrue();
    assertThat(this.brokerServerFacade.getLoadedQueueNames(this.vhostId)).contains(QUEUE_NAME);
  }

  @Test
  void enqueueThenDequeue_roundTripsPayloadOverTcp() throws Exception {
    final byte[] payload = "smoke-payload".getBytes(StandardCharsets.UTF_8);
    this.tcpEnqueue(payload);
    Optional<byte[]> received = Optional.empty();
    for (int i = 0; i < 80 && received.isEmpty(); i++) {
      received = this.tcpDequeue();
      if (received.isEmpty()) {
        Thread.sleep(50);
      }
    }
    assertThat(received).as("message should appear on TCP dequeue after enqueue").isPresent();
    assertThat(received.get()).isEqualTo(payload);
  }

  @Test
  void enqueueThenDequeue_sameProcessFacade_roundTripsPayload() {
    final byte[] payload = "facade-payload".getBytes(StandardCharsets.UTF_8);
    final Message enq =
        Message.builder()
            .operation(BearOperation.ENQUEUE)
            .queue(QUEUE_NAME)
            .auth(this.auth())
            .body(payload)
            .build();
    assertThat(this.brokerServerFacade.identifyOperationAndApply(enq)).isEmpty();
    final Message deq =
        Message.builder()
            .operation(BearOperation.DEQUEUE)
            .queue(QUEUE_NAME)
            .auth(this.auth())
            .build();
    final Optional<byte[]> out = this.brokerServerFacade.identifyOperationAndApply(deq);
    assertThat(out).isPresent();
    assertThat(out.get()).isEqualTo(payload);
  }

  private void tcpEnqueue(final byte[] body) throws IOException {

    final Message msg =
        Message.builder()
            .operation(BearOperation.ENQUEUE)
            .queue(QUEUE_NAME)
            .auth(this.auth())
            .body(body)
            .build();
    try (Socket socket = new Socket("127.0.0.1", this.brokerTcpPort)) {
      this.writeFrame(socket, msg);
    }
  }

  private Optional<byte[]> tcpDequeue() throws IOException {

    final Message msg =
        Message.builder()
            .operation(BearOperation.DEQUEUE)
            .queue(QUEUE_NAME)
            .auth(this.auth())
            .build();
    try (Socket socket = new Socket("127.0.0.1", this.brokerTcpPort)) {
      this.writeFrame(socket, msg);
      return this.readOptionalResponse(socket);
    }
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

  private void writeFrame(final Socket socket, final Message message) throws IOException {

    final byte[] bytes = this.objectMapper.writeValueAsBytes(message);
    final DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
    dos.writeInt(bytes.length);
    int offset = 0;
    int chunk = 0;
    while (offset < bytes.length) {
      final int chunkSize = Math.min(CHUNK, bytes.length - offset);
      dos.writeInt(++chunk);
      dos.write(bytes, offset, chunkSize);
      offset += chunkSize;
    }
    dos.flush();
  }

  private Optional<byte[]> readOptionalResponse(final Socket socket) throws IOException {

    try {
      final DataInputStream dis = new DataInputStream(socket.getInputStream());
      final int totalLen = dis.readInt();
      if (totalLen <= 0) {
        return Optional.empty();
      }
      final byte[] buf = new byte[totalLen];
      int offset = 0;
      while (offset < totalLen) {
        dis.readInt();
        final int chunkLen = Math.min(CHUNK, totalLen - offset);
        dis.readFully(buf, offset, chunkLen);
        offset += chunkLen;
      }
      return Optional.of(buf);
    } catch (final EOFException e) {
      return Optional.empty();
    }
  }
}
