package com.buldreinfo.model;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.UserRepository;

public record Meta(
		String title,
		boolean isAuthenticated, boolean isAdmin, boolean isSuperAdmin,
		int userId, String authenticatedName, String themePreference, MediaIdentity mediaIdentity,
		List<Grade> grades, List<Integer> faYears,
		int defaultZoom, LatLng defaultCenter,
		boolean isBouldering, boolean isClimbing, boolean isIce, String url,
		List<Type> types, List<Region> regions, List<CompassDirection> compassDirections
		) {

	public static Meta from(Setup setup, Optional<Integer> authUserId, UserRepository userRepo, RegionRepository regionRepo) {
		var authUserFuture = CompletableFuture.supplyAsync(() -> userRepo.getAuthenticatedUser(setup, authUserId).orElse(null));
		var faYearsFuture = CompletableFuture.supplyAsync(() -> regionRepo.getFaYears(setup.idRegion()));
		var typesFuture = CompletableFuture.supplyAsync(() -> regionRepo.getTypes(setup.idRegion()));
		var regionsFuture = CompletableFuture.supplyAsync(() -> regionRepo.getRegions(setup.idRegion()));
		CompletableFuture.allOf(authUserFuture, faYearsFuture, typesFuture, regionsFuture).join();
		try {
			var authUser = Optional.ofNullable(authUserFuture.join());
			return new Meta(
					setup.title(),
					authUser.map(AuthenticatedUser::isAuthenticated).orElse(false),
					authUser.map(AuthenticatedUser::isAdmin).orElse(false),
					authUser.map(AuthenticatedUser::isSuperAdmin).orElse(false),
					authUser.map(AuthenticatedUser::userId).orElse(0),
					authUser.map(AuthenticatedUser::authenticatedName).orElse(null),
					authUser.map(AuthenticatedUser::themePreference).orElse(null),
					authUser.map(AuthenticatedUser::mediaIdentity).orElse(null),
					setup.gradeConverter().getGrades(),
					faYearsFuture.join(),
					setup.defaultZoom(),
					setup.defaultCenter(),
					setup.isBouldering(),
					setup.isClimbing(),
					setup.isIce(),
					setup.url(),
					typesFuture.join(),
					regionsFuture.join(),
					setup.compassDirections()
					);
		} catch (Exception e) {
			throw new RuntimeException("Meta construction failed", e);
		}
	}
}