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
class AtmWithdrawIntegrationTest {

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
		String email = "atm.withdraw." + suffix + "@example.com";

		String registerRequestBody = objectMapper.writeValueAsString(Map.of(
			"firstName", "Atm",
			"lastName", "User" + suffix,
			"email", email,
			"password", "Password123!",
			"bsnNumber", "123456784",
			"phoneNumber", "+31 6 44444444"
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
			"minimumAllowedBalance", new BigDecimal("0.00")
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
	void withdrawWithoutJwtIsUnauthorized() throws Exception {
		String body = objectMapper.writeValueAsString(Map.of("amount", new BigDecimal("10.00")));
		mockMvc.perform(post("/atm/withdraw")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void customerCanWithdrawFromCheckingAccount() throws Exception {
		CustomerContext customer = registerCustomerWithAccounts();

		String withdrawBody = objectMapper.writeValueAsString(Map.of(
			"amount", new BigDecimal("50.00"),
			"fromIban", customer.checkingIban()
		));
		mockMvc.perform(post("/atm/withdraw")
				.contentType(MediaType.APPLICATION_JSON)
				.content(withdrawBody)
				.header("Authorization", "Bearer " + customer.jwt()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.amount").value(50.00))
			.andExpect(jsonPath("$.fromIban").value(customer.checkingIban()))
			.andExpect(jsonPath("$.newBalance").value(950.00))
			.andExpect(jsonPath("$.transactionId").exists());

		mockMvc.perform(get("/accounts/mine")
				.header("Authorization", "Bearer " + customer.jwt()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.combinedBalance").value(1950.00));
	}

	@Test
	void withdrawEnforcesDailyLimit() throws Exception {
		CustomerContext customer = registerCustomerWithAccounts();

		String firstWithdraw = objectMapper.writeValueAsString(Map.of(
			"amount", new BigDecimal("250.00"),
			"fromIban", customer.checkingIban()
		));
		mockMvc.perform(post("/atm/withdraw")
				.contentType(MediaType.APPLICATION_JSON)
				.content(firstWithdraw)
				.header("Authorization", "Bearer " + customer.jwt()))
			.andExpect(status().isCreated());

		String secondWithdraw = objectMapper.writeValueAsString(Map.of(
			"amount", new BigDecimal("100.00"),
			"fromIban", customer.checkingIban()
		));
		mockMvc.perform(post("/atm/withdraw")
				.contentType(MediaType.APPLICATION_JSON)
				.content(secondWithdraw)
				.header("Authorization", "Bearer " + customer.jwt()))
			.andExpect(status().isBadRequest());
	}

	@Test
	void withdrawValidatesAmount() throws Exception {
		CustomerContext customer = registerCustomerWithAccounts();
		mockMvc.perform(post("/atm/withdraw")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.header("Authorization", "Bearer " + customer.jwt()))
			.andExpect(status().isBadRequest());
	}
}
