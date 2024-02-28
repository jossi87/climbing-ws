package com.buldreinfo.jersey.jaxb.function;

import java.util.Optional;

import com.buldreinfo.jersey.jaxb.beans.Setup;

public interface FunctionDbUser<Connection, Response> {
	public Response get(Connection c, Setup setup, Optional<Integer> authUserId) throws Exception;
}