package com.example.backend.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserDeletionIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private String loginToken(String email, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
		return body.get("token").asText();
	}

	@Test
	@Order(1)
	void employeeCanSoftDeletePendingCustomer() throws Exception {
		String employeeToken = loginToken("employee@inholland.nl", "Password123!");
		mockMvc.perform(delete("/users/6?permanent=false")
				.header("Authorization", "Bearer " + employeeToken))
				.andExpect(status().isNoContent());
	}

	@Test
	@Order(2)
	void employeeCanReactivateSoftDeletedCustomer() throws Exception {
		String employeeToken = loginToken("employee@inholland.nl", "Password123!");
		mockMvc.perform(delete("/users/7?permanent=false")
				.header("Authorization", "Bearer " + employeeToken))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/users/7/reactivate")
				.header("Authorization", "Bearer " + employeeToken))
				.andExpect(status().isNoContent());
	}
}
