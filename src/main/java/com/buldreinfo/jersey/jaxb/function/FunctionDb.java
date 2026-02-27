package com.buldreinfo.jersey.jaxb.function;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.db.Dao;

public interface FunctionDb<Connection, Response> {
	public Response get(Dao dao, Connection c, Setup setup) throws Exception;
}