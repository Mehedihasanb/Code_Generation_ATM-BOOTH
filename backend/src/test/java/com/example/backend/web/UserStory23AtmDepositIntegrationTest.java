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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserStory23AtmDepositIntegrationTest {

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

	private CustomerContext registerCustomerWithAccounts() throws Exception {
		String suffix = String.valueOf(System.nanoTime());
		String email = "atm.deposit." + suffix + "@example.com";

		String registerRequestBody = objectMapper.writeValueAsString(Map.of(
			"firstName", "Atm",
			"lastName", "Dep" + suffix,
			"email", email,
			"password", "Password123!",
			"bsnNumber", "123456785",
			"phoneNumber", "+31 6 55555555"
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
			"dailyOutgoingTransferLimit", new BigDecimal("300.00"),
			"minimumAllowedBalance", new BigDecimal("5000.00")
		));
		String createAccountsResponse = mockMvc.perform(post("/accounts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(createAccountsBody)
				.header("Authorization", "Bearer " + employeeJwt()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.createdAccounts", hasSize(2)))
			.andReturn()
			.getResponse()
			.getContentAsString();

		String checkingIban = null;
		for (JsonNode line : objectMapper.readTree(createAccountsResponse).get("createdAccounts")) {
			if ("CHECKING".equals(line.get("accountType").asText())) {
				checkingIban = line.get("iban").asText();
			}
		}

		return new CustomerContext(loginJsonWebToken(email, "Password123!"), checkingIban);
	}

	private record CustomerContext(String jwt, String checkingIban) {
	}

	@Test
	void depositWithoutJwtIsUnauthorized() throws Exception {
		String body = objectMapper.writeValueAsString(Map.of("amount", new BigDecimal("10.00")));
		mockMvc.perform(post("/atm/deposit")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void customerCanDepositToCheckingAccount() throws Exception {
		CustomerContext customer = registerCustomerWithAccounts();

		String depositBody = objectMapper.writeValueAsString(Map.of(
			"amount", new BigDecimal("50.00"),
			"toIban", customer.checkingIban()
		));
		mockMvc.perform(post("/atm/deposit")
				.contentType(MediaType.APPLICATION_JSON)
				.content(depositBody)
				.header("Authorization", "Bearer " + customer.jwt()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.amount").value(50.00))
			.andExpect(jsonPath("$.toIban").value(customer.checkingIban()))
			.andExpect(jsonPath("$.newBalance").value(1050.00))
			.andExpect(jsonPath("$.transactionId").exists());

		mockMvc.perform(get("/accounts/mine")
				.header("Authorization", "Bearer " + customer.jwt()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.combinedBalance").value(2050.00));
	}

	@Test
	void depositEnforcesAbsoluteLimit() throws Exception {
		CustomerContext customer = registerCustomerWithAccounts();

		String depositBody = objectMapper.writeValueAsString(Map.of(
			"amount", new BigDecimal("4500.00"),
			"toIban", customer.checkingIban()
		));
		mockMvc.perform(post("/atm/deposit")
				.contentType(MediaType.APPLICATION_JSON)
				.content(depositBody)
				.header("Authorization", "Bearer " + customer.jwt()))
			.andExpect(status().isBadRequest());
	}

	@Test
	void depositEnforcesDailyLimit() throws Exception {
		CustomerContext customer = registerCustomerWithAccounts();

		String firstDeposit = objectMapper.writeValueAsString(Map.of(
			"amount", new BigDecimal("250.00"),
			"toIban", customer.checkingIban()
		));
		mockMvc.perform(post("/atm/deposit")
				.contentType(MediaType.APPLICATION_JSON)
				.content(firstDeposit)
				.header("Authorization", "Bearer " + customer.jwt()))
			.andExpect(status().isCreated());

		String secondDeposit = objectMapper.writeValueAsString(Map.of(
			"amount", new BigDecimal("100.00"),
			"toIban", customer.checkingIban()
		));
		mockMvc.perform(post("/atm/deposit")
				.contentType(MediaType.APPLICATION_JSON)
				.content(secondDeposit)
				.header("Authorization", "Bearer " + customer.jwt()))
			.andExpect(status().isBadRequest());
	}

	@Test
	void depositValidatesAmount() throws Exception {
		CustomerContext customer = registerCustomerWithAccounts();
		mockMvc.perform(post("/atm/deposit")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.header("Authorization", "Bearer " + customer.jwt()))
			.andExpect(status().isBadRequest());
	}
}
