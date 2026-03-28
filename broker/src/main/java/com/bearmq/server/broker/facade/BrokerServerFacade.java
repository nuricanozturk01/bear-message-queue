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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
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

  /** Persisted Chronicle tailer index so dequeue does not replay after broker or vhost reload. */
  private static final String CONSUMER_INDEX_FILENAME = ".bearmq-consumer-index";

  private final VirtualHostService virtualHostService;
  private final QueueService queueService;
  private final BindingService bindingService;
  private final MessagingApiKeyService messagingApiKeyService;
  private final ExecutorService virtualThreadPool;
  private final String storageDir;
  private final int dequeueWaitMs;
  private final Map<String, ChronicleQueue> queueCache = new ConcurrentHashMap<>();
  private final Map<String, Set<String>> routes = new ConcurrentHashMap<>();
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
    this.queueLocks.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
    this.routes.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
    this.exchangeToExchanges.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
    for (final Set<String> destinations : this.exchangeToExchanges.values()) {
      destinations.removeIf(d -> d.startsWith(prefix));
    }
    log.info("Broker runtime state unloaded for vhostId={}", vhostId);
  }

  /**
   * Deletes Chronicle queue directories and consumer index files under {@code storageDir/vhostId}.
   */
  public void purgeVhostStorage(final String vhostId) {

    final Path root = Path.of(this.storageDir + File.separator + vhostId);
    if (!Files.isDirectory(root)) {
      return;
    }
    try (Stream<Path> walk = Files.walk(root)) {
      walk.sorted(Comparator.reverseOrder()).forEach(this::deletePathQuietly);
    } catch (final IOException e) {
      log.warn("BearMQ: failed to purge on-disk queue storage for vhostId={}", vhostId, e);
    }
  }

  private void deletePathQuietly(final Path path) {

    try {
      Files.deleteIfExists(path);
    } catch (final IOException e) {
      log.debug("BearMQ: could not delete {}", path, e);
    }
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
    final Path queueDir = this.queueDataDir(vhost.getId(), queueName);

    final ChronicleQueue chronicleQueue = this.queueCache.get(key);
    if (chronicleQueue == null) {
      return Optional.empty();
    }

    final ReentrantLock queueLock = this.queueLocks.get(key);
    if (queueLock == null) {
      return Optional.empty();
    }

    /*
     * Chronicle ExcerptTailer is single-threaded: it must be created, positioned, read, and closed
     * on the same thread. The previous pattern (tailer from computeIfAbsent on the TCP thread +
     * readInQueue on a virtual thread) triggers ThreadingIllegalStateException on newer Chronicle.
     */
    final int budgetMs = Math.max(0, this.dequeueWaitMs);
    final Future<Optional<byte[]>> future =
        this.virtualThreadPool.submit(
            () -> this.dequeueExclusive(chronicleQueue, queueLock, queueDir, budgetMs));

    try {
      return future.get(budgetMs + 250L, TimeUnit.MILLISECONDS);
    } catch (final TimeoutException e) {
      future.cancel(true);
      return Optional.empty();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Long-poll up to {@code waitBudgetMs}: try read under queue lock, then release lock and park
   * before retrying. Empty queues no longer return instantly (avoids tight client poll loops). Lock
   * is never held during {@link LockSupport#parkNanos} so producers are not blocked.
   */
  private Optional<byte[]> dequeueExclusive(
      final ChronicleQueue chronicleQueue,
      final ReentrantLock lock,
      final Path queueDir,
      final int waitBudgetMs) {

    final long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(waitBudgetMs);
    final long parkChunkNanos = TimeUnit.MILLISECONDS.toNanos(50L);

    while (System.nanoTime() < deadlineNanos) {
      if (Thread.currentThread().isInterrupted()) {
        return Optional.empty();
      }

      lock.lock();
      final ExcerptTailer tailer = chronicleQueue.createTailer();
      try {
        this.restoreTailerPosition(tailer, queueDir);
        final AtomicReference<byte[]> responseBody = new AtomicReference<>();

        final boolean ok =
            tailer.readBytes(
                in -> {
                  final byte[] buf = new byte[(int) in.readRemaining()];
                  in.read(buf);
                  responseBody.set(buf);
                });

        if (ok && responseBody.get() != null) {
          this.persistConsumerIndexAfterSuccessfulRead(chronicleQueue, tailer, queueDir);
          return Optional.of(responseBody.get());
        }
      } finally {
        try {
          tailer.close();
        } catch (final Throwable ignore) {
        }
        lock.unlock();
      }

      final long remaining = deadlineNanos - System.nanoTime();
      if (remaining <= 0) {
        break;
      }
      LockSupport.parkNanos(Math.min(parkChunkNanos, remaining));
    }

    return Optional.empty();
  }

  private Path queueDataDir(final String vhostId, final String queueName) {

    return Path.of(this.storageDir + File.separator + vhostId + File.separator + queueName);
  }

  private void restoreTailerPosition(final ExcerptTailer tailer, final Path queueDir) {

    try {
      final OptionalLong idx = this.readConsumerIndex(queueDir);
      if (idx.isEmpty()) {
        tailer.toStart();
        return;
      }
      /*
       * After reading the last excerpt, a persisted tailer.index() often points at the *next* slot,
       * which does not exist yet — moveToIndex then correctly returns false (Chronicle #683).
       * Use toEnd() for that case, not toStart() (which would redeliver the whole queue).
       */
      tailer.toStart();
      if (!tailer.moveToIndex(idx.getAsLong())) {
        tailer.toEnd();
        this.persistConsumerIndex(queueDir, tailer.index());
        log.debug("BearMQ: consumer offset normalized for {} (EOF or stale index)", queueDir);
      }
    } catch (final Exception e) {
      log.warn("BearMQ: could not restore consumer index for {}, reading from start", queueDir, e);
      tailer.toStart();
      this.persistConsumerIndex(queueDir, tailer.index());
    }
  }

  /**
   * Never persist a "next" index that does not exist yet (typical after consuming the last
   * message), or every restart/long-poll will fail moveToIndex and spam logs.
   */
  private void persistConsumerIndexAfterSuccessfulRead(
      final ChronicleQueue queue, final ExcerptTailer tailer, final Path queueDir) {

    long next = tailer.index();
    final ExcerptTailer probe = queue.createTailer();
    try {
      probe.toStart();
      if (!probe.moveToIndex(next)) {
        tailer.toEnd();
        next = tailer.index();
      }
    } finally {
      try {
        probe.close();
      } catch (final Throwable ignore) {
      }
    }
    this.persistConsumerIndex(queueDir, next);
  }

  private OptionalLong readConsumerIndex(final Path queueDir) {

    final Path file = queueDir.resolve(CONSUMER_INDEX_FILENAME);
    if (!Files.isRegularFile(file)) {
      return OptionalLong.empty();
    }
    try {
      final String s = Files.readString(file, StandardCharsets.UTF_8).trim();
      if (s.isEmpty()) {
        return OptionalLong.empty();
      }
      return OptionalLong.of(Long.parseLong(s));
    } catch (final Exception e) {
      log.debug("BearMQ: no valid consumer index at {}", file, e);
      return OptionalLong.empty();
    }
  }

  private void persistConsumerIndex(final Path queueDir, final long index) {

    try {
      Files.createDirectories(queueDir);
      Files.writeString(
          queueDir.resolve(CONSUMER_INDEX_FILENAME),
          Long.toString(index),
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING,
          StandardOpenOption.SYNC);
    } catch (final Exception e) {
      log.error(
          "BearMQ: failed to persist consumer index for {} (restart may redeliver)", queueDir, e);
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
    this.routes.clear();
    this.exchangeToExchanges.clear();
    this.queueCache.clear();
    this.virtualThreadPool.shutdown();
  }
}
