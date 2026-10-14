package com.stockflow.user.controller;

import java.util.List;
import java.util.Set;

import com.stockflow.common.exception.DuplicateResourceException;
import com.stockflow.common.exception.ResourceNotFoundException;
import com.stockflow.common.web.GlobalExceptionHandler;
import com.stockflow.user.dto.UserCreateRequest;
import com.stockflow.user.dto.UserResponse;
import com.stockflow.user.dto.UserUpdateRequest;
import com.stockflow.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

	@Mock
	private UserService userService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = standaloneSetup(new UserController(userService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void findAllReturnsOk() throws Exception {
		when(userService.findAll()).thenReturn(List.of(response()));

		mockMvc.perform(get("/api/users"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].email").value("admin@example.com"))
				.andExpect(jsonPath("$[0].password").doesNotExist());
	}

	@Test
	void findByIdReturnsOk() throws Exception {
		when(userService.findById(1L)).thenReturn(response());

		mockMvc.perform(get("/api/users/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void findByIdReturns404WhenMissing() throws Exception {
		when(userService.findById(99L))
				.thenThrow(new ResourceNotFoundException("User not found with id: 99"));

		mockMvc.perform(get("/api/users/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void createReturns201() throws Exception {
		when(userService.create(any(UserCreateRequest.class))).thenReturn(response());

		mockMvc.perform(post("/api/users")
					.contentType(MediaType.APPLICATION_JSON)
					.content(createJson()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("admin@example.com"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void createRejectsInvalidPasswordAndEmptyRoles() throws Exception {
		mockMvc.perform(post("/api/users")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"firstName\":\"A\",\"lastName\":\"B\","
							+ "\"email\":\"admin@example.com\",\"password\":\"short\","
							+ "\"active\":true,\"roleNames\":[]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.password").exists())
				.andExpect(jsonPath("$.fieldErrors.roleNames").exists());
	}

	@Test
	void createReturns409ForDuplicateEmail() throws Exception {
		when(userService.create(any(UserCreateRequest.class)))
				.thenThrow(new DuplicateResourceException("User already exists"));

		mockMvc.perform(post("/api/users")
					.contentType(MediaType.APPLICATION_JSON)
					.content(createJson()))
				.andExpect(status().isConflict());
	}

	@Test
	void updateReturns200() throws Exception {
		when(userService.update(any(Long.class), any(UserUpdateRequest.class)))
				.thenReturn(response());

		mockMvc.perform(put("/api/users/1")
					.contentType(MediaType.APPLICATION_JSON)
					.content(updateJson()))
				.andExpect(status().isOk());
	}

	@Test
	void deleteReturns204AndDelegatesSoftDelete() throws Exception {
		mockMvc.perform(delete("/api/users/1"))
				.andExpect(status().isNoContent());

		verify(userService).delete(1L);
	}

	private UserResponse response() {
		return new UserResponse(
				1L, "Admin", "User", "admin@example.com", true, Set.of(), null, null
		);
	}

	private String createJson() {
		return "{\"firstName\":\"Admin\",\"lastName\":\"User\","
				+ "\"email\":\"admin@example.com\",\"password\":\"secret123\","
				+ "\"active\":true,\"roleNames\":[\"ADMIN\"]}";
	}

	private String updateJson() {
		return "{\"firstName\":\"Admin\",\"lastName\":\"Updated\","
				+ "\"email\":\"admin@example.com\",\"active\":true,"
				+ "\"roleNames\":[\"ADMIN\"]}";
	}
}
