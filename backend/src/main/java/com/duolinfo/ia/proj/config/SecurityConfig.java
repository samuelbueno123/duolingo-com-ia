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

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

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
            CorsConfigurationSource corsConfigurationSource) throws Exception {

        http
            .cors(cors -> cors
                .configurationSource(corsConfigurationSource)
            )

            .csrf(csrf -> csrf.disable())

            .securityContext(context -> context
                .securityContextRepository(
                    securityContextRepository
                )
            )

            .sessionManagement(session -> session
                .sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authorizeHttpRequests(authorize -> authorize

                // ==========================================
                // 1. CORS / PRE-FLIGHT
                // ==========================================

                .requestMatchers(
                    HttpMethod.OPTIONS,
                    "/**"
                ).permitAll()

                // ==========================================
                // 2. CADASTRO E AUTENTICAÇÃO PÚBLICOS
                // ==========================================

                // Cadastro de usuário
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/users"
                ).permitAll()

                // Cadastro de aluno
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/students"
                ).permitAll()

                // Cadastro de professor
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/teachers"
                ).permitAll()

                // Login com Google
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/google"
                ).permitAll()

                // Login com email e senha
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/login"
                ).permitAll()

                // ==========================================
                // 3. SWAGGER / DOCUMENTAÇÃO
                // ==========================================

                .requestMatchers(
                    "/",
                    "/index.html",
                    "/favicon.ico",
                    "/error",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/webjars/**"
                ).permitAll()

                // ==========================================
                // 4. OPERAÇÕES DE PROFESSOR
                // ==========================================

                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/teachers/**"
                ).hasRole("TEACHER")

                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/teachers/**"
                ).hasRole("TEACHER")

                // ==========================================
                // 5. RESTANTE DA API
                // ==========================================

                .anyRequest().authenticated()
            )

            // ==========================================
            // JWT FILTER
            // ==========================================

            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class
            )

            // ==========================================
            // TRATAMENTO DE ERROS
            // ==========================================

            .exceptionHandling(exceptions -> exceptions

                .authenticationEntryPoint(
                    (request, response, exception) ->
                        writeError(
                            response,
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Token ausente ou inválido."
                        )
                )

                .accessDeniedHandler(
                    (request, response, exception) ->
                        writeError(
                            response,
                            HttpServletResponse.SC_FORBIDDEN,
                            "Acesso negado para o seu perfil."
                        )
                )
            )

            .headers(headers ->
                headers.frameOptions(
                    frameOptions ->
                        frameOptions.sameOrigin()
                )
            );

        return http.build();
    }

    private static void writeError(
            HttpServletResponse response,
            int status,
            String message) throws IOException {

        response.setStatus(status);
        response.setContentType(
            MediaType.APPLICATION_JSON_VALUE
        );
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
            "{\"success\":false,\"message\":\""
            + message
            + "\"}"
        );
    }
}