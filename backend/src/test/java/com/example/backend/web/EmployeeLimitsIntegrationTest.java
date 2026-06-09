package com.example.backend.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeLimitsIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private String loginJsonWebToken(String email, String password) throws Exception {
		String loginRequestBody = objectMapper.writeValueAsString(Map.of("email", email, "password", password));
		String loginResponseBody = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginRequestBody))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return objectMapper.readTree(loginResponseBody).get("token").asText();
	}

	private String employeeJwt() throws Exception {
		return loginJsonWebToken("employee@inholland.nl", "Password123!");
	}

	private String customerJwt() throws Exception {
		return loginJsonWebToken("customer@inholland.nl", "Password123!");
	}

	private long registerAndOpenAccounts(String uniqueEmail) throws Exception {
		String registerRequestBody = objectMapper.writeValueAsString(Map.of(
			"firstName", "Limit",
			"lastName", "Test",
			"email", uniqueEmail,
			"password", "Password123!",
			"bsnNumber", "123456781",
			"phoneNumber", "+31 6 22222222"
		));
		String registerResponseBody = mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(registerRequestBody))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		long customerId = objectMapper.readTree(registerResponseBody).get("id").asLong();

		String createAccountsBody = objectMapper.writeValueAsString(Map.of(
			"customerRegistrationId", customerId,
			"dailyOutgoingTransferLimit", new BigDecimal("500.00"),
			"minimumAllowedBalance", new BigDecimal("5000.00")
		));
		mockMvc.perform(post("/accounts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(createAccountsBody)
				.header("Authorization", "Bearer " + employeeJwt()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.createdAccounts", hasSize(2)));

		return customerId;
	}

	@Test
	void updateLimitsWithoutJwtIsUnauthorized() throws Exception {
		String body = objectMapper.writeValueAsString(Map.of("absoluteLimit", new BigDecimal("3000.00")));
		mockMvc.perform(put("/users/1/limits")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void customerCannotUpdateLimits() throws Exception {
		String body = objectMapper.writeValueAsString(Map.of("absoluteLimit", new BigDecimal("3000.00")));
		mockMvc.perform(put("/users/1/limits")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body)
				.header("Authorization", "Bearer " + customerJwt()))
			.andExpect(status().isForbidden());
	}

	@Test
	void employeeCanUpdateAbsoluteLimitOnAllAccounts() throws Exception {
		String uniqueEmail = "limits.abs." + System.nanoTime() + "@example.com";
		long customerId = registerAndOpenAccounts(uniqueEmail);

		String updateBody = objectMapper.writeValueAsString(Map.of("absoluteLimit", new BigDecimal("8000.00")));
		mockMvc.perform(put("/users/" + customerId + "/limits")
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateBody)
				.header("Authorization", "Bearer " + employeeJwt()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.customerId").value(customerId))
			.andExpect(jsonPath("$.absoluteLimit").value(8000.00))
			.andExpect(jsonPath("$.accountsUpdated").value(2));

		String directoryResponse = mockMvc.perform(get("/users")
				.param("size", "500")
				.header("Authorization", "Bearer " + employeeJwt()))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		JsonNode directoryRoot = objectMapper.readTree(directoryResponse);
		for (JsonNode customerRow : directoryRoot.path("content")) {
			if (customerRow.path("id").asLong() != customerId) {
				continue;
			}
			for (JsonNode accountNode : customerRow.path("accounts")) {
				if (accountNode.has("minimumAllowedBalance")) {
					org.junit.jupiter.api.Assertions.assertEquals(
						0,
						new BigDecimal("8000.00").compareTo(
							accountNode.get("minimumAllowedBalance").decimalValue()));
				}
			}
			return;
		}
		throw new AssertionError("Customer not found in directory after limit update");
	}

	@Test
	void updateLimitsRejectsAbsoluteLimitBelowCurrentBalance() throws Exception {
		String uniqueEmail = "limits.low." + System.nanoTime() + "@example.com";
		long customerId = registerAndOpenAccounts(uniqueEmail);

		String updateBody = objectMapper.writeValueAsString(Map.of("absoluteLimit", new BigDecimal("100.00")));
		mockMvc.perform(put("/users/" + customerId + "/limits")
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateBody)
				.header("Authorization", "Bearer " + employeeJwt()))
			.andExpect(status().isBadRequest());
	}

	@Test
	void updateLimitsValidatesRequestBody() throws Exception {
		mockMvc.perform(put("/users/1/limits")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.header("Authorization", "Bearer " + employeeJwt()))
			.andExpect(status().isBadRequest());
	}
}
