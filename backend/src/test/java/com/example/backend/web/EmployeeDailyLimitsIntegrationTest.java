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
class EmployeeDailyLimitsIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private String employeeJwt() throws Exception {
		String loginRequestBody = objectMapper.writeValueAsString(
				Map.of("email", "employee@inholland.nl", "password", "Password123!"));
		String loginResponseBody = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginRequestBody))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return objectMapper.readTree(loginResponseBody).get("token").asText();
	}

	private long registerAndOpenAccounts(String uniqueEmail, String firstName) throws Exception {
		String registerRequestBody = objectMapper.writeValueAsString(Map.of(
			"firstName", firstName,
			"lastName", "Limit",
			"email", uniqueEmail,
			"password", "Password123!",
			"bsnNumber", "123456783",
			"phoneNumber", "+31 6 33333333"
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
	void employeeCanUpdateDailyLimitOnAllAccounts() throws Exception {
		String suffix = String.valueOf(System.nanoTime());
		String uniqueEmail = "limits.daily." + suffix + "@example.com";
		String firstName = "Daily" + suffix;
		long customerId = registerAndOpenAccounts(uniqueEmail, firstName);

		String updateBody = objectMapper.writeValueAsString(
				Map.of("dailyOutgoingTransferLimit", new BigDecimal("1500.00")));
		mockMvc.perform(put("/users/" + customerId + "/limits")
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateBody)
				.header("Authorization", "Bearer " + employeeJwt()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.customerId").value(customerId))
			.andExpect(jsonPath("$.dailyOutgoingTransferLimit").value(1500.00))
			.andExpect(jsonPath("$.accountsUpdated").value(2));

		String searchResponse = mockMvc.perform(get("/users/search")
				.param("firstName", firstName)
				.param("lastName", "Limit")
				.header("Authorization", "Bearer " + employeeJwt()))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		JsonNode results = objectMapper.readTree(searchResponse);
		for (JsonNode customerRow : results) {
			if (customerRow.path("id").asLong() != customerId) {
				continue;
			}
			for (JsonNode accountNode : customerRow.path("accounts")) {
				org.junit.jupiter.api.Assertions.assertEquals(
					0,
					new BigDecimal("1500.00").compareTo(
						accountNode.get("dailyOutgoingTransferLimit").decimalValue()));
			}
			return;
		}
		throw new AssertionError("Customer not found after daily limit update");
	}

	@Test
	void updateDailyLimitRejectsZeroOrNegative() throws Exception {
		String suffix = String.valueOf(System.nanoTime());
		long customerId = registerAndOpenAccounts("limits.daily.bad." + suffix + "@example.com", "DailyBad" + suffix);

		String updateBody = objectMapper.writeValueAsString(
				Map.of("dailyOutgoingTransferLimit", new BigDecimal("0")));
		mockMvc.perform(put("/users/" + customerId + "/limits")
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateBody)
				.header("Authorization", "Bearer " + employeeJwt()))
			.andExpect(status().isBadRequest());
	}

	@Test
	void employeeCanUpdateBothLimitsInOneRequest() throws Exception {
		String suffix = String.valueOf(System.nanoTime());
		long customerId = registerAndOpenAccounts("limits.daily.both." + suffix + "@example.com", "DailyBoth" + suffix);

		String updateBody = objectMapper.writeValueAsString(Map.of(
			"absoluteLimit", new BigDecimal("9000.00"),
			"dailyOutgoingTransferLimit", new BigDecimal("2000.00")
		));
		mockMvc.perform(put("/users/" + customerId + "/limits")
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateBody)
				.header("Authorization", "Bearer " + employeeJwt()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.absoluteLimit").value(9000.00))
			.andExpect(jsonPath("$.dailyOutgoingTransferLimit").value(2000.00))
			.andExpect(jsonPath("$.accountsUpdated").value(2));
	}
}
