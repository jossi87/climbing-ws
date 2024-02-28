package com.buldreinfo.jersey.jaxb.function;

import com.buldreinfo.jersey.jaxb.beans.Setup;

public interface FunctionDb<Connection, Response> {
	public Response get(Connection c, Setup setup) throws Exception;
}