// Lugar para Autorizar outras URLs, métodos e headers para o CORS. O Spring Security já tem um filtro de CORS, então não é necessário criar um filtro manualmente.

package com.duolingo.ia.proj.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();


        // ==================================================
        // FRONTEND
        // ==================================================

        configuration.setAllowedOrigins(
                List.of(
                        "http://127.0.0.1:5501",
                        "http://localhost:5501",
                        "http://127.0.0.1:5500",
                        "http://localhost:5500"
                )
        );


        // ==================================================
        // MÉTODOS
        // ==================================================

        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "PATCH",
                        "OPTIONS"
                )
        );


        // ==================================================
        // HEADERS
        // ==================================================

        configuration.setAllowedHeaders(
                List.of("*")
        );


        // ==================================================
        // CREDENCIAIS
        // ==================================================

        configuration.setAllowCredentials(
                true
        );


        // ==================================================
        // REGISTRAR CONFIGURAÇÃO
        // ==================================================

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );


        return source;
    }
}
