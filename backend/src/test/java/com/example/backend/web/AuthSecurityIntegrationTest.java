package com.example.backend.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void loginReturnsJwtWhenCredentialsAreValid() throws Exception {
		Map<String, String> body = Map.of(
			"email", "customer@inholland.nl",
			"password", "Password123!"
		);

		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isString());
	}

	@Test
	void loginReturnsUnauthorizedForInvalidCredentials() throws Exception {
		Map<String, String> body = Map.of(
			"email", "customer@inholland.nl",
			"password", "wrong-password"
		);

		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void privateEndpointReturnsUnauthorizedWithoutJwt() throws Exception {
		mockMvc.perform(get("/api/messages"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void privateEndpointReturnsOkWithValidJwt() throws Exception {
		Map<String, String> body = Map.of(
			"email", "customer@inholland.nl",
			"password", "Password123!"
		);

		String loginResponse = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		String token = objectMapper.readTree(loginResponse).get("token").asText();

		mockMvc.perform(get("/api/messages")
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk());
	}
}
