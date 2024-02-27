package com.buldreinfo.jersey.jaxb.server;

public interface Function<Response> {
	public Response get() throws Exception;
}