package com.buldreinfo.jersey.jaxb;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.config.BuldreinfoConfig;
import com.buldreinfo.jersey.jaxb.db.Dao;
import com.buldreinfo.jersey.jaxb.function.Consumer;
import com.buldreinfo.jersey.jaxb.function.Function;
import com.buldreinfo.jersey.jaxb.function.FunctionDb;
import com.buldreinfo.jersey.jaxb.function.FunctionDbUser;
import com.buldreinfo.jersey.jaxb.helpers.AuthHelper;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;

public class Server {
	private static Server server;
	private static Logger logger = LogManager.getLogger();
	
	public static List<Setup> getSetups() {
		return getServer().setups;
	}
	
	public static void runSql(Consumer<Connection> action) {
		Server server = getServer();
		try (Connection c = server.bds.getConnection()) {
			c.setAutoCommit(false);
			action.run(server.dao, c);
			if (c.getAutoCommit()) {
				logger.warn("AutoCommit=true on transaction");
			}
			else {
				c.commit();
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
	
	protected static Response buildResponseWithSql(HttpServletRequest request, FunctionDb<Connection, Response> function) {
		Server server = getServer();
		Setup setup = server.getSetup(request);
		try (Connection c = server.bds.getConnection()) {
			c.setAutoCommit(false);
			Response res = function.get(server.dao, c, setup);
			c.commit();
			return res;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
	}
	
	protected static Response buildResponseWithSqlAndAuth(HttpServletRequest request, FunctionDbUser<Connection, Response> function) {
		Server server = getServer();
		Setup setup = server.getSetup(request);
		try (Connection c = server.bds.getConnection()) {
			Optional<Integer> authUserId = server.auth.getAuthUserId(server.dao, c, request, setup);
			Response res = function.get(server.dao, c, setup, authUserId);
			c.commit();
			return res;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
	}
	
	private final BasicDataSource bds;
	private final Dao dao = new Dao();
	private final AuthHelper auth = new AuthHelper();
	private final List<Setup> setups = new ArrayList<>();
	
	private Server() {
		Stopwatch stopwatch = Stopwatch.createStarted();
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
		try (Connection c = bds.getConnection()) {
			setups.addAll(dao.getSetups(c));
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage(), e);
		}
		logger.debug("Server initialized in {}", stopwatch);
	}
	
	private Setup getSetup(HttpServletRequest request) {
		Preconditions.checkNotNull(request);
		Preconditions.checkNotNull(request.getServerName(), "Invalid request=" + request);
		final String serverName = request.getServerName().toLowerCase().replace("www.", "");
		return setups.stream()
				.filter(x -> serverName.equalsIgnoreCase(x.domain()))
				.findAny()
				.orElseThrow(() -> new RuntimeException("Invalid serverName=" + serverName));
	}
}