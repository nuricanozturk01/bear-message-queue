package com.bearmq.shared.queue;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QueueRepository extends JpaRepository<Queue, String> {
  List<Queue> findAllByVhostId(String vhostId);

  @Query("select count(q) from Queue q where q.deleted = false")
  long countActiveGlobal();

  @Modifying
  @Query("update Queue q set q.deleted = true where q.vhost.id = :vhostId and q.deleted = false")
  int softDeleteAllForVhost(@Param("vhostId") String vhostId);
}
