package com.bearmq.shared.queue;

import static java.util.Locale.ROOT;
import static org.apache.commons.lang3.RandomStringUtils.secure;

import com.bearmq.shared.broker.Status;
import com.bearmq.shared.broker.dto.QueueRequest;
import com.bearmq.shared.converter.BrokerConverter;
import com.bearmq.shared.vhost.VirtualHost;
import com.github.f4b6a3.ulid.UlidCreator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QueueService {

  private static final int DEFAULT_MESSAGE_SIZE = 1024;
  private static final int MIN_DIGITS = 8;
  private static final int MAX_DIGITS = 30;

  private final QueueRepository queueRepository;
  private final BrokerConverter brokerConverter;
  private final Random random;

  public List<Queue> createAll(final VirtualHost vhost, final List<QueueRequest> queues) {

    final List<Queue> queueObjects = queues.stream().map(this.brokerConverter::toQueue).toList();

    final Set<String> existingNames =
        this.queueRepository.findAllByVhostId(vhost.getId()).stream()
            .map(Queue::getName)
            .collect(Collectors.toSet());

    for (final Queue queue : queueObjects) {
      if (existingNames.contains(queue.getName())) {
        continue;
      }

      final String actualName =
          String.format(
              "queue-%s",
              secure()
                  .next(this.random.nextInt(MIN_DIGITS, MAX_DIGITS), true, false)
                  .toLowerCase(ROOT));
      queue.setId(UlidCreator.getUlid().toString());
      queue.setVhost(vhost);
      queue.setActualName(actualName);
      queue.setStatus(Status.ACTIVE);
      queue.setOverflowPolicy(OverflowPolicy.DEAD_LETTER_QUEUE);
      queue.setMaxMessage(DEFAULT_MESSAGE_SIZE);
    }

    return this.queueRepository.saveAll(
        queueObjects.stream().filter(q -> !existingNames.contains(q.getName())).toList());
  }

  public List<Queue> findAllByVhostId(final String vhostId) {

    return this.queueRepository.findAllByVhostId(vhostId);
  }

  @Transactional
  public void softDeleteById(final String vhostId, final String queueId) {

    final Queue q =
        this.queueRepository
            .findById(queueId)
            .orElseThrow(() -> new IllegalArgumentException("Queue not found"));
    if (q.getVhost() == null || !vhostId.equals(q.getVhost().getId())) {
      throw new IllegalArgumentException("Queue not in this virtual host");
    }
    q.setDeleted(true);
    this.queueRepository.save(q);
  }
}
