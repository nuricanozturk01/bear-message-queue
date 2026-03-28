package com.bearmq.api.common.mapper;

import com.bearmq.api.common.dtos.ApiErrorResponse;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApiErrorResponseMapper {

  @Mapping(target = "timestamp", expression = "java(java.time.Instant.now())")
  @Mapping(target = "fieldErrors", expression = "java((java.util.Map<String,String>)null)")
  ApiErrorResponse simple(int status, String error, String message, String path);

  @Mapping(target = "timestamp", expression = "java(java.time.Instant.now())")
  ApiErrorResponse withFieldErrors(
      int status, String error, String message, String path, Map<String, String> fieldErrors);
}
