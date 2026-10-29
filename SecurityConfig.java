package com.example.studentapi.config;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
@Configuration
public class SecurityConfig {
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, @Value("${app.api-key}") String expectedKey) throws Exception {
    return http.csrf(csrf -> csrf.disable())
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth.requestMatchers("/api/health", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
      .anyRequest().permitAll())
      .addFilterBefore(new OncePerRequestFilter() {
        @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
          String path=req.getRequestURI();
          if (path.equals("/api/health") || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            chain.doFilter(req,res); return;
          }
          String supplied=req.getHeader("X-API-Key");
          if (supplied==null || !MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8), expectedKey.getBytes(StandardCharsets.UTF_8))) {
            res.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing or invalid API key"); return;
          }
          chain.doFilter(req,res);
        }
      }, UsernamePasswordAuthenticationFilter.class)
      .build();
  }
}
