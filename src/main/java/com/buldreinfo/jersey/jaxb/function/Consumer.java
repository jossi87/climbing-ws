package com.buldreinfo.jersey.jaxb.function;

import com.buldreinfo.jersey.jaxb.dao.Dao;

public interface Consumer<Connection> {
	public void run(Dao dao, Connection c) throws Exception;
}
