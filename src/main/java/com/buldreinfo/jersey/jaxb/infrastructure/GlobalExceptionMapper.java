package com.buldreinfo.jersey.jaxb.infrastructure;

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
		if (e instanceof Exception ex) {
			return DatabaseContext.toErrorResponse(ex);
		}
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An unexpected error occurred").build();
	}
}
