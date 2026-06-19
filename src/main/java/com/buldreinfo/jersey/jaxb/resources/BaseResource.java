package com.buldreinfo.jersey.jaxb.resources;

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

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.dao.RegionRepository;
import com.buldreinfo.jersey.jaxb.dao.UserRepository;
import com.buldreinfo.jersey.jaxb.filters.HitTrackingFilter;
import com.buldreinfo.jersey.jaxb.helpers.AuthHelper;
import com.buldreinfo.jersey.jaxb.infrastructure.OpenApiConstants;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public abstract class BaseResource {
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
	private final RegionRepository regionRepo;
	private final TransactionManager txManager;
	private final UserRepository userRepo;

	protected BaseResource(TransactionManager txManager, RegionRepository regionRepo, UserRepository userRepo) {
		this.txManager = txManager;
		this.regionRepo = regionRepo;
		this.userRepo = userRepo;
	}

	protected Response createBadRequestResponse(String message) {
		return Response.status(Response.Status.BAD_REQUEST)
				.type(MediaType.APPLICATION_JSON)
				.entity(Map.of("error", message))
				.build();
	}

	protected Response createNotFoundResponse() {
		return Response.status(Response.Status.NOT_FOUND)
				.entity(OpenApiConstants.NOT_FOUND_DESCRIPTION)
				.build();
	}
	
	protected <T> T executeAuthenticatedTask(HttpServletRequest request, AuthFunction<T> task) throws Exception {
	    Setup setup = getSetup(request);
	    return executeTask(() -> {
	        Optional<Integer> authUserId = authHelper.getAuthUserId(userRepo, request, setup);
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
				return ScopedValue.where(TransactionManager.ACTIVE_CONNECTION, c)
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