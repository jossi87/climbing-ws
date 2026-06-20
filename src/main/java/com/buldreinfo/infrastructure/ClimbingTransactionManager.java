package com.buldreinfo.infrastructure;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;

import javax.sql.DataSource;

import org.springframework.stereotype.Component;

@Component
public class ClimbingTransactionManager {
	public static final ScopedValue<Connection> ACTIVE_CONNECTION = ScopedValue.newInstance();
    private final DataSource dataSource;

    public ClimbingTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    public <T> T executeInTransaction(Callable<T> action) throws Exception {
        try (Connection c = dataSource.getConnection()) {
            boolean initialAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                T result = ScopedValue.where(ACTIVE_CONNECTION, c).call(action::call);
                c.commit();
                return result;
            } catch (Exception e) {
                c.rollback();
                throw e; 
            } finally {
                c.setAutoCommit(initialAutoCommit);
            }
        }
    }

    public void executeInTransaction(Runnable action) throws Exception {
        executeInTransaction(() -> {
            action.run();
            return null;
        });
    }
    
    public Connection getConnection() {
        if (!ACTIVE_CONNECTION.isBound()) {
            throw new IllegalStateException("No active database transaction bound to the current thread context.");
        }
        return ACTIVE_CONNECTION.get();
    }

    public Connection getNewConnection() throws SQLException {
        return dataSource.getConnection();
    }
}