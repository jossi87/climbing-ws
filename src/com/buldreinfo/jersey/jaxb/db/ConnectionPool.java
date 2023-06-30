package com.buldreinfo.jersey.jaxb.db;

import java.sql.SQLException;

import org.apache.commons.dbcp2.BasicDataSource;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 */
public class ConnectionPool {
	private final BasicDataSource bds;
	
	public ConnectionPool() {
		String db = System.getenv("buldreinfo_db");
		Preconditions.checkNotNull(Strings.emptyToNull(db), "Could not find environment variable \"buldreinfo_db\". Expected a value with the following format: \"jdbc:mysql://HOST/DATABASE?user=USER&password=PASSWORD&serverTimezone=UTC\"");
        this.bds = new BasicDataSource();
		this.bds.setDriverClassName("com.mysql.cj.jdbc.Driver");
		this.bds.setUrl(db);
		this.bds.setMaxTotal(64);
	}
	
	protected BasicDataSource getBasicDataSource() throws SQLException {
		return bds;
	}
}