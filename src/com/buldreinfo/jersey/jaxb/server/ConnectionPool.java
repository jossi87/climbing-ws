package com.buldreinfo.jersey.jaxb.db;

import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;

import com.buldreinfo.jersey.jaxb.config.BuldreinfoConfig;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 */
public class ConnectionPool {
	private final BasicDataSource bds;

	public ConnectionPool() {
		BuldreinfoConfig config = BuldreinfoConfig.getConfig();
		String hostname = config.getProperty(BuldreinfoConfig.PROPERTY_KEY_DB_HOSTNAME);
		String database = config.getProperty(BuldreinfoConfig.PROPERTY_KEY_DB_DATABASE);
		String username = config.getProperty(BuldreinfoConfig.PROPERTY_KEY_DB_USERNAME);
		String password = config.getProperty(BuldreinfoConfig.PROPERTY_KEY_DB_PASSWORD);
		String url = String.format("jdbc:mysql://%s/%s?user=%s&password=%s&serverTimezone=UTC", hostname, database, username, password);
		this.bds = new BasicDataSource();
		this.bds.setDriverClassName("com.mysql.cj.jdbc.Driver");
		this.bds.setUrl(url);
		this.bds.setMaxTotal(64);
	}

	protected BasicDataSource getBasicDataSource() throws SQLException {
		return bds;
	}
}