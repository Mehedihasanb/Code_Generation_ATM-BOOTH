package com.example.backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

	private final SecretKey signingKey;
	private final long expirationMs;

	// We removed the injected jwtSecret and replaced it with a dynamic key builder
	public JwtService(@Value("${security.jwt.expiration-ms:1800000}") long expirationMs) {

		// This is the magic line that logouts on restart. Every time the server starts,
		// it makes a new random key. Old tokens will fail instantly.
		this.signingKey = Jwts.SIG.HS256.key().build();

		this.expirationMs = expirationMs;
	}

	public String generateToken(UserDetails userDetails) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("roles", userDetails.getAuthorities());

		Date now = new Date();
		Date expiresAt = new Date(now.getTime() + expirationMs);

		return Jwts.builder()
				.claims(claims)
				.subject(userDetails.getUsername())
				.issuedAt(now)
				.expiration(expiresAt)
				.signWith(signingKey)
				.compact();
	}

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		String username = extractUsername(token);
		return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
	}

	private boolean isTokenExpired(String token) {
		Date expiration = extractClaim(token, Claims::getExpiration);
		return expiration.before(new Date());
	}

	private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		Claims claims = Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
		return claimsResolver.apply(claims);
	}
}