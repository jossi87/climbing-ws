package com.buldreinfo.jersey.jaxb.db;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 */
public class DbConnection implements AutoCloseable {
	private static final Logger logger = LogManager.getLogger();
	private Connection c;
	private boolean success = false;
	private final BuldreinfoRepository buldreinfoRepo;
	
	protected DbConnection(ConnectionPool cp) throws SQLException {
		this.c = cp.getBasicDataSource().getConnection();
		this.c.setAutoCommit(false);
		// Init repo
		this.buldreinfoRepo = new BuldreinfoRepository(this);
	}

	@Override
	public void close() throws Exception {
		if (this.c != null) {
			if (success) {
				this.c.commit();
				// logger.debug("commit done");
			}
			else {
				this.c.rollback();
				logger.debug("rollback done");
			}
			this.c.close();
			this.c = null;
			if (!success) {
				throw new SQLException("success = false, rollback done!");
			}
		}
	}
	
	public Connection getConnection() throws SQLException {
		if (success) {
			throw new SQLException("success=true (setSuccess() only in end of algorithm!)");
		}
		return c;
	}

	public BuldreinfoRepository getBuldreinfoRepo() {
		return buldreinfoRepo;
	}
	
	public void setSuccess() throws SQLException {
		if (success) {
			throw new SQLException("setSuccess() called two times! Check code, setSuccess() must be called one time in each transaction (end of algorithm)");
		}
		this.success = true;
	}

	@Override
	public String toString() {
		return "DbConnection [c=" + c + "]";
	}
}