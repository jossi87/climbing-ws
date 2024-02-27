package com.buldreinfo.jersey.jaxb.server;

import java.sql.Connection;
import java.util.Optional;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.config.BuldreinfoConfig;
import com.buldreinfo.jersey.jaxb.helpers.MetaHelper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;

public class Server {
	private static Server server;
	private static Logger logger = LogManager.getLogger();
	
	public static Response buildResponse(Function<Response> function) {
		try {
			return function.get();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
	}
	
	public static Response buildResponseWithSql(FunctionDb<Connection, Response> function) {
		try (Connection c = getServer().bds.getConnection()) {
			c.setAutoCommit(false);
			Response res = function.get(c);
			c.commit();
			return res;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
	}
	
	public static Response buildResponseWithSqlAndAuth(HttpServletRequest request, FunctionDbUser<Connection, Response> function) {
		try (Connection c = getServer().bds.getConnection()) {
			// Always commit user to avoid multiple parallel transactions inserting the same user...
			Optional<Integer> authUserId = getServer().auth.getAuthUserId(c, request, MetaHelper.getMeta());
			// Now set auto commit to false for the rest of the transaction
			c.setAutoCommit(false); 
			Response res = function.get(c, authUserId);
			c.commit();
			return res;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.serverError().build();
		}
	}
	
	public static Dao getDao() {
		return getServer().dao;
	}
	
	public static void runSql(Consumer<Connection> action) {
		try (Connection c = getServer().bds.getConnection()) {
			c.setAutoCommit(false);
			action.run(c);
			c.commit();
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
	
	private final BasicDataSource bds;
	private final Dao dao = new Dao();
	private final AuthHelper auth = new AuthHelper();
	
	private Server() {
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
}