package com.buldreinfo.jersey.jaxb.function;

import java.util.Optional;

public interface FunctionDbUser<Connection, Response> {
	public Response get(Connection c, Optional<Integer> authUserId) throws Exception;
}