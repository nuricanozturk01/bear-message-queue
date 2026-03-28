package com.bearmq.api.spa;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public class SpaFallbackFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final FilterChain filterChain)
      throws ServletException, IOException {
    if (!"GET".equalsIgnoreCase(request.getMethod())) {
      filterChain.doFilter(request, response);
      return;
    }
    final String uri = request.getRequestURI();
    final String ctx = request.getContextPath();
    final String path = uri.length() > ctx.length() ? uri.substring(ctx.length()) : uri;
    if (!path.startsWith("/")) {
      filterChain.doFilter(request, response);
      return;
    }
    if (path.startsWith("/api/") || path.startsWith("/error")) {
      filterChain.doFilter(request, response);
      return;
    }
    final int slash = path.lastIndexOf('/');
    final String last = slash >= 0 ? path.substring(slash + 1) : path;
    if (last.contains(".")) {
      filterChain.doFilter(request, response);
      return;
    }
    request.getRequestDispatcher("/index.html").forward(request, response);
  }
}
