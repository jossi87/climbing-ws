package com.buldreinfo.jersey.jaxb.function;

import java.util.Optional;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.db.Dao;

public interface FunctionDbUser<Connection, Response> {
	public Response get(Dao dao, Connection c, Setup setup, Optional<Integer> authUserId) throws Exception;
}