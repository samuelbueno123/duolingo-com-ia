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
                    "/api/auth/google", "/api/auth/login", "/api/auth/refresh")
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
                    "/api/students/me",
                    "/api/teachers/me").authenticated()
                .requestMatchers(HttpMethod.GET,
                    "/api/students/*/enrollments/active")
                    .hasAnyRole("STUDENT", "ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/teachers/**")
                    .hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/teachers/**")
                    .hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/teachers/**")
                    .hasRole("ADMIN")

                .requestMatchers(HttpMethod.POST, "/api/exercises")
                    .hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/exercises/submissions/**")
                    .hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/exercises/*/submissions")
                    .hasAnyRole("STUDENT", "ADMIN")
                .requestMatchers("/api/exercises/**")
                    .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/rankings/**")
                    .hasAnyRole("STUDENT", "TEACHER", "ADMIN")

                .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                .requestMatchers("/api/home/**").authenticated()
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
                    writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        "Token ausente ou inválido."))
                .accessDeniedHandler((request, response, exception) ->
                    writeError(response, HttpServletResponse.SC_FORBIDDEN,
                        "Acesso negado para o seu perfil.")))
            .headers(headers -> headers.frameOptions(frameOptions ->
                frameOptions.sameOrigin()));

        return http.build();
    }

    private static void writeError(
            HttpServletResponse response,
            int status,
            String message) throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
            "{\"success\":false,\"message\":\"" + message + "\"}");
    }
}
