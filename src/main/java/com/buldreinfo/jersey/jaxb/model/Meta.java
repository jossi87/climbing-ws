package com.buldreinfo.jersey.jaxb.model;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.dao.Dao;

public record Meta(String title,
		boolean isAuthenticated, boolean isAdmin, boolean isSuperAdmin,
		int userId, String authenticatedName, String themePreference, MediaIdentity mediaIdentity,
		List<Grade> grades, List<Integer> faYears,
		int defaultZoom, LatLng defaultCenter,
		boolean isBouldering, boolean isClimbing, boolean isIce, String url,
		List<Type> types, List<Region> regions, List<CompassDirection> compassDirections) {
	
	public static Meta from(Dao dao, Connection c, Setup setup, Optional<Integer> authUserId) throws SQLException {
		var authUser = dao.getUserRepo().getAuthenticatedUser(c, setup, authUserId);
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
				dao.getRegionRepo().getFaYears(c, setup.idRegion()),
				setup.defaultZoom(),
				setup.defaultCenter(),
				setup.isBouldering(),
				setup.isClimbing(),
				setup.isIce(),
				setup.url(),
				dao.getRegionRepo().getTypes(c, setup.idRegion()),
				dao.getRegionRepo().getRegions(c, setup.idRegion()),
				setup.compassDirections()
		);
	}
}