/**
 * TCP broker runtime. Wire protocol DTOs stay minimal; any mapping to shared domain types should
 * use MapStruct where a projection is needed—same policy as {@code api} and {@code shared}.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "TCP broker runtime",
    allowedDependencies = "shared")
package com.bearmq.server;
