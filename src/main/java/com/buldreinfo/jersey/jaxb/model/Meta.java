package com.buldreinfo.jersey.jaxb.model;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.infrastructure.DatabaseContext;

public record Meta(String title,
		boolean isAuthenticated, boolean isAdmin, boolean isSuperAdmin,
		int userId, String authenticatedName, String themePreference, MediaIdentity mediaIdentity,
		List<Grade> grades, List<Integer> faYears,
		int defaultZoom, LatLng defaultCenter,
		boolean isBouldering, boolean isClimbing, boolean isIce, String url,
		List<Type> types, List<Region> regions, List<CompassDirection> compassDirections) {
	
	public static Meta from(Setup setup, Optional<Integer> authUserId) throws InterruptedException, ExecutionException {
	    var authUserFuture = DatabaseContext.submitDaoTask(d -> d.getUserRepo().getAuthenticatedUser(setup, authUserId));
	    var faYearsFuture = DatabaseContext.submitDaoTask(d -> d.getRegionRepo().getFaYears(setup.idRegion()));
	    var typesFuture = DatabaseContext.submitDaoTask(d -> d.getRegionRepo().getTypes(setup.idRegion()));
	    var regionsFuture = DatabaseContext.submitDaoTask(d -> d.getRegionRepo().getRegions(setup.idRegion()));
	    CompletableFuture.allOf(authUserFuture, faYearsFuture, typesFuture, regionsFuture).join();
	    var authUser = authUserFuture.get();
	    return new Meta(
	            setup.title(),
	            authUser.map(com.buldreinfo.jersey.jaxb.model.AuthenticatedUser::isAuthenticated).orElse(false),
	            authUser.map(com.buldreinfo.jersey.jaxb.model.AuthenticatedUser::isAdmin).orElse(false),
	            authUser.map(com.buldreinfo.jersey.jaxb.model.AuthenticatedUser::isSuperAdmin).orElse(false),
	            authUser.map(com.buldreinfo.jersey.jaxb.model.AuthenticatedUser::userId).orElse(0),
	            authUser.map(com.buldreinfo.jersey.jaxb.model.AuthenticatedUser::authenticatedName).orElse(null),
	            authUser.map(com.buldreinfo.jersey.jaxb.model.AuthenticatedUser::themePreference).orElse(null),
	            authUser.map(com.buldreinfo.jersey.jaxb.model.AuthenticatedUser::mediaIdentity).orElse(null),
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
	}
}