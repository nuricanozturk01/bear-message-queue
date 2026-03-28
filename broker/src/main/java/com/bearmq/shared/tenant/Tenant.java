package com.bearmq.shared.tenant;

import com.bearmq.shared.vhost.VirtualHost;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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
@Table(name = "tenant")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Tenant {

  @Id
  @Column(nullable = false, length = 26, unique = true)
  private String id;

  @Column(nullable = false, length = 150, unique = true)
  private String username;

  @Column(nullable = false)
  private String password;

  @Column(nullable = false, length = 16)
  private String salt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @OneToMany(mappedBy = "tenant")
  private Set<VirtualHost> vhosts;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @ColumnDefault("ACTIVE")
  private TenantStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @ColumnDefault("USER")
  private TenantRole role;

  @Column(nullable = false)
  private boolean deleted;

  @Override
  public boolean equals(final Object o) {

    if (!(o instanceof final Tenant tenant)) {
      return false;
    }
    return Objects.equals(this.id, tenant.id) && Objects.equals(this.username, tenant.username);
  }

  @Override
  public int hashCode() {

    return Objects.hash(this.id, this.username);
  }
}
