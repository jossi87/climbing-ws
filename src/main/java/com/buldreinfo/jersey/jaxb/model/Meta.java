package com.buldreinfo.jersey.jaxb.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.db.Dao;

public record Meta(String title, boolean isAuthenticated, boolean isAdmin, boolean isSuperAdmin,
		String authenticatedName, MediaIdentity mediaIdentity,
		List<Grade> grades, List<Integer> faYears,
		int defaultZoom, LatLng defaultCenter,
		boolean isBouldering, boolean isClimbing, boolean isIce, String url,
		List<Type> types, List<Region> regions, List<CompassDirection> compassDirections) {
	
	public static Meta from(Dao dao, Connection c, Setup setup, Optional<Integer> authUserId) throws SQLException {
		String title = setup.title();
		boolean isAuthenticated = false;
		boolean isAdmin = false;
		boolean isSuperAdmin = false;
		String authenticatedName = null;
		MediaIdentity mediaIdentity = null;
		if (authUserId.isPresent()) {
			try (PreparedStatement ps = c.prepareStatement("""
					SELECT ur.admin_write, ur.superadmin_write, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) authenticated_name,
					       m.id media_id, UNIX_TIMESTAMP(m.updated_at) media_version_stamp, mma.focus_x media_focus_x, mma.focus_y media_focus_y
					FROM user u
					LEFT JOIN user_region ur ON (u.id=ur.user_id AND ur.region_id=?)
					LEFT JOIN media m ON u.media_id=m.id
					LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
					WHERE u.id=?
					""")) {
				ps.setInt(1, setup.idRegion());
				ps.setInt(2, authUserId.get());
				try (ResultSet rst = ps.executeQuery()) {
					while (rst.next()) {
						isAuthenticated = true;
						isAdmin = rst.getBoolean("admin_write");
						isSuperAdmin = rst.getBoolean("superadmin_write");
						if (isSuperAdmin) { // climbing-web often only checks for isAdmin
							isAdmin = true;
						}
						authenticatedName = rst.getString("authenticated_name");
						int mediaId = rst.getInt("media_id");
						if (mediaId > 0) {
							long mediaVersionStamp = rst.getLong("media_version_stamp");
							int mediaFocusX = rst.getInt("media_focus_x");
							int mediaFocusY = rst.getInt("media_focus_y");
							mediaIdentity = new MediaIdentity(mediaId, mediaVersionStamp, mediaFocusX, mediaFocusY);
						}
					}
				}
			}
		}
		List<Grade> grades = setup.gradeConverter().getGrades();
		List<Integer> faYears = dao.getFaYears(c, setup.idRegion());
		int defaultZoom = setup.defaultZoom();
		LatLng defaultCenter = setup.defaultCenter();
		String url = setup.url();
		List<Type> types = dao.getTypes(c, setup.idRegion());
		List<Region> regions = dao.getRegions(c, setup.idRegion());
		List<CompassDirection> compassDirections = setup.compassDirections();
		return new Meta(title, isAuthenticated, isAdmin, isSuperAdmin, authenticatedName, mediaIdentity, grades, faYears, defaultZoom, defaultCenter, setup.isBouldering(), setup.isClimbing(), setup.isIce(), url, types, regions, compassDirections);
	}
}