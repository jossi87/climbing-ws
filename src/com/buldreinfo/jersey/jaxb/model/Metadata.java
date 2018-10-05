package com.buldreinfo.jersey.jaxb.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GradeHelper;
import com.buldreinfo.jersey.jaxb.metadata.beans.Setup;
import com.buldreinfo.jersey.jaxb.metadata.jsonld.JsonLd;

public class Metadata {
	private final String title;
	private final boolean isAuthenticated;
	private final boolean isAdmin;
	private final boolean isSuperAdmin;
	private final OpenGraph og;
	private final List<Grade> grades;
	private final boolean showLogoPlay;
	private final boolean showLogoSis;
	private final boolean showLogoBrv;
	private String description;
	private JsonLd jsonLd;
	private int defaultZoom;
	private LatLng defaultCenter;
	private final boolean isBouldering;
	private List<Type> types;
	private String canonical;

	public Metadata(DbConnection c, Setup setup, int authUserId, String subTitle, OpenGraph og) throws SQLException {
		this.title = setup.getTitle(subTitle);
		boolean isAuthenticated = false;
		boolean isAdmin = false;
		boolean isSuperAdmin = false;
		if (authUserId != -1) {
			PreparedStatement ps = c.getConnection().prepareStatement("SELECT auth.write FROM user u LEFT JOIN permission auth ON (u.id=auth.user_id AND auth.region_id=?) WHERE u.id=?");
			ps.setInt(1, setup.getIdRegion());
			ps.setInt(2, authUserId);
			ResultSet rst = ps.executeQuery();
			while (rst.next()) {
				int write = rst.getInt("write");
				isAuthenticated = true;
				isAdmin = write >= 1;
				isSuperAdmin = write == 2;
			}
			rst.close();
			ps.close();
		}
		this.isAuthenticated = isAuthenticated;
		this.isAdmin = isAdmin;
		this.isSuperAdmin = isSuperAdmin;
		this.og = og;
		List<Grade> grades = new ArrayList<>();
		Map<Integer, String> lookup = GradeHelper.getGrades(setup.getIdRegion());
		for (int id : lookup.keySet()) {
			grades.add(new Grade(id, lookup.get(id)));
		}
		this.grades = grades;
		this.showLogoPlay = setup.isShowLogoPlay();
		this.showLogoSis = setup.isShowLogoSis();
		this.showLogoBrv = setup.isShowLogoBrv();
		this.isBouldering = setup.isBouldering();
	}
	
	public String getCanonical() {
		return canonical;
	}
	
	public LatLng getDefaultCenter() {
		return defaultCenter;
	}
	
	public int getDefaultZoom() {
		return defaultZoom;
	}
	
	public String getDescription() {
		return description;
	}
	
	public List<Grade> getGrades() {
		return grades;
	}
	
	public JsonLd getJsonLd() {
		return jsonLd;
	}
	
	public OpenGraph getOg() {
		return og;
	}
	
	public String getTitle() {
		return title;
	}
	
	public List<Type> getTypes() {
		return types;
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

	public boolean isSuperAdmin() {
		return isSuperAdmin;
	}

	public Metadata setCanonical(String canonical) {
		this.canonical = canonical;
		return this;
	}
	
	public Metadata setDefaultCenter(LatLng defaultCenter) {
		this.defaultCenter = defaultCenter;
		return this;
	}
	
	public Metadata setDefaultZoom(int defaultZoom) {
		this.defaultZoom = defaultZoom;
		return this;
	}

	public Metadata setDescription(String description) {
		this.description = description;
		return this;
	}

	public Metadata setJsonLd(JsonLd jsonLd) {
		this.jsonLd = jsonLd;
		return this;
	}

	public Metadata setTypes(List<Type> types) {
		this.types = types;
		return this;
	}
}