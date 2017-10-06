package com.buldreinfo.jersey.jaxb.db;

import java.io.IOException;
import java.sql.SQLException;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 */
public class ConnectionPoolProvider {
	private static ConnectionPoolProvider cpp = new ConnectionPoolProvider();
	
	public static synchronized DbConnection startTransaction() throws SQLException, IOException {
		return new DbConnection(cpp.pool);
	}
	
	private final ConnectionPool pool;

	public ConnectionPoolProvider() {
		pool = new ConnectionPool();
	}
}