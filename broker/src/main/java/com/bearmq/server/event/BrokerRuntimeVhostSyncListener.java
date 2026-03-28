package com.bearmq.server.event;

import com.bearmq.server.broker.facade.BrokerServerFacade;
import com.bearmq.shared.event.VirtualHostActivatedEvent;
import com.bearmq.shared.event.VirtualHostDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrokerRuntimeVhostSyncListener {

  private final BrokerServerFacade brokerServerFacade;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onVirtualHostDeleted(final VirtualHostDeletedEvent event) {

    log.debug("Unloading runtime broker state for vhostId={}", event.virtualHostId());
    this.brokerServerFacade.unloadVhost(event.virtualHostId());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onVirtualHostActivated(final VirtualHostActivatedEvent event) {

    log.debug("Loading runtime broker state for vhostId={}", event.virtualHostId());
    this.brokerServerFacade.loadVhostRuntime(event.virtualHostId());
  }
}
