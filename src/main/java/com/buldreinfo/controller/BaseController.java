package com.buldreinfo.controller;

import java.sql.Connection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.filters.HitTrackingFilter;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.infrastructure.OpenApiConstants;

import jakarta.servlet.http.HttpServletRequest;

public abstract class BaseController {
	public record UserContext(Setup setup, Optional<Integer> authUserId) {}

	@FunctionalInterface
	protected interface ContextTask<T> {
		T apply(UserContext ctx) throws Exception;
	}

	@FunctionalInterface
	protected interface PublicTask<T> {
		T execute(Setup setup) throws Exception;
	}

	public static final Executor executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("climbing-ws-", 0).factory());
	private final RegionRepository regionRepo;
	private final ClimbingTransactionManager txManager;

	protected BaseController(ClimbingTransactionManager txManager, RegionRepository regionRepo) {
		this.txManager = txManager;
		this.regionRepo = regionRepo;
	}

	private Optional<Integer> getAuthenticatedUserId() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		return (auth != null && auth.getPrincipal() instanceof Integer userId) ? Optional.of(userId) : Optional.empty();
	}

	private Setup getSetup(HttpServletRequest request) throws Exception {
		String serverName = request.getServerName();
		return executeTask(regionRepo::getSetups).stream()
				.filter(s -> s.domain().equalsIgnoreCase(serverName))
				.findFirst()
				.orElseThrow(() -> new NoSuchElementException("Invalid serverName=" + serverName));
	}

	protected ResponseEntity<?> createBadRequestResponse(String message) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
	}

	protected ResponseEntity<String> createNotFoundResponse() {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(OpenApiConstants.NOT_FOUND_DESCRIPTION);
	}

	protected <T> T executeContextualTask(HttpServletRequest request, ContextTask<T> task) throws Exception {
		Setup setup = getSetup(request);
		Optional<Integer> authUserId = getAuthenticatedUserId();
		return executeTask(() -> task.apply(new UserContext(setup, authUserId)));
	}

	protected <T> T executePublicTask(HttpServletRequest request, PublicTask<T> task) throws Exception {
		return executeTask(() -> task.execute(getSetup(request)));
	}

	protected <T> T executeTask(Callable<T> task) throws Exception {
		return txManager.executeInTransaction(task);
	}

	protected boolean isHitTrackingEnabled(HttpServletRequest request) {
		Object attr = request.getAttribute(HitTrackingFilter.SHOULD_UPDATE_HITS_KEY);
		return (attr instanceof Boolean) && (Boolean) attr;
	}

	protected <T> CompletableFuture<T> supplyAsync(Callable<T> task) {
		return CompletableFuture.supplyAsync(() -> {
			try (Connection c = txManager.getNewConnection()) {
				c.setAutoCommit(false);
				return ScopedValue.where(ClimbingTransactionManager.ACTIVE_CONNECTION, c)
						.call(() -> {
							try {
								T result = task.call();
								c.commit();
								return result;
							} catch (Exception e) {
								c.rollback();
								throw e;
							}
						});
			} catch (Exception e) {
				throw new CompletionException(e);
			}
		}, executor);
	}
}