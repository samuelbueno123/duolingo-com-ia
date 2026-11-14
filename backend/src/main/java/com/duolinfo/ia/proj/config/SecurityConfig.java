package com.duolinfo.ia.proj.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new NullSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            CorsConfigurationSource corsConfigurationSource,
            JwtAuthenticationFilter jwtAuthFilter) throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .securityContext(context -> context
                .securityContextRepository(securityContextRepository))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                .requestMatchers(HttpMethod.POST, "/api/users", "/api/students")
                    .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/institutions/requests")
                    .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/teachers")
                    .permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/api/auth/google",
                    "/api/auth/login",
                    "/api/auth/refresh",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password")
                    .permitAll()

                .requestMatchers(HttpMethod.GET,
                    "/api/catalog/languages",
                    "/api/catalog/proficiency-levels").permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/api/institutions",
                    "/api/institutions/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/media/profile-pictures/**")
                    .permitAll()
                .requestMatchers(HttpMethod.GET,
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info")
                    .permitAll()
                .requestMatchers("/actuator/**")
                    .hasRole("ADMIN")

                .requestMatchers(
                    "/api/students/me",
                    "/api/students/me/**").hasAnyRole("STUDENT", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/teachers/me").authenticated()
                .requestMatchers(
                    "/api/teachers/me",
                    "/api/teachers/me/**").authenticated()
                .requestMatchers("/api/users/me", "/api/users/me/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/users/*").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/users/*").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/users/*").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/users/*/profile-picture").authenticated()

                .requestMatchers(HttpMethod.GET,
                    "/api/students/*/enrollments/active",
                    "/api/students/*/enrollments")
                    .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/students/*")
                    .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/students/*")
                    .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/students/**")
                    .hasAnyRole("STUDENT", "TEACHER", "ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/teachers/**")
                    .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/teachers/**")
                    .hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/teachers/**")
                    .hasRole("ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/classes/**")
                    .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers("/api/classes/**")
                    .hasAnyRole("TEACHER", "ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/schools/**")
                    .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers("/api/schools/**")
                    .hasAnyRole("TEACHER", "ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/enrollments/**")
                    .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers("/api/enrollments/**")
                    .hasAnyRole("TEACHER", "ADMIN")

                .requestMatchers("/api/class-teacher-assignments/**")
                    .hasAnyRole("TEACHER", "ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/achievements/**")
                    .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers("/api/achievements/**")
                    .hasRole("ADMIN")

                .requestMatchers(HttpMethod.POST, "/api/exercises", "/api/exercises/generate-ai", "/api/exercises/generate-options-ai", "/api/exercises/explain-ai")
                    .hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/exercises/submissions/**")
                    .hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/exercises/*/submissions")
                    .hasAnyRole("STUDENT", "ADMIN")
                .requestMatchers("/api/exercises/**")
                    .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/rankings/**")
                    .hasAnyRole("STUDENT", "TEACHER", "ADMIN")

                // Language learning modules
                .requestMatchers("/api/srs/**")
                    .hasAnyRole("STUDENT", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/ai/explain-mistake")
                    .authenticated()
                .requestMatchers("/api/ai/**")
                    .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers("/api/pronunciation/**")
                    .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers("/api/community/**")
                    .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers("/api/learning-path/**")
                    .hasAnyRole("STUDENT", "TEACHER", "ADMIN")

                .requestMatchers("/api/auth/me", "/api/auth/me/**", "/api/auth/logout").authenticated()
                .requestMatchers("/api/home/**").authenticated()
                .requestMatchers("/api/realtime/**").authenticated()
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/favicon.ico",
                    "/error",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/webjars/**").permitAll()

                // New API operations are administrative unless explicitly granted above.
                .anyRequest().hasRole("ADMIN"))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) ->
                    writeError(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Unauthorized", "Token ausente ou inválido."))
                .accessDeniedHandler((request, response, exception) ->
                    writeError(request, response, HttpServletResponse.SC_FORBIDDEN,
                        "Forbidden", "Acesso negado para o seu perfil.")))
            .headers(headers -> headers.frameOptions(frameOptions ->
                frameOptions.sameOrigin()));

        return http.build();
    }

    private static void writeError(
            jakarta.servlet.http.HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String error,
            String message) throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = (String) request.getAttribute(CorrelationIdFilter.REQUEST_ATTR);
        }
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = org.slf4j.MDC.get(CorrelationIdFilter.MDC_KEY);
        }
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = "N/A";
        }

        String path = request.getRequestURI();
        String timestamp = java.time.LocalDateTime.now().toString();

        String json = String.format(
            "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\",\"correlationId\":\"%s\",\"success\":false}",
            timestamp, status, error, message, path, correlationId
        );
        response.getWriter().write(json);
    }
}
