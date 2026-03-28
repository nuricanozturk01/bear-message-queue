package com.bearmq.shared.queue;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QueueRepository extends JpaRepository<Queue, String> {
  List<Queue> findAllByVhostId(String vhostId);

  @Query("select count(q) from Queue q where q.deleted = false")
  long countActiveGlobal();
}
