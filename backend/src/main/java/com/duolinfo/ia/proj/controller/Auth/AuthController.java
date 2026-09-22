package com.duolinfo.ia.proj.controller.Auth;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.duolinfo.ia.proj.config.GoogleTokenVerifier;
import com.duolinfo.ia.proj.entity.User;
import com.duolinfo.ia.proj.service.JwtService;
import com.duolinfo.ia.proj.service.StudentService;
import com.duolinfo.ia.proj.service.TeacherService;
import com.duolinfo.ia.proj.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(
		name = "Auth",
		description = "Autenticação via Google e emissão de Tokens JWT"
)
public class AuthController {

	private final GoogleTokenVerifier googleTokenVerifier;
	private final StudentService studentService;
	private final TeacherService teacherService;
	private final UserService userService;
	private final JwtService jwtService; // Injeção do serviço de JWT

	public AuthController(
			GoogleTokenVerifier googleTokenVerifier,
			StudentService studentService,
			TeacherService teacherService,
			UserService userService,
			JwtService jwtService) {

		this.googleTokenVerifier = googleTokenVerifier;
		this.studentService = studentService;
		this.teacherService = teacherService;
		this.userService = userService;
		this.jwtService = jwtService;
	}

	@PostMapping("/google")
	@Operation(
			summary = "Realiza login com Google",
			description = "Valida o token do Google, cadastra/atualiza o usuário e retorna o Token JWT da aplicação."
	)
	public ResponseEntity<?> loginGoogle(@RequestBody GoogleLoginRequest request) {

		try {
			if (request == null
					|| request.getCredential() == null
					|| request.getCredential().isBlank()) {

				return ResponseEntity.badRequest().body(
						Map.of(
								"success", false,
								"message", "Credencial do Google não informada."
						      )
				                                       );
			}

			GoogleIdToken idToken = googleTokenVerifier.verify(request.getCredential());

			if (idToken == null) {
				return ResponseEntity
						.status(HttpStatus.UNAUTHORIZED)
						.body(
								Map.of(
										"success", false,
										"message", "Token do Google inválido."
								      )
						     );
			}

			Payload payload = idToken.getPayload();

			if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
				return ResponseEntity
						.status(HttpStatus.UNAUTHORIZED)
						.body(
								Map.of(
										"success", false,
										"message", "O Google não confirmou este endereço de email."
								      )
						     );
			}

			String googleId = payload.getSubject();
			String email = payload.getEmail();
			String name = (String) payload.get("name");
			String picture = (String) payload.get("picture");
			String hostedDomain = (String) payload.get("hd");

			// Garante que o usuário existe no banco de dados
			User user = userService.findByEmail(email);
			if (user == null) {
				user = new User();
				user.setEmail(email);
				user.setName(name);
				user.setGoogleId(googleId);
				user.setProfilePicture(picture);
				user = userService.create(user);
			} else if (user.getGoogleId() == null || user.getGoogleId().isBlank()) {
				user.setGoogleId(googleId);
				user.setProfilePicture(picture);
				userService.update(user);
			}

			String profileType = resolveProfileType(googleId, email);

			// Gera o Token JWT da sua aplicação
			String token = jwtService.generateToken(user, profileType);

			GooglePayload googlePayload = new GooglePayload(
					googleId,
					email,
					name,
					picture,
					hostedDomain
			);

			Map<String, Object> response = new LinkedHashMap<>();
			response.put("success", true);
			response.put("message", "Login Google realizado com sucesso.");
			response.put("token", token); // <-- O front-end precisa desse campo
			response.put("user", googlePayload);
			response.put("profileType", profileType);

			return ResponseEntity.ok(response);

		} catch (GeneralSecurityException | IOException | IllegalArgumentException exception) {
			return ResponseEntity
					.status(HttpStatus.UNAUTHORIZED)
					.body(
							Map.of(
									"success", false,
									"message", "Não foi possível validar o token do Google."
							      )
					     );
		}
	}

	@PostMapping("/login")
	@Operation(
			summary = "Realiza login com email e senha",
			description = "Valida as credenciais, busca/cria o usuário e retorna o Token JWT."
	)
	public ResponseEntity<?> loginWithPassword(@RequestBody PasswordLoginRequest request) {

		if (request == null
				|| request.getEmail() == null
				|| request.getEmail().isBlank()
				|| request.getPassword() == null
				|| request.getPassword().isBlank()) {

			return ResponseEntity.badRequest().body(
					Map.of(
							"success", false,
							"message", "Email e senha são obrigatórios."
					      )
			                                       );
		}

		String email = request.getEmail().trim().toLowerCase();
		String rawPassword = request.getPassword();

		User user = userService.findByEmail(email);

		if (user == null) {
			String defaultName = email.contains("@")
			                     ? email.substring(0, email.indexOf("@"))
			                     : email;
			if (!defaultName.isEmpty()) {
				defaultName = Character.toUpperCase(defaultName.charAt(0)) + defaultName.substring(1);
			}

			user = new User();
			user.setEmail(email);
			user.setName(defaultName);
			user.setPasswordHash(rawPassword);

			user = userService.create(user);
		} else {
			if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
				if (!user.getPasswordHash().equals(rawPassword)) {
					return ResponseEntity
							.status(HttpStatus.UNAUTHORIZED)
							.body(
									Map.of(
											"success", false,
											"message", "Senha incorreta."
									      )
							     );
				}
			} else {
				user.setPasswordHash(rawPassword);
				userService.update(user);
			}
		}

		String googleId = user.getGoogleId() != null ? user.getGoogleId() : "";
		String profileType = resolveProfileType(googleId, email);

		// Gera o Token JWT
		String token = jwtService.generateToken(user, profileType);

		GooglePayload userPayload = new GooglePayload(
				googleId,
				email,
				user.getName(),
				user.getProfilePicture(),
				null
		);

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("success", true);
		response.put("message", "Login realizado com sucesso.");
		response.put("token", token); // <-- O front-end precisa desse campo
		response.put("user", userPayload);
		response.put("profileType", profileType);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/me")
	@Operation(
			summary = "Retorna o usuário autenticado",
			description = "Retorna os dados do usuário a partir da autenticação resolvida pelo filtro JWT."
	)
	public ResponseEntity<?> me(Authentication authentication) {
		return ResponseEntity.ok(
				authenticatedResponse(authentication)
		                        );
	}

	@PostMapping("/logout")
	@Operation(
			summary = "Encerra a sessão",
			description = "Em arquitetura JWT, o logout é concluído limpando o token no front-end."
	)
	public ResponseEntity<?> logout() {
		return ResponseEntity.ok(
				Map.of(
						"success", true,
						"message", "Logout realizado com sucesso."
				      )
		                        );
	}

	private Map<String, Object> authenticatedResponse(Authentication authentication) {

		Map<String, Object> response = new LinkedHashMap<>();

		response.put("success", true);
		response.put(
				"authenticated",
				authentication != null && authentication.isAuthenticated()
		            );
		response.put(
				"user",
				authentication != null ? authentication.getPrincipal() : null
		            );
		response.put(
				"profileType",
				authentication == null ? null : getProfileType(authentication)
		            );

		return response;
	}

	private String getProfileType(Authentication authentication) {
		return authentication
				.getAuthorities()
				.stream()
				.findFirst()
				.map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
				.orElse("USER");
	}

	private String resolveProfileType(String googleId, String email) {

		if (teacherService.findByGoogleId(googleId) != null
				|| (email != null && teacherService.findByEmail(email) != null)) {
			return "TEACHER";
		}

		if (studentService.findByGoogleId(googleId) != null
				|| (email != null && studentService.findByEmail(email) != null)) {
			return "STUDENT";
		}

		return "USER";
	}
}