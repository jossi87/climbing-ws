package com.buldreinfo.model;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.GradeSystemRepository;
import com.buldreinfo.dao.MapRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.exception.InternalServerErrorException;

public record Meta(
		String title,
		boolean isAuthenticated, boolean isAdmin, boolean isSuperAdmin,
		int userId, String authenticatedName, String themePreference, String languagePreference, MediaIdentity mediaIdentity,
		List<GradeSystem> allGradeSystems, List<Integer> regionGradeSystemIds, List<Integer> activityGradeSystemIds,
		List<Integer> faYears,
		int defaultZoom, LatLng defaultCenter, String defaultLanguage,
		boolean isBouldering, boolean isClimbing, boolean isIce, String url,
		List<Type> types, List<Region> regions, List<Map> maps, List<CompassDirection> compassDirections, List<Language> languages
		) {

	/** Upper bound for the fan-out below: a request must fail instead of hanging forever when a query gets stuck. */
	private static final long FANOUT_TIMEOUT_SECONDS = 30;

	public static Meta from(Setup setup, Optional<Integer> authUserId, UserRepository userRepo, RegionRepository regionRepo, MapRepository mapRepo, GradeSystemRepository gradeSystemRepo) {
		// supplyAsync() without an executor runs on ForkJoinPool.commonPool(), which has availableProcessors - 1
		// workers and is shared with the pdf/image jobs. Blocking db work there starves the pool: when its few
		// workers are stuck, every /meta and /without-js request parks forever in join(). Use own virtual threads,
		// as AreaService and SectorService do.
		var executor = Executors.newVirtualThreadPerTaskExecutor();
		try {
			var authUserFuture = CompletableFuture.supplyAsync(() -> userRepo.getAuthenticatedUser(setup, authUserId).orElse(null), executor);
			var faYearsFuture = CompletableFuture.supplyAsync(() -> regionRepo.getFaYears(setup.idRegion()), executor);
			var typesFuture = CompletableFuture.supplyAsync(() -> regionRepo.getTypes(setup.idRegion(), setup.idLanguage()), executor);
			var regionsFuture = CompletableFuture.supplyAsync(() -> regionRepo.getRegions(setup.idRegion(), setup.idLanguage()), executor);
			var mapsFuture = CompletableFuture.supplyAsync(() -> mapRepo.getMaps(setup.idRegion()), executor);
			var languagesFuture = CompletableFuture.supplyAsync(() -> regionRepo.getLanguages(), executor);
			var allGradeSystemsFuture = CompletableFuture.supplyAsync(() -> gradeSystemRepo.getGradeSystems(setup.idLanguage()), executor);
			var activityGradeSystemIdsFuture = CompletableFuture.supplyAsync(
					() -> gradeSystemRepo.getActivityGradeSystemIds(setup.idRegion(), authUserId.orElse(0)), executor);
			awaitAll(CompletableFuture.allOf(authUserFuture, faYearsFuture, typesFuture, regionsFuture, mapsFuture, languagesFuture,
					allGradeSystemsFuture, activityGradeSystemIdsFuture));

			var authUser = Optional.ofNullable(authUserFuture.join());
			var regionGradeSystemIds = setup.regionGradeSystemIds();
			return new Meta(
					setup.title(),
					authUser.map(AuthenticatedUser::isAuthenticated).orElse(false),
					authUser.map(AuthenticatedUser::isAdmin).orElse(false),
					authUser.map(AuthenticatedUser::isSuperAdmin).orElse(false),
					authUser.map(AuthenticatedUser::userId).orElse(0),
					authUser.map(AuthenticatedUser::authenticatedName).orElse(null),
					authUser.map(AuthenticatedUser::themePreference).orElse(null),
					authUser.map(AuthenticatedUser::languagePreference).orElse(null),
					authUser.map(AuthenticatedUser::mediaIdentity).orElse(null),
					allGradeSystemsFuture.join(),
					regionGradeSystemIds,
					activityGradeSystemIdsFuture.join(),
					faYearsFuture.join(),
					setup.defaultZoom(),
					setup.defaultCenter(),
					setup.defaultLanguage(),
					setup.isBouldering(),
					setup.isClimbing(),
					setup.isIce(),
					setup.url(),
					typesFuture.join(),
					regionsFuture.join(),
					mapsFuture.join(),
					setup.compassDirections(),
					languagesFuture.join()
					);
		} finally {
			// Nothing is left running on the happy path; on a timeout this abandons the stuck tasks so the
			// request thread is released instead of waiting for them.
			executor.shutdownNow();
		}
	}

	private static void awaitAll(CompletableFuture<?> fanout) {
		try {
			fanout.get(FANOUT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			throw new InternalServerErrorException("Loading meta timed out after " + FANOUT_TIMEOUT_SECONDS + "s", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new InternalServerErrorException("Interrupted while loading meta", e);
		} catch (ExecutionException e) {
			throw new InternalServerErrorException("Failed to load meta", e.getCause() != null ? e.getCause() : e);
		}
	}
}