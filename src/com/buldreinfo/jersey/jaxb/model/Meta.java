package com.buldreinfo.jersey.jaxb.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.buldreinfo.jersey.jaxb.server.Server;
import com.buldreinfo.jersey.jaxb.server.Setup;
import com.buldreinfo.jersey.jaxb.server.Setup.GRADE_SYSTEM;

public record Meta(String title, boolean isAuthenticated, boolean isAdmin, boolean isSuperAdmin, List<Grade> grades, int defaultZoom, LatLng defaultCenter,
		boolean isBouldering, boolean isClimbing, boolean isIce, String url,
		List<Type> types, List<Site> sites, List<CompassDirection> compassDirections) {
	public static Meta from(Connection c, Setup setup, Optional<Integer> authUserId) throws SQLException {
		String title = setup.getTitle();
		boolean isAuthenticated = false;
		boolean isAdmin = false;
		boolean isSuperAdmin = false;
		if (authUserId.isPresent()) {
			try (PreparedStatement ps = c.prepareStatement("SELECT ur.admin_write, ur.superadmin_write FROM user u LEFT JOIN user_region ur ON (u.id=ur.user_id AND ur.region_id=?) WHERE u.id=?")) {
				ps.setInt(1, setup.getIdRegion());
				ps.setInt(2, authUserId.get());
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						isAuthenticated = true;
						isAdmin = rst.getBoolean("admin_write");
						isSuperAdmin = rst.getBoolean("superadmin_write");
						if (isSuperAdmin) { // buldreinfo-web often only checks for isAdmin
							isAdmin = true;
						}
					}
				}
			}
		}
		List<Grade> grades = setup.getGradeConverter().getGrades();
		int defaultZoom = setup.getDefaultZoom();
		LatLng defaultCenter = setup.getDefaultCenter();
		GRADE_SYSTEM gradeSystem = setup.getGradeSystem();
		boolean isBouldering = gradeSystem.equals(GRADE_SYSTEM.BOULDER);
		boolean isClimbing = gradeSystem.equals(GRADE_SYSTEM.CLIMBING);
		boolean isIce = gradeSystem.equals(GRADE_SYSTEM.ICE);
		String url = setup.getUrl();
		List<Type> types = Server.getDao().getTypes(c, setup.getIdRegion());
		List<Site> sites = Server.getDao().getSites(c, setup.getIdRegion());
		List<CompassDirection> compassDirections = setup.getCompassDirections();
		return new Meta(title, isAuthenticated, isAdmin, isSuperAdmin, grades, defaultZoom, defaultCenter, isBouldering, isClimbing, isIce, url, types, sites, compassDirections);
	}
}