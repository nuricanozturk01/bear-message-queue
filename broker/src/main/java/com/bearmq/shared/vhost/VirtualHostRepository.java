package com.bearmq.shared.vhost;

import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VirtualHostRepository extends JpaRepository<VirtualHost, String> {

  @Query("select count(v) from VirtualHost v where v.deleted = false")
  long countActiveGlobal();

  Page<VirtualHost> findAllByDeletedFalse(@NotNull Pageable pageable);

  Optional<VirtualHost> findByNameAndDeletedFalse(String name);

  @Query(
      "select v from VirtualHost v where v.name = :name and v.username = :username "
          + "and v.password = :password and v.deleted = false "
          + "and v.status = com.bearmq.shared.broker.Status.ACTIVE")
  Optional<VirtualHost> findActiveByCredentials(
      @Param("name") String name,
      @Param("username") String username,
      @Param("password") String password);
}
