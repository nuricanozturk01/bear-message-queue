/**
 * Open module: domain model, persistence, and ports. {@code api} and {@code server} may depend on
 * {@code shared}; {@code shared} must not depend on {@code api} or {@code server}.
 *
 * <p>Entity/request projections exposed across module boundaries use MapStruct ({@code @Mapper}) in
 * {@code shared.converter} (and related packages); prefer mappers over manual DTO construction.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Shared domain",
    type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.bearmq.shared;
