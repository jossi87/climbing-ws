package com.buldreinfo.jersey.jaxb.db;

import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 */
public class ConnectionPool {
	private static final String HOST = "192.168.0.3";
	private static final String DATABASE = "buldreinfo";
	private static final String USER = "map";
	private static final String PASSWORD = "Fasdg32ad#!\"dfawrqQQq";
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