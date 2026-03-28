package com.bearmq.shared.binding;

import com.bearmq.shared.broker.Status;
import com.bearmq.shared.exchange.Exchange;
import com.bearmq.shared.queue.Queue;
import com.bearmq.shared.vhost.VirtualHost;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "binding")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public final class Binding {
  @Id
  @Column(nullable = false, length = 26, unique = true)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vhost_id", nullable = false)
  private VirtualHost vhost;

  @Column(name = "source_exchange_id", nullable = false, length = 26)
  private String sourceExchangeId;

  @Column(name = "destination_queue_id", length = 26)
  private String destinationQueueId;

  @Column(name = "destination_exchange_id", length = 26)
  private String destinationExchangeId;

  @Enumerated(EnumType.STRING)
  @Column(name = "destination_type", nullable = false, length = 16)
  private DestinationType destinationType;

  @Column(name = "routing_key")
  private String routingKey;

  @Type(JsonType.class)
  @Column(columnDefinition = "json")
  private JsonNode arguments;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Status status;

  @Column(nullable = false)
  private long version;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(nullable = false)
  private boolean deleted;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vhost_id", insertable = false, updatable = false)
  private VirtualHost vhostRef;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "source_exchange_id",
      referencedColumnName = "id",
      insertable = false,
      updatable = false)
  private Exchange sourceExchangeRef;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "destination_queue_id",
      referencedColumnName = "id",
      insertable = false,
      updatable = false)
  private Queue destinationQueueRef;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "destination_exchange_id",
      referencedColumnName = "id",
      insertable = false,
      updatable = false)
  private Exchange destinationExchangeRef;

  @Override
  public boolean equals(final Object o) {

    if (!(o instanceof final Binding b)) {
      return false;
    }

    return Objects.equals(this.id, b.id);
  }

  @Override
  public int hashCode() {

    return Objects.hash(this.id);
  }
}
