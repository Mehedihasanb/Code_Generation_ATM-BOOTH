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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeSprint2IntegrationTest {

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

	@Test
	void listCustomersWithoutJwtIsUnauthorized() throws Exception {
		mockMvc.perform(get("/users")).andExpect(status().isUnauthorized());
	}

	@Test
	void listCustomersAsCustomerIsForbidden() throws Exception {
		mockMvc.perform(get("/users")
				.header("Authorization", "Bearer " + customerJwt()))
			.andExpect(status().isForbidden());
	}

	@Test
	void customerCannotCreateAccounts() throws Exception {
		String createAccountsBody = objectMapper.writeValueAsString(Map.of(
			"customerRegistrationId", 1L,
			"dailyOutgoingTransferLimit", new BigDecimal("100.00"),
			"minimumAllowedBalance", new BigDecimal("0")
		));
		mockMvc.perform(post("/accounts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(createAccountsBody)
				.header("Authorization", "Bearer " + customerJwt()))
			.andExpect(status().isForbidden());
	}

	@Test
	void employeeCanListCustomersWithHasAccountFilter() throws Exception {
		mockMvc.perform(get("/users")
				.param("hasAccount", "false")
				.param("size", "50")
				.header("Authorization", "Bearer " + employeeJwt()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isArray());
	}

	@Test
	void registerThenCreateAccountsThenCloseOneAccount() throws Exception {
		String uniqueEmail = "pending.sprint2." + System.nanoTime() + "@example.com";
		String registerRequestBody = objectMapper.writeValueAsString(Map.of(
			"firstName", "Pat",
			"lastName", "Pending",
			"email", uniqueEmail,
			"password", "Password123!",
			"bsnNumber", "123456782",
			"phoneNumber", "+31 6 00000000"
		));
		String registerResponseBody = mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(registerRequestBody))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		long newCustomerId = objectMapper.readTree(registerResponseBody).get("id").asLong();

		mockMvc.perform(get("/users")
				.param("hasAccount", "false")
				.param("size", "200")
				.header("Authorization", "Bearer " + employeeJwt()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[?(@.email == '" + uniqueEmail + "')]").exists());

		String createAccountsBody = objectMapper.writeValueAsString(Map.of(
			"customerRegistrationId", newCustomerId,
			"dailyOutgoingTransferLimit", new BigDecimal("2500.00"),
			"minimumAllowedBalance", new BigDecimal("-500.00")
		));
		String createAccountsResponseBody = mockMvc.perform(post("/accounts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(createAccountsBody)
				.header("Authorization", "Bearer " + employeeJwt()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.createdAccounts", hasSize(2)))
			.andExpect(jsonPath("$.customerApprovalStatus").value("APPROVED"))
			.andReturn()
			.getResponse()
			.getContentAsString();

		JsonNode createAccountsJson = objectMapper.readTree(createAccountsResponseBody);
		String ibanToClose = createAccountsJson.get("createdAccounts").get(0).get("iban").asText();

		mockMvc.perform(get("/users")
				.param("hasAccount", "false")
				.param("size", "300")
				.header("Authorization", "Bearer " + employeeJwt()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[?(@.email == '" + uniqueEmail + "')]").isEmpty());

		mockMvc.perform(put("/accounts/" + ibanToClose + "/close")
				.header("Authorization", "Bearer " + employeeJwt()))
			.andExpect(status().isNoContent());

		String directoryAfterClose = mockMvc.perform(get("/users")
				.param("size", "400")
				.header("Authorization", "Bearer " + employeeJwt()))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		JsonNode directoryRoot = objectMapper.readTree(directoryAfterClose);
		for (JsonNode customerRow : directoryRoot.path("content")) {
			if (customerRow.path("id").asLong() != newCustomerId) {
				continue;
			}
			for (JsonNode accountNode : customerRow.path("accounts")) {
				if (ibanToClose.equals(accountNode.path("iban").asText())) {
					assertFalse(accountNode.path("active").asBoolean());
					return;
				}
			}
		}
		throw new AssertionError("Closed IBAN not found in directory response");
	}

	@Test
	void denyPendingCustomerThenLoginReturnsForbidden() throws Exception {
		String uniqueEmail = "deny.test." + System.nanoTime() + "@example.com";
		String registerRequestBody = objectMapper.writeValueAsString(Map.of(
			"firstName", "Deny",
			"lastName", "Case",
			"email", uniqueEmail,
			"password", "Password123!",
			"bsnNumber", "123456785",
			"phoneNumber", "+31 6 11111111"
		));
		String registerResponseBody = mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(registerRequestBody))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		long customerId = objectMapper.readTree(registerResponseBody).get("id").asLong();

		mockMvc.perform(post("/auth/customers/" + customerId + "/deny")
				.header("Authorization", "Bearer " + employeeJwt()))
			.andExpect(status().isNoContent());

		String loginBody = objectMapper.writeValueAsString(Map.of("email", uniqueEmail, "password", "Password123!"));
		mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody))
			.andExpect(status().isForbidden());
	}
}
