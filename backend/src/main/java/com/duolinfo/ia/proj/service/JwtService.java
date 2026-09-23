package com.duolinfo.ia.proj.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.duolinfo.ia.proj.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final String SECRET_KEY = "sua_chave_secreta_super_segura_com_32_bytes_no_minimo";

	private SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
	}

	// Método com 2 parâmetros esperado pelos Controllers
	public String generateToken(User user, String profileType) {
		return Jwts.builder()
		           .subject(user.getEmail())
		           .claim("id", user.getId())
		           .claim("profileType", profileType)
		           .issuedAt(new Date(System.currentTimeMillis()))
		           .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
		           .signWith(getSigningKey())
		           .compact();
	}

	// Sobrecarga com 1 parâmetro para retrocompatibilidade
	public String generateToken(User user) {
		return generateToken(user, "USER");
	}

	public String getEmailFromToken(String token) {
		return getClaims(token).getSubject();
	}

	public String getRoleFromToken(String token) {
		return getClaims(token).get("profileType", String.class);
	}

	public boolean isTokenValid(String token) {
		try {
			getClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private Claims getClaims(String token) {
		return Jwts.parser()
		           .verifyWith(getSigningKey())
		           .build()
		           .parseSignedClaims(token)
		           .getPayload();
	}
}