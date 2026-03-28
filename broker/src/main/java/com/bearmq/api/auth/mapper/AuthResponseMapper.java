package com.bearmq.api.auth.mapper;

import com.bearmq.api.auth.dto.AuthResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthResponseMapper {

  AuthResponse toResponse(String token, String refreshToken, String apiKey);
}
