package com.buldreinfo.jersey.jaxb.function;

public interface FunctionDb<Connection, Response> {
	public Response get(Connection c) throws Exception;
}