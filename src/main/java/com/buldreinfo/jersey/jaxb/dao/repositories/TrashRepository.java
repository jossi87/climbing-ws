package com.buldreinfo.jersey.jaxb.dao.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.dao.Dao;
import com.buldreinfo.jersey.jaxb.model.Trash;

public record TrashRepository(Dao dao) {
	public List<Trash> getTrash(Connection c, Optional<Integer> authUserId, Setup setup) throws SQLException {
		dao.getRegionRepo().ensureAdminWriteRegion(c, setup, authUserId);
		List<Trash> res = new ArrayList<>();
		String sqlStr =
				// Area
				"SELECT a.id area_id, null sector_id, null problem_id, null media_id, a.name, DATE_FORMAT(a.trash,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by"
				+ " FROM (((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN user u ON a.trash_by=u.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE a.trash IS NOT NULL AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, null)=1"
				+ " GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by"
				// Sector
				+ " UNION ALL"
				+ " SELECT null area_id, s.id sector_id, null problem_id, null media_id, CONCAT(s.name,' (',a.name,')') name, DATE_FORMAT(s.trash,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by"
				+ " FROM ((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN user u ON s.trash_by=u.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE a.trash IS NULL AND s.trash IS NOT NULL AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, null)=1"
				+ " GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by"
				// Problem
				+ " UNION ALL"
				+ " SELECT null area_id, null sector_id, p.id problem_id, null media_id, CONCAT(p.name,' (',a.name,'/',s.name,')') name, DATE_FORMAT(p.trash,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by"
				+ " FROM (((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN user u ON p.trash_by=u.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE a.trash IS NULL AND s.trash IS NULL AND p.trash IS NOT NULL AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, null)=1"
				+ " GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by"
				// Media (Area)
				+ " UNION ALL"
				+ " SELECT a.id area_id, null sector_id, null problem_id, m.id media_id, a.name, DATE_FORMAT(m.deleted_timestamp,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by"
				+ " FROM (((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN media_area ma ON a.id=ma.area_id) INNER JOIN media m ON ma.media_id=m.id) INNER JOIN user u ON m.deleted_user_id=u.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE m.deleted_user_id IS NOT NULL AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, null)=1"
				+ " GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by"
				// Media (Sector)
				+ " UNION ALL"
				+ " SELECT null area_id, s.id sector_id, null problem_id, m.id media_id, CONCAT(s.name,' (',a.name,')') name, DATE_FORMAT(m.deleted_timestamp,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by"
				+ " FROM ((((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN media_sector ms ON s.id=ms.sector_id) INNER JOIN media m ON ms.media_id=m.id) INNER JOIN user u ON m.deleted_user_id=u.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE m.deleted_user_id IS NOT NULL AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, null)=1"
				+ " GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by"
				// Media (Problem)
				+ " UNION ALL"
				+ " SELECT null area_id, null sector_id, p.id problem_id, m.id media_id, CONCAT(p.name,' (',a.name,'/',s.name,')') name, DATE_FORMAT(m.deleted_timestamp,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by"
				+ " FROM (((((((region r INNER JOIN region_type rt ON r.id=rt.region_id) INNER JOIN area a ON r.id=a.region_id) INNER JOIN sector s ON a.id=s.area_id) INNER JOIN problem p ON s.id=p.sector_id) INNER JOIN media_problem mp ON p.id=mp.problem_id) INNER JOIN media m ON mp.media_id=m.id) INNER JOIN user u ON m.deleted_user_id=u.id) LEFT JOIN user_region ur ON (r.id=ur.region_id AND ur.user_id=?)"
				+ " WHERE m.deleted_user_id IS NOT NULL AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=?) AND (r.id=? OR ur.user_id IS NOT NULL) AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, null)=1"
				+ " GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by"
				// Order results
				+ " ORDER BY trash DESC";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElseThrow());
			ps.setInt(2, setup.idRegion());
			ps.setInt(3, setup.idRegion());
			ps.setInt(4, authUserId.orElseThrow());
			ps.setInt(5, setup.idRegion());
			ps.setInt(6, setup.idRegion());
			ps.setInt(7, authUserId.orElseThrow());
			ps.setInt(8, setup.idRegion());
			ps.setInt(9, setup.idRegion());
			ps.setInt(10, authUserId.orElseThrow());
			ps.setInt(11, setup.idRegion());
			ps.setInt(12, setup.idRegion());
			ps.setInt(13, authUserId.orElseThrow());
			ps.setInt(14, setup.idRegion());
			ps.setInt(15, setup.idRegion());
			ps.setInt(16, authUserId.orElseThrow());
			ps.setInt(17, setup.idRegion());
			ps.setInt(18, setup.idRegion());
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int areaId = rst.getInt("area_id");
					int sectorId = rst.getInt("sector_id");
					int problemId = rst.getInt("problem_id");
					int mediaId = rst.getInt("media_id");
					String name = rst.getString("name");
					String when = rst.getString("trash");
					String by = rst.getString("trash_by");
					res.add(new Trash(areaId, sectorId, problemId, mediaId, name, when, by));
				}
			}
		}
		return res;
	}
	
	public void trashRecover(Connection c, Setup setup, Optional<Integer> authUserId, int idArea, int idSector, int idProblem, int idMedia) throws SQLException {
		dao.getRegionRepo().ensureSuperadminWriteRegion(c, setup, authUserId);
		String sqlStr = null;
		int id = 0;
		// Important to check media first. A media in trash always has idArea, idSector or idProblem!
		if (idMedia > 0) {
			sqlStr = "UPDATE media SET deleted_user_id=NULL, deleted_timestamp=NULL WHERE id=?";
			id = idMedia;
		}
		else if (idArea > 0) {
			sqlStr = "UPDATE area SET trash=NULL, trash_by=NULL WHERE id=?";
			id = idArea;
		}
		else if (idSector > 0) {
			sqlStr = "UPDATE sector SET trash=NULL, trash_by=NULL WHERE id=?";
			id = idSector;
		}
		else if (idProblem > 0) {
			sqlStr = "UPDATE problem SET trash=NULL, trash_by=NULL WHERE id=?";
			id = idProblem;
		}
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, id);
			ps.execute();
		}
	}
}