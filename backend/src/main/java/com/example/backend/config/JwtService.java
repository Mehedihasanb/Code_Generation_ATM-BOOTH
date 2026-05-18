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

@Service
public class JwtService {

	private final SecretKey signingKey;
	private final long expirationMs;

	public JwtService(
			@Value("${security.jwt.secret}") String jwtSecret,
			@Value("${security.jwt.expiration-ms}") long expirationMs) {
		this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
		this.expirationMs = expirationMs;
	}

	public String generateToken(UserDetails userDetails) {
		Map<String, Object> jwtClaims = new HashMap<>();
		jwtClaims.put("roles", userDetails.getAuthorities());

		Date issuedAtDate = new Date();
		Date expiresAtDate = new Date(issuedAtDate.getTime() + expirationMs);

		return Jwts.builder()
				.claims(jwtClaims)
				.subject(userDetails.getUsername())
				.issuedAt(issuedAtDate)
				.expiration(expiresAtDate)
				.signWith(signingKey)
				.compact();
	}

	public String extractUsername(String jwtTokenString) {
		return extractClaim(jwtTokenString, Claims::getSubject);
	}

	public boolean isTokenValid(String jwtTokenString, UserDetails userDetails) {
		String emailFromTokenSubject = extractUsername(jwtTokenString);
		return emailFromTokenSubject.equals(userDetails.getUsername()) && !isTokenExpired(jwtTokenString);
	}

	private boolean isTokenExpired(String jwtTokenString) {
		Date expirationDate = extractClaim(jwtTokenString, Claims::getExpiration);
		return expirationDate.before(new Date());
	}

	private <ClaimType> ClaimType extractClaim(String jwtTokenString, Function<Claims, ClaimType> claimsExtractor) {
		Claims parsedClaims = Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(jwtTokenString)
				.getPayload();
		return claimsExtractor.apply(parsedClaims);
	}
}
