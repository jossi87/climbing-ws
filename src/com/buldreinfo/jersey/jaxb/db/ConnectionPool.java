package com.buldreinfo.jersey.jaxb.db;

import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 */
public class ConnectionPool {
	private static final String HOST = "127.0.0.1";
	private static final String DATABASE = "buldreinfo";
	private static final String USER = "buldreinfo";
	private static final String PASSWORD = "5459OoiwqQwerJgfg12_224WrejvJqGhhJ";
	private final BasicDataSource bds;
	
	public ConnectionPool() {
		this.bds = new BasicDataSource();
		this.bds.setDriverClassName("com.mysql.jdbc.Driver");
		this.bds.setUrl(String.format("jdbc:mysql://%s/%s?user=%s&password=%s", HOST, DATABASE, USER, PASSWORD));
	}
	
	protected BasicDataSource getBasicDataSource() throws SQLException {
		return bds;
	}
}