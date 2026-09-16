// Lugar para Autorizar outras URLs, métodos e headers para o CORS. O Spring Security já tem um filtro de CORS, então não é necessário criar um filtro manualmente.

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

	@Value("${CORS_ALLOWED_ORIGINS}")
	private String allowedOrigins;

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {

		CorsConfiguration configuration = new CorsConfiguration();

		configuration.setAllowedOrigins(
				Arrays.stream(allowedOrigins.split(","))
				      .map(String::trim)
				      .filter(origin -> !origin.isBlank())
				      .toList()
		                               );

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