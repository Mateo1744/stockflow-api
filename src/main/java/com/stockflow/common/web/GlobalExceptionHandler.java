package com.stockflow.common.web;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.stockflow.common.exception.DuplicateResourceException;
import com.stockflow.common.exception.InsufficientStockException;
import com.stockflow.common.exception.InvalidOperationException;
import com.stockflow.common.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiErrorResponse> handleAuthentication(
			AuthenticationException exception,
			HttpServletRequest request
	) {
		return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials", request, Map.of());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiErrorResponse> handleAccessDenied(
			AccessDeniedException exception,
			HttpServletRequest request
	) {
		return buildResponse(HttpStatus.FORBIDDEN, "Access denied", request, Map.of());
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
			ResourceNotFoundException exception,
			HttpServletRequest request
	) {
		return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
	}

	@ExceptionHandler({
			DuplicateResourceException.class,
			InvalidOperationException.class,
			InsufficientStockException.class
	})
	public ResponseEntity<ApiErrorResponse> handleConflict(
			RuntimeException exception,
			HttpServletRequest request
	) {
		return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception,
			HttpServletRequest request
	) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors().forEach(error ->
				fieldErrors.putIfAbsent(
						error.getField(),
						error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()
				)
		);
		return buildResponse(
				HttpStatus.BAD_REQUEST,
				"Request validation failed",
				request,
				fieldErrors
		);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
			ConstraintViolationException exception,
			HttpServletRequest request
	) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		exception.getConstraintViolations().forEach(violation ->
				fieldErrors.putIfAbsent(
						violation.getPropertyPath().toString(),
						violation.getMessage()
				)
		);
		return buildResponse(
				HttpStatus.BAD_REQUEST,
				"Request validation failed",
				request,
				fieldErrors
		);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
			HttpMessageNotReadableException exception,
			HttpServletRequest request
	) {
		return buildResponse(
				HttpStatus.BAD_REQUEST,
				"Malformed JSON request",
				request,
				Map.of()
		);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
			HttpRequestMethodNotSupportedException exception,
			HttpServletRequest request
	) {
		return buildResponse(
				HttpStatus.METHOD_NOT_ALLOWED,
				"HTTP method not supported for this endpoint",
				request,
				Map.of()
		);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
			Exception exception,
			HttpServletRequest request
	) {
		return buildResponse(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"An unexpected error occurred",
				request,
				Map.of()
		);
	}

	private ResponseEntity<ApiErrorResponse> buildResponse(
			HttpStatus status,
			String message,
			HttpServletRequest request,
			Map<String, String> fieldErrors
	) {
		ApiErrorResponse response = new ApiErrorResponse(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				request.getRequestURI(),
				fieldErrors
		);
		return ResponseEntity.status(status).body(response);
	}
}
