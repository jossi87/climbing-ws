package com.buldreinfo.jersey.jaxb;

import java.sql.Connection;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.config.BuldreinfoConfig;
import com.buldreinfo.jersey.jaxb.db.Dao;
import com.buldreinfo.jersey.jaxb.function.Consumer;
import com.buldreinfo.jersey.jaxb.function.Function;
import com.buldreinfo.jersey.jaxb.function.FunctionDb;
import com.buldreinfo.jersey.jaxb.function.FunctionDbUser;
import com.buldreinfo.jersey.jaxb.function.FunctionDsUser;
import com.buldreinfo.jersey.jaxb.helpers.AuthHelper;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;

public class Server {
	private static volatile Server server;
	private static Logger logger = LogManager.getLogger();

	public static Collection<Setup> getSetups() {
		return getServer().setupMap.values();
	}

	public static void runSql(Consumer<Connection> action) {
		Server server = getServer();
		try (Connection c = server.ds.getConnection()) {
			try {
				action.run(server.dao, c);
				if (!c.getAutoCommit()) {
					c.commit();
				}
			} catch (Exception e) {
				if (!c.getAutoCommit()) {
					c.rollback();
				}
				throw e;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	private static synchronized Server getServer() {
		Server result = server;
		if (result == null) {
			server = result = new Server();
		}
		return result;
	}

	protected static Response buildResponse(Function<Response> function) {
		try {
			return function.get();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
	}

	protected static Response buildResponseWithDsAndAuth(HttpServletRequest request, FunctionDsUser<HikariDataSource, Response> function) {
	    Server server = getServer();
	    Setup setup = server.getSetup(request);
	    try (Connection authConn = server.ds.getConnection()) {
	        Optional<Integer> authUserId = server.auth.getAuthUserId(server.dao, authConn, request, setup);
	        return function.get(server.dao, server.ds, setup, authUserId);
	    } catch (Exception e) {
	    	logger.error(e.getMessage(), e);
	        return Response.serverError().build();
	    }
	}
	
	protected static Response buildResponseWithSql(HttpServletRequest request, FunctionDb<Connection, Response> function) {
		Server server = getServer();
		Setup setup = server.getSetup(request);
		try (Connection c = server.ds.getConnection()) {
			try {
				Response res = function.get(server.dao, c, setup);
				c.commit();
				return res;
			} catch (Exception e) {
				c.rollback();
				throw e;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
	}

	protected static Response buildResponseWithSqlAndAuth(HttpServletRequest request, FunctionDbUser<Connection, Response> function) {
		Server server = getServer();
		Setup setup = server.getSetup(request);
		try (Connection c = server.ds.getConnection()) {
			try {
				Optional<Integer> authUserId = server.auth.getAuthUserId(server.dao, c, request, setup);
				Response res = function.get(server.dao, c, setup, authUserId);
				c.commit();
				return res;
			} catch (Exception e) {
				c.rollback();
				throw e;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
	}

	private final HikariDataSource ds;
	private final Dao dao = new Dao();
	private final AuthHelper auth = new AuthHelper();
	private final Map<String, Setup> setupMap = new ConcurrentHashMap<>();

	private Server() {
		Stopwatch stopwatch = Stopwatch.createStarted();
		BuldreinfoConfig config = BuldreinfoConfig.getConfig();
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setJdbcUrl(String.format("jdbc:mysql://%s/%s", 
				config.getProperty(BuldreinfoConfig.PROPERTY_KEY_DB_HOSTNAME),
				config.getProperty(BuldreinfoConfig.PROPERTY_KEY_DB_DATABASE)));
		hikariConfig.setUsername(config.getProperty(BuldreinfoConfig.PROPERTY_KEY_DB_USERNAME));
		hikariConfig.setPassword(config.getProperty(BuldreinfoConfig.PROPERTY_KEY_DB_PASSWORD));
		hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
		hikariConfig.setAutoCommit(false);
		hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
		hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
		hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
		hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
		hikariConfig.setMaximumPoolSize(25); 
		hikariConfig.setMinimumIdle(10);
		hikariConfig.setConnectionTimeout(10000);
		hikariConfig.setLeakDetectionThreshold(4000);
		this.ds = new HikariDataSource(hikariConfig);
		try (Connection c = ds.getConnection()) {
			dao.getSetups(c).forEach(s -> {
				setupMap.put(s.domain().toLowerCase(), s);
			});
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage(), e);
		}
		logger.info("Server initialized in {}", stopwatch);
	}

	private Setup getSetup(HttpServletRequest request) {
		Preconditions.checkNotNull(request);
		Preconditions.checkNotNull(request.getServerName(), "Invalid request=" + request);
		final String serverName = request.getServerName().toLowerCase().replace("www.", "");
		Setup setup = setupMap.get(serverName);
		if (setup == null) {
			throw new RuntimeException("Invalid serverName=" + serverName);
		}
		return setup;
	}
}