package com.bearmq.shared.broker.runtime;

import java.util.Set;

public interface BrokerRuntimePort {

  void reloadVhostRuntime(String vhostId);

  boolean isVhostLoaded(String vhostId);

  Set<String> getLoadedQueueNames(String vhostId);
}
