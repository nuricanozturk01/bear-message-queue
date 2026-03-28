package com.bearmq.shared.exchange;

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
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "exchange")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public final class Exchange {
  @Id
  @Column(nullable = false, length = 26, unique = true)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vhost_id", nullable = false)
  private VirtualHost vhost;

  @Column(nullable = false)
  private String name;

  @Column(name = "actual_name", nullable = false)
  private String actualName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private ExchangeType type;

  @Column(nullable = false)
  @ColumnDefault("true")
  private boolean durable;

  @Column(name = "auto_delete", nullable = false)
  @ColumnDefault("'false'")
  private boolean autoDelete;

  @Column(nullable = false)
  @ColumnDefault("'false'")
  private boolean internal;

  @Column(nullable = false)
  @ColumnDefault("'false'")
  private boolean delayed;

  @Type(JsonType.class)
  @Column(columnDefinition = "json")
  private JsonNode arguments;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @ColumnDefault("'ACTIVE'")
  private Status status;

  @Column(nullable = false)
  @ColumnDefault("0")
  private long version;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(nullable = false)
  private boolean deleted;

  @Override
  public boolean equals(final Object o) {

    if (!(o instanceof Exchange e)) {
      return false;
    }

    return Objects.equals(id, e.id) && Objects.equals(actualName, e.actualName);
  }

  @Override
  public int hashCode() {

    return Objects.hash(id, actualName);
  }
}
