package com.buldreinfo.jersey.jaxb.server;

public interface FunctionDb<Connection, Response> {
	public Response get(Connection c) throws Exception;
}