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

	private String employeeJwt() throws Exception {
		String body = objectMapper.writeValueAsString(Map.of(
			"email", "employee@inholland.nl",
			"password", "Password123!"
		));
		String response = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return objectMapper.readTree(response).get("token").asText();
	}

	@Test
	void loginReturnsJwtWhenCredentialsAreValid() throws Exception {
		Map<String, String> validLoginRequestBody = Map.of(
			"email", "customer@inholland.nl",
			"password", "Password123!"
		);

		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(validLoginRequestBody)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isString())
			.andExpect(jsonPath("$.customerApprovalStatus").value("APPROVED"));
	}

	@Test
	void loginReturnsUnauthorizedForInvalidCredentials() throws Exception {
		Map<String, String> invalidLoginRequestBody = Map.of(
			"email", "customer@inholland.nl",
			"password", "wrong-password"
		);

		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidLoginRequestBody)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEmployeeRouteReturnsUnauthorizedWithoutJwt() throws Exception {
		mockMvc.perform(get("/users"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEmployeeRouteReturnsOkWithValidEmployeeJwt() throws Exception {
		mockMvc.perform(get("/users")
				.param("size", "10")
				.header("Authorization", "Bearer " + employeeJwt()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isArray());
	}
}
