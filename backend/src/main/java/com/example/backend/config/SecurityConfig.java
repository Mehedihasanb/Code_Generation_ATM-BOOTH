package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpStatus;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
		http
			// Vue dev server calls this API from another origin; Spring applies corsConfigurationSource below.
			.cors(Customizer.withDefaults())
			// REST API + JWT: no browser session cookie CSRF flow.
			.csrf(csrf -> csrf.disable())
			// We issue JWT ourselves from /auth/login, not Spring's built-in login forms.
			.formLogin(form -> form.disable())
			.httpBasic(httpBasic -> httpBasic.disable())
			// No server-side session: each request must carry Authorization Bearer token.
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			// If request hits a protected route without a valid login context, answer 401 (not a redirect).
			.exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/api/health",
					"/auth/login",
					"/v3/api-docs/**",
					"/swagger-ui/**",
					"/swagger-ui.html",
					"/swagger-ui/index.html",
					"/h2-console/**"
				).permitAll()
				.anyRequest().authenticated()
			)
			// Turn Authorization header into Spring Security authentication before controllers run.
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			// Lets H2 console load inside an iframe while developing.
			.headers(h -> h.frameOptions(f -> f.sameOrigin()));
		return http.build();
	}

	@Bean
	public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
		// Temporary demo users so JWT login works before real customer registration exists.
		return new InMemoryUserDetailsManager(
			User.withUsername("customer@inholland.nl")
				.password(passwordEncoder.encode("Password123!"))
				.roles("CUSTOMER")
				.build(),
			User.withUsername("employee@inholland.nl")
				.password(passwordEncoder.encode("Password123!"))
				.roles("EMPLOYEE")
				.build()
		);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		// Stores passwords hashed; AuthenticationManager compares raw password from login to this hash.
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		// Used by AuthController to check email + password in one line.
		return configuration.getAuthenticationManager();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		// Vite dev URL; browser blocks cross-origin calls unless these headers are allowed.
		config.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
