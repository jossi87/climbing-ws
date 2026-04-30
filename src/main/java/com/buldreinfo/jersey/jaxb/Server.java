package com.buldreinfo.jersey.jaxb;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import com.google.common.base.Stopwatch;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;

public class Server {
	@FunctionalInterface
	public interface DaoTask<T> {
	    T run(Dao dao, Connection c) throws SQLException;
	}
	public static final String HEADER_INTERNAL_REQUEST = "X-Internal-Request";
	public static final String HEADER_INTERNAL_REQUEST_VALUE = "true";
	private static final long HITS_COOLDOWN_MILLIS = Duration.ofMinutes(30).toMillis();
	private static final int HITS_COOLDOWN_CACHE_MAX_SIZE = 200000;
	private static volatile Server server;
	private static Logger logger = LogManager.getLogger();

	private static final Map<String, Long> hitsCooldownMap = new ConcurrentHashMap<>();

	public static Collection<Setup> getSetups() {
		return getServer().setupMap.values();
	}

	public static void runAsync(Runnable action) {
		getServer().executor.submit(action);
	}

	public static <T> T runParallel(FunctionDbUser<Connection, T> coordinator) {
	    Server server = getServer();
	    try (Connection c = server.ds.getConnection()) {
	        Setup setup = null;
	        Optional<Integer> authUserId = Optional.empty();
	        return coordinator.get(server.dao, c, setup, authUserId, false);
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
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

	public static <T> CompletableFuture<T> submitDaoTask(DaoTask<T> task) {
	    Server server = getServer();
	    return CompletableFuture.supplyAsync(() -> {
	        try (Connection c = server.ds.getConnection()) {
	            T result = task.run(server.dao, c);
	            c.commit();
	            return result;
	        } catch (SQLException e) {
	            throw new CompletionException(e);
	        }
	    }, server.executor);
	}

	private static void cleanupHitsCooldownMap(long now) {
		if (hitsCooldownMap.size() < HITS_COOLDOWN_CACHE_MAX_SIZE) {
			return;
		}
		hitsCooldownMap.entrySet().removeIf(e -> now - e.getValue() >= HITS_COOLDOWN_MILLIS);
	}

	private static String getBadRequestMessage(IllegalArgumentException e) {
		if (e != null && e.getMessage() != null && e.getMessage().startsWith("File too large")) {
			return "File too large";
		}
		return "Invalid request parameters.";
	}
	
	private static String getHitsCooldownKey(HttpServletRequest request) {
		String ip = request.getHeader("CF-Connecting-IP");
		if (ip == null || ip.isBlank()) {
			ip = request.getHeader("X-Forwarded-For");
		}
		if (ip == null || ip.isBlank()) {
			ip = request.getHeader("X-Real-IP");
		}
		if (ip == null || ip.isBlank()) {
			ip = request.getRemoteAddr();
		}
		String uri = request.getRequestURI();
		String query = request.getQueryString();
		String ua = request.getHeader("User-Agent");
		return String.join("|",
				uri == null ? "" : uri,
				query == null ? "" : query,
				ip == null ? "" : ip,
				ua == null ? "" : ua);
	}

	private static synchronized Server getServer() {
		Server result = server;
		if (result == null) {
			server = result = new Server();
		}
		return result;
	}

	private static boolean isBotRequest(HttpServletRequest request) {
		String ua = request.getHeader("User-Agent");
		if (ua == null) {
			return false;
		}
		String x = ua.toLowerCase();
		return x.contains("bot")
				|| x.contains("crawler")
				|| x.contains("spider")
				|| x.contains("slurp")
				|| x.contains("bingpreview")
				|| x.contains("headless");
	}

	private static boolean shouldUpdateHits(HttpServletRequest request) {
		String internalHeader = request.getHeader(HEADER_INTERNAL_REQUEST);
		boolean isInternal = HEADER_INTERNAL_REQUEST_VALUE.equalsIgnoreCase(internalHeader);
		if (isInternal || isBotRequest(request)) {
			return false;
		}
		long now = System.currentTimeMillis();
		String key = getHitsCooldownKey(request);
		Long previous = hitsCooldownMap.putIfAbsent(key, now);
		if (previous == null) {
			cleanupHitsCooldownMap(now);
			return true;
		}
		if (now - previous >= HITS_COOLDOWN_MILLIS) {
			hitsCooldownMap.put(key, now);
			cleanupHitsCooldownMap(now);
			return true;
		}
		return false;
	}

	protected static Response buildResponse(Function<Response> function) {
		try {
			return function.get();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An unexpected error occurred").build();
		}
	}

	protected static Response buildResponseWithSql(HttpServletRequest request, FunctionDb<Connection, Response> function) {
		Server server = getServer();
		Setup setup = server.getSetup(request);
		boolean shouldUpdateHits = shouldUpdateHits(request);
		try (Connection c = server.ds.getConnection()) {
			try {
				Response res = function.get(server.dao, c, setup, shouldUpdateHits);
				c.commit();
				return res;
			} catch (Exception e) {
				c.rollback();
				throw e; // Caught by the outer block
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return switch (e) {
			case IllegalArgumentException iae -> Response.status(Response.Status.BAD_REQUEST).entity(getBadRequestMessage(iae)).build();
			case NoSuchElementException _ -> Response.status(Response.Status.NOT_FOUND).entity("Not found").build();
			case SQLException _ -> Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Database error occurred").build();
			default -> Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Service error").build();
			};
		}
	}

	protected static Response buildResponseWithSqlAndAuth(HttpServletRequest request, FunctionDbUser<Connection, Response> function) {
		Server server = getServer();
		Setup setup = server.getSetup(request);
		boolean shouldUpdateHits = shouldUpdateHits(request);
		try (Connection c = server.ds.getConnection()) {
			try {
				Optional<Integer> authUserId = server.auth.getAuthUserId(server.dao, c, request, setup);
				Response res = function.get(server.dao, c, setup, authUserId, shouldUpdateHits);
				c.commit();
				return res;
			} catch (Exception e) {
				c.rollback();
				throw e;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return switch (e) {
			case IllegalArgumentException iae -> Response.status(Response.Status.BAD_REQUEST).entity(getBadRequestMessage(iae)).build();
			case NoSuchElementException _ -> Response.status(Response.Status.NOT_FOUND).entity("Not found").build();
			case SQLException _ -> Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Database error occurred").build();
			default -> Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An unexpected error occurred").build();
			};
		}
	}

	protected static Response buildResponseWithSqlAndRequiredAuth(HttpServletRequest request, FunctionDbUser<Connection, Response> function) {
		Server server = getServer();
		Setup setup = server.getSetup(request);
		boolean shouldUpdateHits = shouldUpdateHits(request);
		try (Connection c = server.ds.getConnection()) {
			try {
				Optional<Integer> authUserId = server.auth.getAuthUserId(server.dao, c, request, setup);
				if (authUserId.isEmpty()) {
					c.rollback();
					return Response.status(Response.Status.UNAUTHORIZED).entity("Authentication required.").build();
				}
				Response res = function.get(server.dao, c, setup, authUserId, shouldUpdateHits);
				c.commit();
				return res;
			} catch (Exception e) {
				c.rollback();
				throw e;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return switch (e) {
			case IllegalArgumentException iae -> Response.status(Response.Status.BAD_REQUEST).entity(getBadRequestMessage(iae)).build();
			case NoSuchElementException _ -> Response.status(Response.Status.NOT_FOUND).entity("Not found").build();
			case SQLException _ -> Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Database error occurred").build();
			default -> Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An unexpected error occurred").build();
			};
		}
	}

	private final HikariDataSource ds;
	private final Dao dao = new Dao();
	private final AuthHelper auth = new AuthHelper();
	private final Map<String, Setup> setupMap = new ConcurrentHashMap<>();
	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
	
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
		// GROUP_CONCAT has a max length 1024 characters by default (getActivity needs more characters)
		hikariConfig.setConnectionInitSql("SET SESSION group_concat_max_len = 1000000");
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
		Objects.requireNonNull(request);
		Objects.requireNonNull(request.getServerName(), "Invalid request=" + request);
		final String serverName = request.getServerName().toLowerCase()
				.replace("www.", "")
				.replace("staging.", "");
		Setup setup = setupMap.get(serverName);
		if (setup == null) {
			throw new RuntimeException("Invalid serverName=" + serverName);
		}
		return setup;
	}
}