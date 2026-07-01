package com.buldreinfo.infrastructure;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.buldreinfo.exception.ForbiddenException;
import com.buldreinfo.exception.InternalServerErrorException;
import com.buldreinfo.exception.TooManyRequestsException;
import com.buldreinfo.exception.UnauthorizedException;
import com.buldreinfo.exception.ValidationFailedException;
import com.fasterxml.jackson.core.JsonProcessingException;

@ControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger logger = LogManager.getLogger();

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleException(Exception e) {
		Throwable cause = unwrap(e);

		return switch (cause) {
		case ValidationFailedException vfe -> {
			logger.warn("Validation failed: {}", vfe.getMessage());
			yield ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", vfe.getMessage()));
		}
		case UnauthorizedException ue -> {
			logger.warn("Unauthorized access attempt: {}", ue.getMessage());
			yield ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ue.getMessage()));
		}
		case ForbiddenException fe -> {
			logger.warn("Forbidden access attempt: {}", fe.getMessage());
			yield ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", fe.getMessage()));
		}
		case TooManyRequestsException tme -> {
			logger.warn("Too many requests: {}", tme.getMessage());
			yield ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", tme.getMessage()));
		}
		case InternalServerErrorException ise -> {
			logger.error("Internal server error: {}", ise.getMessage(), ise);
			yield ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ise.getMessage()));
		}
		case NoSuchElementException nse -> {
			logger.warn("Resource element missing: {}", nse.getMessage());
			yield ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", nse.getMessage()));
		}
		case NoResourceFoundException _ -> {
			logger.warn("Resource path not found");
			yield ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "The requested resource was not found."));
		}
		case MissingServletRequestParameterException ex -> {
			logger.warn("Missing parameter: {}", ex.getParameterName());
			yield ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Required parameter '" + ex.getParameterName() + "' is missing"));
		}
		case MethodArgumentTypeMismatchException ex -> {
			logger.warn("Parameter type mismatch for parameter '{}'", ex.getName());
			yield ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid value for parameter '" + ex.getName() + "'"));
		}
		case HttpMediaTypeNotAcceptableException _ -> {
			logger.warn("Media type not acceptable");
			yield ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(Map.of("error", "The requested media type is not acceptable"));
		}
		case HttpRequestMethodNotSupportedException ex -> {
			logger.warn("Method not allowed: {}", ex.getMethod());
			yield ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Map.of("error", "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint"));
		}
		case HttpMessageNotReadableException _ -> {
			logger.warn("Malformed JSON payload received");
			yield ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Malformed JSON request payload"));
		}
		case DataAccessException dae -> {
			logger.error("Database tracking or data execution exception", dae);
			yield ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Database error occurred"));
		}
		case JsonProcessingException jpe -> {
			logger.error("Jackson JSON mapping failed", jpe);
			yield ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Internal JSON processing error"));
		}
		default -> {
			logger.error("Unhandled infrastructure or runtime exception: {}", cause.getMessage(), cause);
			yield ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred"));
		}
		};
	}

	private Throwable unwrap(Throwable e) {
		Throwable cause = e;
		while (cause.getCause() != null && 
				(cause instanceof CompletionException || 
						cause instanceof InvocationTargetException ||
						cause instanceof ExecutionException)) {
			cause = cause.getCause();
		}
		return cause;
	}
}