package com.buldreinfo.jersey.jaxb.db;

import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 */
public class ConnectionPool {
	private static final String HOST = "172.104.157.185";
	private static final String DATABASE = "buldreinfo";
	private static final String USER = "buldreinfo";
	private static final String PASSWORD = "5459OoiwqQwerJgfg12_224WrejvJqGhhJ";
	private final BasicDataSource bds;
	
	public ConnectionPool() {
		this.bds = new BasicDataSource();
		this.bds.setDriverClassName("com.mysql.cj.jdbc.Driver");
		this.bds.setUrl(String.format("jdbc:mysql://%s/%s?user=%s&password=%s&serverTimezone=UTC", HOST, DATABASE, USER, PASSWORD));
	}
	
	protected BasicDataSource getBasicDataSource() throws SQLException {
		return bds;
	}
}