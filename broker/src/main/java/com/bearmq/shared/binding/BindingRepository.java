package com.bearmq.shared.binding;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BindingRepository extends JpaRepository<Binding, String> {
  List<Binding> findAllByVhostId(String vhostId);

  @Query(
      "select b from Binding b left join fetch b.sourceExchangeRef left join fetch"
          + " b.destinationQueueRef left join fetch b.destinationExchangeRef where b.vhost.id ="
          + " :vhostId and b.deleted = false")
  List<Binding> findAllActiveForReadByVhostId(@Param("vhostId") String vhostId);

  @Modifying
  @Query("update Binding b set b.deleted = true where b.vhost.id = :vhostId and b.deleted = false")
  int softDeleteAllForVhost(@Param("vhostId") String vhostId);
}
