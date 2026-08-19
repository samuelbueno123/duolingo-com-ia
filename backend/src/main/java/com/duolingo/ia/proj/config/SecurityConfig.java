// Aqui é aonde gere o funcionamento dos dados e seguranção do backend
package com.duolingo.ia.proj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http)
            throws Exception {

        http

            // ==============================================
            // CORS
            // ==============================================

            .cors(cors -> {
            })


            // ==============================================
            // CSRF
            // ==============================================

            .csrf(csrf ->
                    csrf.disable()
            )


            // ==============================================
            // AUTORIZAÇÃO
            // ==============================================

            .authorizeHttpRequests(authorize -> authorize


                // ------------------------------------------
                // LOGIN GOOGLE
                // ------------------------------------------

                .requestMatchers(
                        "/api/auth/google"
                ).permitAll()

                // Preflight do navegador para o POST de login.
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()


                // ------------------------------------------
                // FRONTEND
                // ------------------------------------------

                .requestMatchers(
                        "/h2-console/**",
                        "/",
                        "/index.html",
                        "/app.js",
                        "/style.css",
                        "/favicon.ico"
                ).permitAll()


                // ------------------------------------------
                // RESTANTE
                // ------------------------------------------

                .anyRequest()
                .authenticated()
            );

        // Necessário apenas para o console H2 em ambiente local.
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));


        return http.build();
    }
}
