package com.buldreinfo.jersey.jaxb.model;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.dao.RegionRepository;
import com.buldreinfo.jersey.jaxb.dao.UserRepository;
import com.buldreinfo.jersey.jaxb.infrastructure.FunctionalUtils;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;
import com.buldreinfo.jersey.jaxb.resources.BaseResource;

public record Meta(String title,
                   boolean isAuthenticated, boolean isAdmin, boolean isSuperAdmin,
                   int userId, String authenticatedName, String themePreference, MediaIdentity mediaIdentity,
                   List<Grade> grades, List<Integer> faYears,
                   int defaultZoom, LatLng defaultCenter,
                   boolean isBouldering, boolean isClimbing, boolean isIce, String url,
                   List<Type> types, List<Region> regions, List<CompassDirection> compassDirections) {

	public static Meta from(Setup setup, Optional<Integer> authUserId, TransactionManager txManager, UserRepository userRepo, RegionRepository regionRepo) {
	    var authUserFuture = CompletableFuture.supplyAsync(FunctionalUtils.transactional(txManager, () -> userRepo.getAuthenticatedUser(setup, authUserId).orElse(null)), BaseResource.executor);
	    var faYearsFuture = CompletableFuture.supplyAsync(FunctionalUtils.transactional(txManager, () -> regionRepo.getFaYears(setup.idRegion())), BaseResource.executor);
	    var typesFuture = CompletableFuture.supplyAsync(FunctionalUtils.transactional(txManager, () -> regionRepo.getTypes(setup.idRegion())), BaseResource.executor);
	    var regionsFuture = CompletableFuture.supplyAsync(FunctionalUtils.transactional(txManager, () -> regionRepo.getRegions(setup.idRegion())), BaseResource.executor);

	    CompletableFuture.allOf(authUserFuture, faYearsFuture, typesFuture, regionsFuture).join();

	    try {
	        var authUser = Optional.ofNullable(authUserFuture.get());
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
	            faYearsFuture.get(),
	            setup.defaultZoom(),
	            setup.defaultCenter(),
	            setup.isBouldering(),
	            setup.isClimbing(),
	            setup.isIce(),
	            setup.url(),
	            typesFuture.get(),
	            regionsFuture.get(),
	            setup.compassDirections()
	        );
	    } catch (Exception e) {
	        throw new RuntimeException("Meta construction failed", e);
	    }
	}
}