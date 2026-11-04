package com.duolinfo.ia.proj.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

	@Value("${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5000,http://localhost:5500,http://localhost:8081}")
	private String allowedOrigins;

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {

		CorsConfiguration configuration = new CorsConfiguration();

		List<String> origins = Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.filter(origin -> !origin.isBlank())
				.toList();

		if (origins.contains("*")) {
			configuration.setAllowedOriginPatterns(List.of("*"));
		} else {
			// Permite origens explicitamente configuradas e também origens de rede local/IP para acesso mobile
			configuration.setAllowedOriginPatterns(List.of(
					"*",
					"http://localhost:*",
					"http://127.0.0.1:*",
					"http://192.168.*.*:*",
					"http://10.*.*.*:*",
					"http://172.*.*.*:*"
			));
		}

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

		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source =
				new UrlBasedCorsConfigurationSource();

		source.registerCorsConfiguration("/**", configuration);

		return source;
	}
}
