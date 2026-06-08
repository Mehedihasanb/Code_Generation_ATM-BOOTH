package com.example.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

// runs on every request BEFORE controllers (wired in SecurityConfig.addFilterBefore)
// reads Authorization: Bearer <token> from the frontend and tells Spring who is logged in
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;

	public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String authHeader = request.getHeader("Authorization");

		// no token is fine for public routes like /auth/login
		// on protected routes SecurityConfig returns 401 because nobody got set as authenticated
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		// cut off the "Bearer " prefix, leave just the token string
		String jwtToken = authHeader.substring(7);
		try {
			String emailFromTokenSubject = jwtService.extractUsername(jwtToken);

			// only set auth if not already set (avoids doing it twice on same request)
			if (emailFromTokenSubject != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				// load full user + role from DB (same userDetailsService as SecurityConfig)
				UserDetails userDetails = userDetailsService.loadUserByUsername(emailFromTokenSubject);

				if (jwtService.isTokenValid(jwtToken, userDetails)) {
					// this line is what makes hasRole("EMPLOYEE") and @PreAuthorize work in controllers
					UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
							userDetails,
							null,
							userDetails.getAuthorities());
					authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authenticationToken);
				}
			}
		} catch (Exception invalidJwtException) {
			// bad or expired token: clear context, request continues, protected route still gets 401
			logger.debug("JWT rejected for request {}", request.getRequestURI(), invalidJwtException);
			SecurityContextHolder.clearContext();
		}

		// always pass the request on to the next filter or controller
		filterChain.doFilter(request, response);
	}
}
