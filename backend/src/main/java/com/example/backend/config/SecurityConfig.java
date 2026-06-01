package com.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.example.backend.domain.CustomerApprovalStatus;
import com.example.backend.domain.UserRegistration;
import com.example.backend.repository.UserRegistrationRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Value("${app.cors.allowed-origins}")
	private String allowedOrigins;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
			JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
		httpSecurity
				// Vue dev server calls this API from another origin; Spring applies
				// corsConfigurationSource below.
				.cors(Customizer.withDefaults())
				// REST API + JWT: no browser session cookie CSRF flow.
				.csrf(crossSiteRequestForgeryConfig -> crossSiteRequestForgeryConfig.disable())
				// We issue JWT ourselves from /auth/login, not Spring's built-in login forms.
				.formLogin(formLoginConfig -> formLoginConfig.disable())
				.httpBasic(httpBasicAuthConfig -> httpBasicAuthConfig.disable())
				// No server-side session: each request must carry Authorization Bearer token.
				.sessionManagement(sessionManagementConfig -> sessionManagementConfig
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				// If request hits a protected route without a valid login context, answer 401
				// not a redirect
				.exceptionHandling(exceptionHandlingConfig -> exceptionHandlingConfig
						.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.authorizeHttpRequests(authorizeRequestsConfig -> authorizeRequestsConfig
						.requestMatchers(HttpMethod.GET, "/accounts/checking-options").hasRole("EMPLOYEE")
						.requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()
						.requestMatchers(HttpMethod.POST, "/auth/customers/*/deny").hasRole("EMPLOYEE")
						.requestMatchers(HttpMethod.GET, "/users/search").hasRole("EMPLOYEE")
						.requestMatchers(HttpMethod.GET, "/users").hasAnyRole("EMPLOYEE", "CUSTOMER")
						.requestMatchers(HttpMethod.GET, "/accounts/mine").hasRole("CUSTOMER")
						.requestMatchers(HttpMethod.GET, "/transactions").authenticated()
						.requestMatchers(HttpMethod.POST, "/transactions").hasAnyRole("CUSTOMER", "EMPLOYEE")
						.requestMatchers(HttpMethod.POST, "/accounts").hasRole("EMPLOYEE")
						.requestMatchers(HttpMethod.PUT, "/accounts/*/close").hasRole("EMPLOYEE")
						.requestMatchers(HttpMethod.PUT, "/users/*/limits").hasRole("EMPLOYEE")
						// Add this line so Spring Security stops hiding our custom error messages!
						.requestMatchers("/error").permitAll()
						.requestMatchers(
								"/api/health",
								"/v3/api-docs/**",
								"/swagger-ui/**",
								"/swagger-ui.html",
								"/swagger-ui/index.html",
								"/h2-console/**")
						.permitAll()
						.anyRequest().hasAnyRole("CUSTOMER", "EMPLOYEE", "PENDING_CUSTOMER"))
				// Turn Authorization header into Spring Security authentication before
				// controllers run.
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				// Lets H2 console load inside an iframe while developing.
				.headers(headerConfig -> headerConfig
						.frameOptions(frameOptionsConfig -> frameOptionsConfig.sameOrigin()));
		return httpSecurity.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
			throws Exception {
		// Used by AuthController to check email + password in one line.
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public UserDetailsService userDetailsService(UserRegistrationRepository userRegistrationRepository) {
		return loginEmail -> userRegistrationRepository.findByEmail(loginEmail.trim().toLowerCase())
				.map(userRegistration -> User.withUsername(userRegistration.getEmail())
						.password(userRegistration.getPassword())
						.roles(resolveSpringSecurityRole(userRegistration))
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}

	private String resolveSpringSecurityRole(UserRegistration userRegistration) {
		if (!"CUSTOMER".equals(userRegistration.getRole())) {
			return userRegistration.getRole();
		}
		if (userRegistration.getCustomerApprovalStatus() == CustomerApprovalStatus.PENDING) {
			return "PENDING_CUSTOMER";
		}
		return "CUSTOMER";
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration corsConfiguration = new CorsConfiguration();
		List<String> origins = Arrays.stream(allowedOrigins.split(","))
			.map(String::trim)
			.filter(origin -> !origin.isEmpty())
			.toList();
		corsConfiguration.setAllowedOrigins(origins);
		corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		corsConfiguration.setAllowedHeaders(List.of("*"));
		corsConfiguration.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
		urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
		return urlBasedCorsConfigurationSource;
	}
}
