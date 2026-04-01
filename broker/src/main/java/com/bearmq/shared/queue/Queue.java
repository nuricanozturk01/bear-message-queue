package com.bearmq.shared.queue;

import com.bearmq.shared.broker.Status;
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
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "queue")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public final class Queue {
  @Id
  @Column(nullable = false, length = 26, unique = true)
  private String id;

  @Column(nullable = false, length = 26)
  private String name;

  @Column(name = "actual_name", nullable = false, unique = true)
  private String actualName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vhost_id", nullable = false)
  private VirtualHost vhost;

  @Column(nullable = false)
  @ColumnDefault("true")
  private boolean durable;

  @Column(nullable = false)
  @ColumnDefault("false")
  private boolean exclusive;

  @Column(name = "dead_letter", nullable = false)
  @ColumnDefault("false")
  private boolean deadLetter;

  @Column(name = "auto_delete", nullable = false)
  @ColumnDefault("false")
  private boolean autoDelete;

  @Type(JsonType.class)
  @Column(columnDefinition = "json")
  private JsonNode arguments;

  @Enumerated(EnumType.STRING)
  @Column
  private Status status;

  @Column(name = "overflow_policy", nullable = false)
  @Enumerated(EnumType.STRING)
  @ColumnDefault("BLOCK")
  private OverflowPolicy overflowPolicy;

  @Column(name = "max_bytes", nullable = false)
  @ColumnDefault("4096")
  private long maxBytes;

  @Column(name = "max_message_count", nullable = false)
  private long maxMessage;

  @Column(name = "message_ttl_ms")
  private long messageTtlMs;

  @Column(name = "message_retention_ms")
  private long messageRetentionMs;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private boolean deleted;

  @Override
  public boolean equals(final Object o) {

    if (!(o instanceof Queue queue)) {
      return false;
    }

    return Objects.equals(id, queue.id) && Objects.equals(actualName, queue.actualName);
  }

  @Override
  public int hashCode() {

    return Objects.hash(id, actualName);
  }
}
