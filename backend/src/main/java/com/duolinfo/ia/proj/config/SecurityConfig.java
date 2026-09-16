package com.duolinfo.ia.proj.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfigurationSource;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			SecurityContextRepository securityContextRepository,
			CorsConfigurationSource corsConfigurationSource) throws Exception {

		http
				// usa explicitamente o CorsConfigurationSource
				.cors(cors -> cors
						.configurationSource(corsConfigurationSource)
				     )

				// csrf desabilitado porque a api usa autenticação própria
				.csrf(csrf -> csrf.disable())

				// persistência da autenticação na sessão
				.securityContext(context -> context
								.securityContextRepository(securityContextRepository)
								.requireExplicitSave(true)
				                )

				.sessionManagement(session -> session
								.sessionCreationPolicy(
										SessionCreationPolicy.IF_REQUIRED
								                      )
				                  )

				.authorizeHttpRequests(authorize -> authorize

								// preflight do cors
								.requestMatchers(
										HttpMethod.OPTIONS,
										"/**"
								                ).permitAll()

								// login google
								.requestMatchers(
										HttpMethod.POST,
										"/api/auth/google"
								                ).permitAll()

								// recursos públicos
								.requestMatchers(
										"/",
										"/index.html",
										"/favicon.ico",
										"/error",
										"/h2-console/**",
										"/swagger-ui/**",
										"/swagger-ui.html",
										"/v3/api-docs/**",
										"/webjars/**"
								                ).permitAll()

								// todo o restante exige autenticação
								.anyRequest().authenticated()
				                      )

				.exceptionHandling(exceptions -> exceptions

								.authenticationEntryPoint(
										(request, response, exception) ->
												writeError(
														response,
														HttpServletResponse.SC_UNAUTHORIZED,
														"Autenticação necessária."
												          )
								                         )

								.accessDeniedHandler(
										(request, response, exception) ->
												writeError(
														response,
														HttpServletResponse.SC_FORBIDDEN,
														"Acesso negado."
												          )
								                    )
				                  )

				// logout controlado pelo AuthController
				.logout(logout -> logout.disable())

				// necessário para o h2 console em ambiente local
				.headers(headers ->
								headers.frameOptions(frameOptions ->
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
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		response.getWriter().write(
				"{\"success\":false,\"message\":\"" + message + "\"}"
		                          );
	}
}