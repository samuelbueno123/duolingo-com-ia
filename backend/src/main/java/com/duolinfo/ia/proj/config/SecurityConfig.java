package com.duolinfo.ia.proj.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthFilter; // Seu filtro de validação JWT

	public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
		this.jwtAuthFilter = jwtAuthFilter;
	}

	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new NullSecurityContextRepository(); // Não usa sessão HTTP
	}

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			SecurityContextRepository securityContextRepository,
			CorsConfigurationSource corsConfigurationSource) throws Exception {

		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.csrf(csrf -> csrf.disable())

				.securityContext(context -> context
								.securityContextRepository(securityContextRepository)
				                )
				.sessionManagement(session -> session
								.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				                  )

				.authorizeHttpRequests(authorize -> authorize
								// Preflight do CORS
								.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

								// Endpoints públicos de cadastro e autenticação
								.requestMatchers(HttpMethod.POST, "/api/users").permitAll() // Criação de usuário
								.requestMatchers(HttpMethod.POST, "/api/auth/google").permitAll() // Login/Cadastro Google
								.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll() // Login por email/senha

								// Documentação pública
								.requestMatchers(
										"/", "/index.html", "/favicon.ico", "/error",
										"/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/webjars/**"
								                ).permitAll()

								// Todos os outros endpoints exigem o Token JWT
								.anyRequest().authenticated()
				                      )

				// Adiciona o filtro JWT antes do filtro padrão do Spring
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

				.exceptionHandling(exceptions -> exceptions
								.authenticationEntryPoint((request, response, exception) ->
												writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token ausente ou inválido.")
								                         )
								.accessDeniedHandler((request, response, exception) ->
												writeError(response, HttpServletResponse.SC_FORBIDDEN, "Acesso negado.")
								                    )
				                  )

				.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

		return http.build();
	}

	private static void writeError(HttpServletResponse response, int status, String message) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
	}
}