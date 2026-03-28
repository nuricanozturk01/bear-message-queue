package com.bearmq;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithStructureTest {

  @Test
  void verifyModuleBoundaries() {
    ApplicationModules.of(BrokerApplication.class).verify();
  }
}
