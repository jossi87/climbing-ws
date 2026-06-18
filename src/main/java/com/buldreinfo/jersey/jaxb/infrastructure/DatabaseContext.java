package com.buldreinfo.jersey.jaxb.infrastructure;

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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.config.BuldreinfoConfig;
import com.buldreinfo.jersey.jaxb.dao.Dao;
import com.buldreinfo.jersey.jaxb.helpers.AuthHelper;
import com.google.common.base.Stopwatch;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;

public class DatabaseContext {
	@FunctionalInterface
	public interface AuthenticatedDatabaseTransactionAction<R> {
		R execute(Dao dao, Setup setup, Optional<Integer> authUserId, boolean shouldUpdateHits) throws Exception;
	}

	@FunctionalInterface
	public interface DaoTask<T> {
		T run(Dao dao) throws Exception;
	}

	@FunctionalInterface
	public interface DatabaseTransactionAction<R> {
		R execute(Dao dao, Setup setup, boolean shouldUpdateHits) throws Exception;
	}

	@FunctionalInterface
	public interface ThrowingSupplier<T> {
		T get() throws Exception;
	}

	private interface ConnectionTask {
		Response run(DatabaseContext server, Connection c, Setup setup, boolean shouldUpdateHits) throws Exception;
	}

	private static class InstanceHolder {
		private static final DatabaseContext INSTANCE = new DatabaseContext();
	}

	public static final String HEADER_INTERNAL_REQUEST = "X-Internal-Request";
	public static final String HEADER_INTERNAL_REQUEST_VALUE = "true";
	private static final ScopedValue<Connection> ACTIVE_CONNECTION = ScopedValue.newInstance();
	private static final int HITS_COOLDOWN_CACHE_MAX_SIZE = 200000;
	private static final long HITS_COOLDOWN_MILLIS = Duration.ofMinutes(30).toMillis();
	private static final Map<String, Long> hitsCooldownMap = new ConcurrentHashMap<>();
	private static final Logger logger = LogManager.getLogger();

	public static Response buildResponse(ThrowingSupplier<Response> function) {
		try {
			return function.get();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An unexpected error occurred").build();
		}
	}

	public static Response buildResponseWithSql(HttpServletRequest request, DatabaseTransactionAction<Response> action) {
		return executeWithConnection(request, (server, c, setup, shouldUpdateHits) -> {
			return ScopedValue.where(ACTIVE_CONNECTION, c).call(() -> {
				Response res = action.execute(server.dao, setup, shouldUpdateHits);
				c.commit();
				return res;
			});
		});
	}

	public static Response buildResponseWithSqlAndAuth(HttpServletRequest request, AuthenticatedDatabaseTransactionAction<Response> action) {
		return executeWithConnection(request, (server, c, setup, shouldUpdateHits) -> {
			return ScopedValue.where(ACTIVE_CONNECTION, c).call(() -> {
				Optional<Integer> authUserId = server.auth.getAuthUserId(server.dao, request, setup);
				Response res = action.execute(server.dao, setup, authUserId, shouldUpdateHits);
				c.commit();
				return res;
			});
		});
	}

	public static Response buildResponseWithSqlAndRequiredAuth(HttpServletRequest request, AuthenticatedDatabaseTransactionAction<Response> action) {
		return executeWithConnection(request, (server, c, setup, shouldUpdateHits) -> {
			return ScopedValue.where(ACTIVE_CONNECTION, c).call(() -> {
				Optional<Integer> authUserId = server.auth.getAuthUserId(server.dao, request, setup);
				if (authUserId.isEmpty()) {
					return Response.status(Response.Status.UNAUTHORIZED).entity("Authentication required.").build();
				}
				Response res = action.execute(server.dao, setup, authUserId, shouldUpdateHits);
				c.commit();
				return res;
			});
		});
	}

	public static Connection getConnection() {
		if (!ACTIVE_CONNECTION.isBound()) {
			throw new IllegalStateException("No active database transaction bound to the current thread context.");
		}
		return ACTIVE_CONNECTION.get();
	}

	public static Executor getExecutor() {
		return getServer().executor;
	}

	public static Collection<Setup> getSetups() {
		DatabaseContext server = getServer();
		if (!server.initialized) {
			synchronized (server) {
				if (!server.initialized) {
					runSql(dao -> {
						try {
							dao.getRegionRepo().getSetups().forEach(s -> {
								server.setupMap.put(s.domain().toLowerCase(), s);
							});
						} catch (SQLException e) {
							throw new RuntimeException("Failed to load setups", e);
						}
					});
					server.initialized = true;
				}
			}
		}
		return server.setupMap.values();
	}

	public static void runAsync(Runnable action) {
		getServer().executor.submit(action);
	}

	public static void runSql(Consumer<Dao> action) {
		DatabaseContext server = getServer();
		try (Connection c = server.ds.getConnection()) {
			boolean initialAutoCommit = c.getAutoCommit();
			try {
				ScopedValue.where(ACTIVE_CONNECTION, c).call(() -> {
					action.accept(server.dao);
					return null;
				});
				if (!initialAutoCommit) c.commit();
			} catch (Exception e) {
				if (!initialAutoCommit) c.rollback();
				throw e;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static <T> CompletableFuture<T> submitDaoTask(DaoTask<T> task) {
		DatabaseContext server = getServer();
		return CompletableFuture.supplyAsync(() -> {
			try (Connection c = server.ds.getConnection()) {
				return ScopedValue.where(ACTIVE_CONNECTION, c).call(() -> {
					boolean isCommitted = false;
					try {
						T result = task.run(server.dao);
						c.commit();
						isCommitted = true;
						return result;
					} catch (Exception e) {
						if (!isCommitted) {
							try {
								c.rollback();
							} catch (SQLException ex) {
								logger.error("Rollback failed during submitDaoTask exception handling", ex);
							}
						}
						throw e;
					}
				});
			} catch (Exception e) {
				throw new CompletionException(e);
			}
		}, server.executor);
	}

	private static synchronized void cleanupHitsCooldownMap(long now) {
		if (hitsCooldownMap.size() < HITS_COOLDOWN_CACHE_MAX_SIZE) {
			return;
		}
		hitsCooldownMap.entrySet().removeIf(e -> now - e.getValue() >= HITS_COOLDOWN_MILLIS);
		if (hitsCooldownMap.size() >= HITS_COOLDOWN_CACHE_MAX_SIZE) {
			hitsCooldownMap.clear();
		}
	}

	private static Response executeWithConnection(HttpServletRequest request, ConnectionTask task) {
		DatabaseContext server = getServer();
		Setup setup = server.getSetup(request);
		boolean shouldUpdateHits = shouldUpdateHits(request);
		try (Connection c = server.ds.getConnection()) {
			boolean initialAutoCommit = c.getAutoCommit();
			try {
				return task.run(server, c, setup, shouldUpdateHits);
			} catch (Exception e) {
				if (!initialAutoCommit) {
					try {
						c.rollback();
					} catch (SQLException ex) {
						logger.error("Rollback failed during executeWithConnection exception handling", ex);
					}
				}
				throw e;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return toErrorResponse(e);
		}
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

	private static DatabaseContext getServer() {
		return InstanceHolder.INSTANCE;
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
			if (hitsCooldownMap.size() >= HITS_COOLDOWN_CACHE_MAX_SIZE) {
				runAsync(() -> cleanupHitsCooldownMap(now));
			}
			return true;
		}
		if (now - previous >= HITS_COOLDOWN_MILLIS) {
			if (hitsCooldownMap.replace(key, previous, now)) {
				runAsync(() -> cleanupHitsCooldownMap(now));
				return true;
			}
			return false;
		}
		return false;
	}

	protected static Response toErrorResponse(Exception e) {
		return switch (e) {
		case IllegalArgumentException iae -> Response.status(Response.Status.BAD_REQUEST).entity(getBadRequestMessage(iae)).build();
		case NoSuchElementException _ -> Response.status(Response.Status.NOT_FOUND).entity("Not found").build();
		case SQLException _ -> Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Database error occurred").build();
		default -> Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("An unexpected error occurred").build();
		};
	}

	private final AuthHelper auth = new AuthHelper();
	private final Dao dao = new Dao();
	private final HikariDataSource ds;
	private final ExecutorService executor;
	private boolean initialized = false;
	private final Map<String, Setup> setupMap = new ConcurrentHashMap<>();

	private DatabaseContext() {
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
		hikariConfig.setLeakDetectionThreshold(0);
		hikariConfig.setConnectionInitSql("SET SESSION group_concat_max_len = 1000000");
		this.ds = new HikariDataSource(hikariConfig);
		var threadFactory = Thread.ofVirtual().name("climbing-ws-", 0).factory();
		this.executor = Executors.newThreadPerTaskExecutor(threadFactory);
		logger.info("Server initialized in {}", stopwatch);
	}

	private Setup getSetup(HttpServletRequest request) {
		Objects.requireNonNull(request);
		Objects.requireNonNull(request.getServerName(), "Invalid request=" + request);
		final String serverName = request.getServerName().toLowerCase()
				.replace("www.", "")
				.replace("staging.", "");
		Setup setup = getSetups().stream()
				.filter(s -> s.domain().equalsIgnoreCase(serverName))
				.findFirst()
				.orElseThrow(() -> new NoSuchElementException("Invalid serverName=" + serverName));
		return setup;
	}
}