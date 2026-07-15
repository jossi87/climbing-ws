package com.buldreinfo.infrastructure;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleException(Exception e) {
		Throwable cause = unwrap(e);

		return switch (cause) {
		case ValidationFailedException vfe -> {
			logger.warn("Validation failed: {}", vfe.getMessage());
			yield jsonError(HttpStatus.BAD_REQUEST, vfe.getMessage());
		}
		case UnauthorizedException ue -> {
			logger.warn("Unauthorized access attempt: {}", ue.getMessage());
			yield jsonError(HttpStatus.UNAUTHORIZED, ue.getMessage());
		}
		case ForbiddenException fe -> {
			logger.warn("Forbidden access attempt: {}", fe.getMessage());
			yield jsonError(HttpStatus.FORBIDDEN, fe.getMessage());
		}
		case TooManyRequestsException tme -> {
			logger.warn("Too many requests: {}", tme.getMessage());
			yield jsonError(HttpStatus.TOO_MANY_REQUESTS, tme.getMessage());
		}
		case InternalServerErrorException ise -> {
			logger.error("Internal server error: {}", ise.getMessage(), ise);
			yield jsonError(HttpStatus.INTERNAL_SERVER_ERROR, ise.getMessage());
		}
		case NoSuchElementException nse -> {
			logger.warn("Resource element missing: {}", nse.getMessage());
			yield jsonError(HttpStatus.NOT_FOUND, nse.getMessage());
		}
		case NoResourceFoundException _ -> {
			logger.warn("Resource path not found");
			yield jsonError(HttpStatus.NOT_FOUND, "The requested resource was not found.");
		}
		case MissingServletRequestParameterException ex -> {
			logger.warn("Missing parameter: {}", ex.getParameterName());
			yield jsonError(HttpStatus.BAD_REQUEST, "Required parameter '" + ex.getParameterName() + "' is missing");
		}
		case MethodArgumentTypeMismatchException ex -> {
			logger.warn("Parameter type mismatch for parameter '{}'", ex.getName());
			yield jsonError(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + ex.getName() + "'");
		}
		case HttpMediaTypeNotAcceptableException _ -> {
			logger.warn("Media type not acceptable");
			yield jsonError(HttpStatus.NOT_ACCEPTABLE, "The requested media type is not acceptable");
		}
		case HttpRequestMethodNotSupportedException ex -> {
			logger.warn("Method not allowed: {}", ex.getMethod());
			yield jsonError(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint");
		}
		case HttpMessageNotReadableException _ -> {
			logger.warn("Malformed JSON payload received");
			yield jsonError(HttpStatus.BAD_REQUEST, "Malformed JSON request payload");
		}
		case DataAccessException dae -> {
			logger.error("Database tracking or data execution exception", dae);
			yield jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Database error occurred");
		}
		case JsonProcessingException jpe -> {
			logger.error("Jackson JSON mapping failed", jpe);
			yield jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal JSON processing error");
		}
		default -> {
			logger.error("Unhandled infrastructure or runtime exception: {}", cause.getMessage(), cause);
			yield jsonError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
		}
		};
	}

	private static ResponseEntity<Object> jsonError(HttpStatus status, String message) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return ResponseEntity.status(status).headers(headers).body(Map.of("error", message));
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