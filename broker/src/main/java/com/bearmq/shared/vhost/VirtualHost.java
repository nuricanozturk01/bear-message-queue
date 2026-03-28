package com.bearmq.shared.vhost;

import com.bearmq.shared.binding.Binding;
import com.bearmq.shared.broker.Status;
import com.bearmq.shared.exchange.Exchange;
import com.bearmq.shared.queue.Queue;
import com.bearmq.shared.tenant.Tenant;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "virtual_host")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class VirtualHost {

  @Id
  @Column(nullable = false, unique = true, length = 26)
  private String id;

  @Column private String name;

  @Column private String description;

  @Column(nullable = false, length = 150, unique = true)
  private String username;

  @Column(nullable = false, length = 150)
  private String password;

  @Column(nullable = false)
  private String domain;

  @Column(nullable = false, length = 150, unique = true)
  private String url;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @ColumnDefault("ACTIVE")
  private Status status;

  @OneToMany(
      mappedBy = "vhost",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private Set<Queue> queues;

  @OneToMany(
      mappedBy = "vhost",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private Set<Exchange> exchanges;

  @OneToMany(
      mappedBy = "vhost",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private Set<Binding> bindings;

  @Column(nullable = false)
  private boolean deleted;

  @Override
  public boolean equals(final Object o) {

    if (!(o instanceof VirtualHost that)) {
      return false;
    }

    return Objects.equals(this.id, that.id) && Objects.equals(this.name, that.name);
  }

  @Override
  public int hashCode() {

    return Objects.hash(this.id, this.name);
  }
}
