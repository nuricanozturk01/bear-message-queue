package com.bearmq.server.broker.facade;

import static com.bearmq.shared.binding.DestinationType.EXCHANGE;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;

import com.bearmq.server.broker.dto.Auth;
import com.bearmq.server.broker.dto.Message;
import com.bearmq.shared.binding.Binding;
import com.bearmq.shared.binding.BindingService;
import com.bearmq.shared.broker.Status;
import com.bearmq.shared.broker.runtime.BrokerRuntimePort;
import com.bearmq.shared.queue.Queue;
import com.bearmq.shared.queue.QueueService;
import com.bearmq.shared.settings.MessagingApiKeyService;
import com.bearmq.shared.vhost.VirtualHost;
import com.bearmq.shared.vhost.VirtualHostService;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@NamedInterface
@SuppressWarnings("resource")
public class BrokerServerFacade implements BrokerRuntimePort {

  private final VirtualHostService virtualHostService;
  private final QueueService queueService;
  private final BindingService bindingService;
  private final MessagingApiKeyService messagingApiKeyService;
  private final ExecutorService virtualThreadPool;
  private final String storageDir;
  private final int dequeueWaitMs;
  private final Map<String, ChronicleQueue> queueCache = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> routes = new ConcurrentHashMap<>();
  private final Map<String, ExcerptTailer> consumerTailers = new ConcurrentHashMap<>();
  private final Map<String, ReentrantLock> queueLocks = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> exchangeToExchanges = new ConcurrentHashMap<>();

  public BrokerServerFacade(
      final VirtualHostService virtualHostService,
      final QueueService queueService,
      final BindingService bindingService,
      final MessagingApiKeyService messagingApiKeyService,
      @Qualifier("thread.virtual") final ExecutorService virtualThreadPool,
      @Value("${bearmq.broker.storage-dir:./data/queues}") final String storageDir,
      @Value("${bearmq.server.broker.dequeue-wait-ms:500}") final int dequeueWaitMs) {
    this.virtualHostService = virtualHostService;
    this.queueService = queueService;
    this.bindingService = bindingService;
    this.messagingApiKeyService = messagingApiKeyService;
    this.virtualThreadPool = virtualThreadPool;
    this.storageDir = storageDir;
    this.dequeueWaitMs = dequeueWaitMs;
  }

  public void loadQueues() {

    final List<VirtualHost> vhosts = this.virtualHostService.findAll();

    for (final VirtualHost vhost : vhosts) {
      if (vhost.isDeleted() || vhost.getStatus() != Status.ACTIVE) {
        continue;
      }
      final List<Queue> queues =
          this.queueService.findAllByVhostId(vhost.getId()).stream()
              .filter(q -> !q.isDeleted())
              .toList();
      final List<Binding> bindings =
          this.bindingService.findAllByVhostId(vhost.getId()).stream()
              .filter(b -> !b.isDeleted())
              .toList();

      this.prepareAndUpQueues(vhost, queues, bindings);
    }
  }

  @Override
  public void reloadVhostRuntime(final String vhostId) {

    this.unloadVhost(vhostId);
    this.loadVhostRuntime(vhostId);
  }

  public void loadVhostRuntime(final String vhostId) {

    final VirtualHost vhost = this.virtualHostService.requireEntityById(vhostId);
    if (vhost.getStatus() != Status.ACTIVE) {
      return;
    }
    final List<Queue> queues =
        this.queueService.findAllByVhostId(vhostId).stream().filter(q -> !q.isDeleted()).toList();
    final List<Binding> bindings =
        this.bindingService.findAllByVhostId(vhostId).stream().filter(b -> !b.isDeleted()).toList();
    this.prepareAndUpQueues(vhost, queues, bindings);
  }

  public void prepareAndUpQueues(
      final VirtualHost vhost, final List<Queue> queues, final List<Binding> bindings) {
    final String vhostId = vhost.getId();

    this.prepareQueues(vhostId, queues);

    for (final Binding binding : bindings) {
      if (binding.isDeleted()) {
        continue;
      }
      final String sourceExchangeName = binding.getSourceExchangeRef().getName();
      final String sourceExchangeKey = this.exchangeKey(vhostId, sourceExchangeName);

      if (binding.getDestinationType() == EXCHANGE) {
        final String destinationExchangeName = binding.getDestinationExchangeRef().getName();
        this.bindExchangeToExchange(vhostId, destinationExchangeName, sourceExchangeKey);
        continue;
      }

      final String queueName = binding.getDestinationQueueRef().getName();
      this.routes.computeIfAbsent(sourceExchangeKey, k -> newKeySet()).add(queueName);
    }

    log.info("Queues opened: {}", this.queueCache.keySet());
    log.info("Routes prepared: {}", this.routes.keySet());
  }

  public Optional<byte[]> identifyOperationAndApply(final Message msg) {

    final VirtualHost vhost = this.getVhost(msg);

    return switch (msg.getOperation()) {
      case ENQUEUE -> this.enqueue(vhost, msg);
      case PUBLISH -> this.publish(vhost, msg);
      case DEQUEUE -> this.dequeue(vhost, msg);
    };
  }

  @Override
  public boolean isVhostLoaded(final String vhostId) {

    final String prefix = vhostId + ":";
    return this.queueCache.keySet().stream().anyMatch(k -> k.startsWith(prefix));
  }

  @Override
  public Set<String> getLoadedQueueNames(final String vhostId) {

    final String prefix = vhostId + ":";
    final int prefixLen = prefix.length();
    final Set<String> names = ConcurrentHashMap.newKeySet();
    for (final String key : this.queueCache.keySet()) {
      if (key.startsWith(prefix)) {
        names.add(key.substring(prefixLen));
      }
    }
    return names;
  }

  public void unloadVhost(final String vhostId) {

    final String prefix = vhostId + ":";
    this.queueCache
        .entrySet()
        .removeIf(
            e -> {
              if (e.getKey().startsWith(prefix)) {
                try {
                  e.getValue().close();
                } catch (final Throwable ignore) {
                }
                return true;
              }
              return false;
            });
    this.consumerTailers.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
    this.queueLocks.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
    this.routes.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
    this.exchangeToExchanges.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
    for (final Set<String> destinations : this.exchangeToExchanges.values()) {
      destinations.removeIf(d -> d.startsWith(prefix));
    }
    log.info("Broker runtime state unloaded for vhostId={}", vhostId);
  }

  private Optional<byte[]> enqueue(final VirtualHost vhost, final Message msg) {

    final String queueName = msg.getQueue();
    final String key = this.queueKey(vhost.getId(), queueName);
    final ChronicleQueue chronicleQueue = this.queueCache.get(key);

    if (chronicleQueue == null) {
      throw new IllegalArgumentException("Queue not found: " + queueName);
    }

    final byte[] body = msg.getBody();
    if (body.length == 0) {
      throw new IllegalArgumentException("Missing body");
    }

    try (final ExcerptAppender appender = chronicleQueue.createAppender()) {
      appender.writeBytes(bytes -> bytes.write(body));
    }

    return Optional.empty();
  }

  private Optional<byte[]> publish(final VirtualHost vhost, final Message msg) {

    final String exchangeName = msg.getExchange();
    final String key = this.exchangeKey(vhost.getId(), exchangeName);

    final Set<String> queueNames = this.resolveQueuesFor(key);

    if (queueNames.isEmpty()) {
      return Optional.empty();
    }

    final byte[] body = msg.getBody();
    if (body.length == 0) {
      throw new IllegalArgumentException("Missing body");
    }

    for (final String queueName : queueNames) {
      final ChronicleQueue chronicleQueue =
          this.queueCache.get(this.queueKey(vhost.getId(), queueName));

      if (chronicleQueue == null) {
        log.warn("Queue missing for route: {}", queueName);
        continue;
      }

      try (final ExcerptAppender appender = chronicleQueue.createAppender()) {
        appender.writeBytes(bytes -> bytes.write(body));
      }
    }

    return Optional.empty();
  }

  private Optional<byte[]> dequeue(final VirtualHost vhost, final Message msg) {

    final String queueName = msg.getQueue();
    final String key = this.queueKey(vhost.getId(), queueName);

    final ChronicleQueue chronicleQueue = this.queueCache.get(key);
    if (chronicleQueue == null) {
      return Optional.empty();
    }

    final ReentrantLock queueLock = this.queueLocks.get(key);
    if (queueLock == null) {
      return Optional.empty();
    }

    final ExcerptTailer tailer =
        this.consumerTailers.computeIfAbsent(key, k -> chronicleQueue.createTailer().toStart());

    final Future<Optional<byte[]>> future =
        this.virtualThreadPool.submit(() -> this.readInQueue(queueLock, tailer));

    try {
      return future.get(this.dequeueWaitMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    } catch (final TimeoutException e) {
      future.cancel(true);
      return Optional.empty();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Optional<byte[]> readInQueue(final ReentrantLock lock, final ExcerptTailer tailer) {

    lock.lock();
    try {
      final AtomicReference<byte[]> responseBody = new AtomicReference<>();

      final boolean ok =
          tailer.readBytes(
              in -> {
                final byte[] buf = new byte[(int) in.readRemaining()];
                in.read(buf);
                responseBody.set(buf);
              });

      return ok && responseBody.get() != null ? Optional.of(responseBody.get()) : Optional.empty();
    } finally {
      lock.unlock();
    }
  }

  private Set<String> resolveQueuesFor(final String rootExKey) {

    final Set<String> visitedExchanges = ConcurrentHashMap.newKeySet();
    final Set<String> resultQueues = ConcurrentHashMap.newKeySet();
    final ArrayDeque<String> q = new ArrayDeque<>();
    q.add(rootExKey);

    while (!q.isEmpty()) {
      final String ex = q.poll();

      if (!visitedExchanges.add(ex)) {
        continue;
      }

      final Set<String> qs = this.routes.getOrDefault(ex, Set.of());
      resultQueues.addAll(qs);

      final Set<String> next = this.exchangeToExchanges.getOrDefault(ex, Set.of());
      for (final String nx : next) {
        if (!visitedExchanges.contains(nx)) {
          q.add(nx);
        }
      }
    }
    return resultQueues;
  }

  private void prepareQueues(final String vhostId, final List<Queue> queues) {

    for (final Queue queue : queues) {
      if (queue.isDeleted()) {
        continue;
      }
      final String key = this.queueKey(vhostId, queue.getName());

      this.queueCache.computeIfAbsent(key, k -> this.openChronicle(this.resolveQueuePath(queue)));
      this.queueLocks.computeIfAbsent(key, k -> new ReentrantLock(true));
    }
  }

  private void bindExchangeToExchange(
      final String vhostId, final String destExchangeName, final String srcExchangeKey) {
    final String destinationExchangeKey = this.exchangeKey(vhostId, destExchangeName);

    if (!this.isCreateCycle(srcExchangeKey, destinationExchangeKey)) {
      this.exchangeToExchanges
          .computeIfAbsent(srcExchangeKey, k -> newKeySet())
          .add(destinationExchangeKey);
    }
  }

  private boolean isCreateCycle(final String srcExchangeKey, final String destExchangeKey) {

    if (srcExchangeKey.equals(destExchangeKey)) {
      return true;
    }
    final Set<String> visited = ConcurrentHashMap.newKeySet();
    final ArrayDeque<String> queue = new ArrayDeque<>();
    queue.add(destExchangeKey);

    while (!queue.isEmpty()) {
      final String exchangeKey = queue.poll();
      if (!visited.add(exchangeKey)) {
        continue;
      }

      if (exchangeKey.equals(srcExchangeKey)) {
        return true;
      }

      final Set<String> nextExchanges =
          this.exchangeToExchanges.getOrDefault(exchangeKey, Set.of());
      queue.addAll(nextExchanges);
    }

    return false;
  }

  private ChronicleQueue openChronicle(final Path dir) {

    try {
      Files.createDirectories(dir);
    } catch (final Exception e) {
      log.error("Failed to create chronicle queue.", e);
      throw new IllegalStateException(e);
    }

    return ChronicleQueue.singleBuilder(dir.toFile()).build();
  }

  private VirtualHost getVhost(final Message msg) {

    final Auth auth = msg.getAuth();

    final String apiKey = this.decodeBase64(auth.getApiKey());
    if (!this.messagingApiKeyService.matchesMessagingApiKey(apiKey)) {
      throw new RuntimeException("Invalid API key");
    }

    final String host = this.decodeBase64(auth.getVhost());
    final String username = this.decodeBase64(auth.getUsername());

    final VirtualHost vhost =
        this.virtualHostService.findByVhostInfo(host, username, auth.getPassword());
    if (vhost.getStatus() != Status.ACTIVE) {
      throw new IllegalStateException("Virtual host is not active");
    }
    return vhost;
  }

  private String decodeBase64(final String val) {

    return new String(Base64.getDecoder().decode(val), StandardCharsets.UTF_8);
  }

  private Path resolveQueuePath(final Queue q) {

    return Path.of(
        this.storageDir + File.separator + q.getVhost().getId() + File.separator + q.getName());
  }

  private String queueKey(final String vhostId, final String queueName) {

    return String.format("%s:%s", vhostId, queueName);
  }

  private String exchangeKey(final String vhostId, final String exchangeName) {

    return String.format("%s:%s", vhostId, exchangeName);
  }

  @PreDestroy
  public void shutdown() {

    this.queueCache
        .values()
        .forEach(
            cq -> {
              try {
                cq.close();
              } catch (final Throwable ignore) {
              }
            });
    this.consumerTailers.clear();
    this.routes.clear();
    this.exchangeToExchanges.clear();
    this.queueCache.clear();
    this.virtualThreadPool.shutdown();
  }
}
