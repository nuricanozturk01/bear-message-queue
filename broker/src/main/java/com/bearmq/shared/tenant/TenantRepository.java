package com.bearmq.shared.tenant;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, String> {
  Optional<Tenant> findByUsername(String username);

  long countByRoleAndDeleted(TenantRole role, boolean deleted);
}
