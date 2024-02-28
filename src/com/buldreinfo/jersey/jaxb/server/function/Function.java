package com.buldreinfo.jersey.jaxb.server.function;

public interface Function<Response> {
	public Response get() throws Exception;
}