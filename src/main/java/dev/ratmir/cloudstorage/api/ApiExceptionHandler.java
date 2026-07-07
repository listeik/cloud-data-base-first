package dev.ratmir.cloudstorage.api;

import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(FeatureNotImplementedException.class)
	ResponseEntity<ErrorResponse> handleNotImplemented(FeatureNotImplementedException exception) {
		return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(ErrorResponse.of(exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException exception) {
		var message = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return ResponseEntity.badRequest().body(ErrorResponse.of(message));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ErrorResponse> handleInvalidParameter(ConstraintViolationException exception) {
		return ResponseEntity.badRequest().body(ErrorResponse.of(exception.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.badRequest().body(ErrorResponse.of(exception.getMessage()));
	}

	@ExceptionHandler(ResponseStatusException.class)
	ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException exception) {
		var message = exception.getReason() == null ? exception.getStatusCode().toString() : exception.getReason();
		return ResponseEntity.status(exception.getStatusCode()).body(ErrorResponse.of(message));
	}
}
