package com.buldreinfo.jersey.jaxb.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.buldreinfo.jersey.jaxb.beans.GradeSystem;
import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.db.Dao;

public record Meta(String title, boolean isAuthenticated, boolean isAdmin, boolean isSuperAdmin, String authenticatedName, List<Grade> grades, List<Integer> faYears,
		int defaultZoom, LatLng defaultCenter,
		boolean isBouldering, boolean isClimbing, boolean isIce, String url,
		List<Type> types, List<Site> sites, List<CompassDirection> compassDirections) {
	
	public static Meta from(Dao dao, Connection c, Setup setup, Optional<Integer> authUserId) throws SQLException {
		String title = setup.title();
		boolean isAuthenticated = false;
		boolean isAdmin = false;
		boolean isSuperAdmin = false;
		String authenticatedName = null;
		if (authUserId.isPresent()) {
			try (PreparedStatement ps = c.prepareStatement("SELECT ur.admin_write, ur.superadmin_write, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) names FROM user u LEFT JOIN user_region ur ON (u.id=ur.user_id AND ur.region_id=?) WHERE u.id=?")) {
				ps.setInt(1, setup.idRegion());
				ps.setInt(2, authUserId.get());
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						isAuthenticated = true;
						isAdmin = rst.getBoolean("admin_write");
						isSuperAdmin = rst.getBoolean("superadmin_write");
						if (isSuperAdmin) { // buldreinfo-web often only checks for isAdmin
							isAdmin = true;
						}
						authenticatedName = rst.getString("name");
					}
				}
			}
		}
		List<Grade> grades = setup.gradeConverter().getGrades();
		List<Integer> faYears = dao.getFaYears(c, setup.idRegion());
		int defaultZoom = setup.defaultZoom();
		LatLng defaultCenter = setup.defaultCenter();
		GradeSystem gradeSystem = setup.gradeSystem();
		boolean isBouldering = gradeSystem.equals(GradeSystem.BOULDER);
		boolean isClimbing = gradeSystem.equals(GradeSystem.CLIMBING);
		boolean isIce = gradeSystem.equals(GradeSystem.ICE);
		String url = setup.url();
		List<Type> types = dao.getTypes(c, setup.idRegion());
		List<Site> sites = dao.getSites(c, setup.idRegion());
		List<CompassDirection> compassDirections = setup.compassDirections();
		return new Meta(title, isAuthenticated, isAdmin, isSuperAdmin, authenticatedName, grades, faYears, defaultZoom, defaultCenter, isBouldering, isClimbing, isIce, url, types, sites, compassDirections);
	}
}