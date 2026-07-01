package com.buldreinfo.infrastructure;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;

@ControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger logger = LogManager.getLogger();

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleException(Exception e) {
		Throwable cause = unwrap(e);
		logger.error("Error occurred: {}", cause.getMessage(), cause);

		return switch (cause) {
		case ResponseStatusException rse -> ResponseEntity.status(rse.getStatusCode()).body(Map.of("error", rse.getReason()));
		case ValidationFailedException vfe -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", vfe.getMessage()));
		case IllegalArgumentException iae -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", getBadRequestMessage(iae)));
		case HttpMessageNotReadableException _ -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Malformed JSON request payload"));
		case NoSuchElementException _ -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Not found"));
		case DataAccessException _ -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Database error occurred"));
		case JsonProcessingException _ -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Internal JSON processing error"));
		default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred"));
		};
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Object> handleNotFound(@SuppressWarnings("unused") NoResourceFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "The requested resource was not found."));
	}

	private String getBadRequestMessage(IllegalArgumentException e) {
		if (e != null && e.getMessage() != null && e.getMessage().startsWith("File too large")) {
			return "File too large";
		}
		return "Invalid request parameters.";
	}

	private Throwable unwrap(Throwable e) {
		Throwable cause = e;
		while (cause.getCause() != null && 
				(cause instanceof CompletionException || 
						cause instanceof InvocationTargetException ||
						cause.getClass().getName().equals("java.util.concurrent.ExecutionException"))) {
			cause = cause.getCause();
		}
		return cause;
	}
}
