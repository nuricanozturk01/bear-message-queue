/**
 * HTTP API module. REST DTO and record projections are assembled with MapStruct ({@code @Mapper});
 * avoid ad-hoc field-by-field mapping in services and controllers.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "HTTP API",
    allowedDependencies = "shared")
package com.bearmq.api;
