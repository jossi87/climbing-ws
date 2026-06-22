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
import java.util.concurrent.ThreadFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.MediaRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.filters.HitTrackingFilter;
import com.buldreinfo.helpers.AuthHelper;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.infrastructure.OpenApiConstants;
import com.buldreinfo.io.StorageManager;

import jakarta.servlet.http.HttpServletRequest;

public abstract class BaseController {
	@FunctionalInterface
	protected interface AuthFunction<T> {
		T apply(Setup setup, Optional<Integer> authUserId) throws Exception;
	}
	@FunctionalInterface
	protected interface SetupFunction<T> {
		T apply(Setup setup) throws Exception;
	}
	private static final ThreadFactory VIRTUAL_THREAD_FACTORY = Thread.ofVirtual().name("climbing-ws-", 0).factory();
	public static final Executor executor = Executors.newThreadPerTaskExecutor(VIRTUAL_THREAD_FACTORY);
	private final AuthHelper authHelper = new AuthHelper();
	private final StorageManager storage;
	private final MediaRepository mediaRepo;
	private final RegionRepository regionRepo;
	private final ClimbingTransactionManager txManager;
	private final UserRepository userRepo;

	protected BaseController(StorageManager storage, ClimbingTransactionManager txManager, MediaRepository mediaRepo, RegionRepository regionRepo, UserRepository userRepo) {
		this.storage = storage;
		this.txManager = txManager;
		this.mediaRepo = mediaRepo;
		this.regionRepo = regionRepo;
		this.userRepo = userRepo;
	}

	@SuppressWarnings("unchecked")
	protected <T> ResponseEntity<T> createBadRequestResponse(String message) {
		return (ResponseEntity<T>) ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(Map.of("error", message));
	}

	protected ResponseEntity<String> createNotFoundResponse() {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(OpenApiConstants.NOT_FOUND_DESCRIPTION);
	}

	protected <T> T executeAuthenticatedTask(HttpServletRequest request, AuthFunction<T> task) throws Exception {
		Setup setup = getSetup(request);
		return executeTask(() -> {
			Optional<Integer> authUserId = authHelper.getAuthUserId(storage, userRepo, mediaRepo, request, setup);
			return task.apply(setup, authUserId);
		});
	}

	protected <T> T executeSetupTask(HttpServletRequest request, SetupFunction<T> task) throws Exception {
		Setup setup = getSetup(request);
		return executeTask(() -> task.apply(setup));
	}

	protected <T> T executeTask(Callable<T> task) throws Exception {
		return txManager.executeInTransaction(task);
	}

	protected Setup getSetup(HttpServletRequest request) throws Exception {
		String serverName = request.getServerName().toLowerCase().replace("www.", "").replace("staging.", "");
		return executeTask(() -> regionRepo.getSetups()).stream()
				.filter(s -> s.domain().equalsIgnoreCase(serverName))
				.findFirst()
				.orElseThrow(() -> new NoSuchElementException("Invalid serverName=" + serverName));
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