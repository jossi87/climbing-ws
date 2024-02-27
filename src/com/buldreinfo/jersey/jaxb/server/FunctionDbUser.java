package com.buldreinfo.jersey.jaxb.server;

import java.util.Optional;

public interface FunctionDbUser<Connection, Response> {
	public Response get(Connection c, Optional<Integer> userId) throws Exception;
}