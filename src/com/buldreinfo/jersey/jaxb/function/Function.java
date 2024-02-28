package com.buldreinfo.jersey.jaxb.function;

public interface Function<Response> {
	public Response get() throws Exception;
}