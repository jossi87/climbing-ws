package com.buldreinfo.jersey.jaxb;

import java.sql.SQLException;
import java.util.NoSuchElementException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
	private static Logger logger = LogManager.getLogger();

	@Override
	public Response toResponse(Throwable e) {
		logger.error(e.getMessage(), e);
		if (e instanceof WebApplicationException wae) {
			Response response = wae.getResponse();
			if (response != null) {
				return response;
			}
		}
		return switch (e) {
		case IllegalArgumentException iae -> Response.status(Response.Status.BAD_REQUEST).entity(getBadRequestMessage(iae)).build();
		case NoSuchElementException _ -> Response.status(Response.Status.NOT_FOUND).entity("Not found").build();
		case SQLException _ -> Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Database error occurred").build();
		default -> Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An unexpected error occurred").build();
		};
	}

	private String getBadRequestMessage(IllegalArgumentException e) {
		if (e != null && e.getMessage() != null && e.getMessage().startsWith("File too large")) {
			return "File too large";
		}
		return "Invalid request parameters.";
	}
}
