package com.buldreinfo.jersey.jaxb.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GradeHelper;
import com.buldreinfo.jersey.jaxb.helpers.Setup;
import com.buldreinfo.jersey.jaxb.helpers.Setup.GRADE_SYSTEM;

public class Meta {
	private final String title;
	private final boolean isAuthenticated;
	private final boolean isAdmin;
	private final boolean isSuperAdmin;
	private final List<Grade> grades;
	private final int defaultZoom;
	private final LatLng defaultCenter;
	private final boolean isBouldering;
	private final boolean isClimbing;
	private final boolean isIce;
	private final String url;
	private final List<Type> types;

	public Meta(DbConnection c, Setup setup, int authUserId) throws SQLException {
		this.title = setup.getTitle();
		boolean isAuthenticated = false;
		boolean isAdmin = false;
		boolean isSuperAdmin = false;
		if (authUserId != -1) {
			PreparedStatement ps = c.getConnection().prepareStatement("SELECT ur.admin_write, ur.superadmin_write FROM user u LEFT JOIN user_region ur ON (u.id=ur.user_id AND ur.region_id=?) WHERE u.id=?");
			ps.setInt(1, setup.getIdRegion());
			ps.setInt(2, authUserId);
			ResultSet rst = ps.executeQuery();
			while (rst.next()) {
				isAuthenticated = true;
				isAdmin = rst.getBoolean("admin_write");
				isSuperAdmin = rst.getBoolean("superadmin_write");
				if (isSuperAdmin) { // buldreinfo-web often only checks for isAdmin
					isAdmin = true;
				}
			}
			rst.close();
			ps.close();
		}
		this.isAuthenticated = isAuthenticated;
		this.isAdmin = isAdmin;
		this.isSuperAdmin = isSuperAdmin;
		List<Grade> grades = new ArrayList<>();
		Map<Integer, String> lookup = GradeHelper.getGrades(setup);
		for (int id : lookup.keySet()) {
			grades.add(new Grade(id, lookup.get(id)));
		}
		this.grades = grades;
		this.defaultZoom = setup.getDefaultZoom();
		this.defaultCenter = setup.getDefaultCenter();
		GRADE_SYSTEM gradeSystem = setup.getGradeSystem();
		this.isBouldering = gradeSystem.equals(GRADE_SYSTEM.BOULDER);
		this.isClimbing = gradeSystem.equals(GRADE_SYSTEM.CLIMBING);
		this.isIce = gradeSystem.equals(GRADE_SYSTEM.ICE);
		this.url = setup.getUrl();
		this.types = c.getBuldreinfoRepo().getTypes(setup.getIdRegion());
	}
	
	public LatLng getDefaultCenter() {
		return defaultCenter;
	}
	
	public int getDefaultZoom() {
		return defaultZoom;
	}
	
	public List<Grade> getGrades() {
		return grades;
	}
	
	public String getTitle() {
		return title;
	}
	
	public List<Type> getTypes() {
		return types;
	}
	
	public String getUrl() {
		return url;
	}
	
	public boolean isAdmin() {
		return isAdmin;
	}
	
	public boolean isAuthenticated() {
		return isAuthenticated;
	}
	
	public boolean isBouldering() {
		return isBouldering;
	}
	
	public boolean isClimbing() {
		return isClimbing;
	}
	
	public boolean isIce() {
		return isIce;
	}
	
	public boolean isSuperAdmin() {
		return isSuperAdmin;
	}
}