package com.buldreinfo.model;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.GradeSystemRepository;
import com.buldreinfo.dao.MapRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.UserRepository;

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

	public static Meta from(Setup setup, Optional<Integer> authUserId, UserRepository userRepo, RegionRepository regionRepo, MapRepository mapRepo, GradeSystemRepository gradeSystemRepo) {
		var authUserFuture = CompletableFuture.supplyAsync(() -> userRepo.getAuthenticatedUser(setup, authUserId).orElse(null));
		var faYearsFuture = CompletableFuture.supplyAsync(() -> regionRepo.getFaYears(setup.idRegion()));
		var typesFuture = CompletableFuture.supplyAsync(() -> regionRepo.getTypes(setup.idRegion(), setup.idLanguage()));
		var regionsFuture = CompletableFuture.supplyAsync(() -> regionRepo.getRegions(setup.idRegion(), setup.idLanguage()));
		var mapsFuture = CompletableFuture.supplyAsync(() -> mapRepo.getMaps(setup.idRegion()));
		var languagesFuture = CompletableFuture.supplyAsync(() -> regionRepo.getLanguages());
		var allGradeSystemsFuture = CompletableFuture.supplyAsync(() -> gradeSystemRepo.getGradeSystems(setup.idLanguage()));
		var activityGradeSystemIdsFuture = CompletableFuture.supplyAsync(
				() -> gradeSystemRepo.getActivityGradeSystemIds(setup.idRegion(), authUserId.orElse(0)));
		CompletableFuture.allOf(authUserFuture, faYearsFuture, typesFuture, regionsFuture, mapsFuture, languagesFuture,
				allGradeSystemsFuture, activityGradeSystemIdsFuture).join();
		try {
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
		} catch (Exception e) {
			throw new RuntimeException("Meta construction failed", e);
		}
	}
}