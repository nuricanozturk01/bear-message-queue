package com.bearmq.shared.exchange;

import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ExchangeRepository extends JpaRepository<Exchange, String> {
  Set<Exchange> findAllByVhostId(String vhostId);

  @Query("from Exchange e where e.vhost.id = :vhostId")
  List<Exchange> findListByVhostId(String vhostId);

  @Query("select count(e) from Exchange e where e.deleted = false")
  long countActiveGlobal();
}
