package com.example.backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Creates JWT after login and checks JWT later using the same secret from application.yml.
 */
@Service
public class JwtService {

	private final SecretKey signingKey;
	private final long expirationMs;

	public JwtService(
		@Value("${security.jwt.secret}") String jwtSecret,
		@Value("${security.jwt.expiration-ms}") long expirationMs
	) {
		// Must stay identical across restarts or old tokens fail verification.
		this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
		this.expirationMs = expirationMs;
	}

	public String generateToken(UserDetails userDetails) {
		Map<String, Object> claims = new HashMap<>();
		// Stored inside JWT payload so filters later know ROLE_CUSTOMER vs ROLE_EMPLOYEE.
		claims.put("roles", userDetails.getAuthorities());

		Date now = new Date();
		Date expiresAt = new Date(now.getTime() + expirationMs);

		return Jwts.builder()
			.claims(claims)
			// Same string Spring Security uses as username (here the email).
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
			// Throws if signature broken or token tampered.
			.parseSignedClaims(token)
			.getPayload();
		return claimsResolver.apply(claims);
	}
}
