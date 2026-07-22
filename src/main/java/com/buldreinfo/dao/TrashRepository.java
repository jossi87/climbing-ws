package com.buldreinfo.dao;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.model.Trash;

@Repository
public class TrashRepository {
	private final JdbcClient jdbcClient;

	public TrashRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Transactional(readOnly = true)
	public List<Trash> getTrash(Optional<Integer> authUserId, Setup setup) {
		var sqlStr = """
				WITH req AS (
				    SELECT ? auth_user_id, ? region_id
				)
				-- Area
				SELECT a.id area_id, null sector_id, null problem_id, null media_id, a.name, DATE_FORMAT(a.trash,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by
				FROM req
				CROSS JOIN region r
				JOIN region_type rt ON r.id=rt.region_id
				JOIN area a ON r.id=a.region_id
				JOIN user u ON a.trash_by=u.id
				LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=req.auth_user_id
				WHERE a.trash IS NOT NULL 
				  AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=req.region_id) 
				  AND (r.id=req.region_id OR ur.user_id IS NOT NULL) 
				  AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by

				UNION ALL

				-- Sector
				SELECT null area_id, s.id sector_id, null problem_id, null media_id, CONCAT(s.name,' (',a.name,')') name, DATE_FORMAT(s.trash,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by
				FROM req
				CROSS JOIN region r
				JOIN region_type rt ON r.id=rt.region_id
				JOIN area a ON r.id=a.region_id
				JOIN sector s ON a.id=s.area_id
				JOIN user u ON s.trash_by=u.id
				LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=req.auth_user_id
				WHERE a.trash IS NULL 
				  AND s.trash IS NOT NULL 
				  AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=req.region_id) 
				  AND (r.id=req.region_id OR ur.user_id IS NOT NULL) 
				  AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0))
				GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by

				UNION ALL

				-- Problem
				SELECT null area_id, null sector_id, p.id problem_id, null media_id, CONCAT(p.name,' (',a.name,'/',s.name,')') name, DATE_FORMAT(p.trash,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by
				FROM req
				CROSS JOIN region r
				JOIN region_type rt ON r.id=rt.region_id
				JOIN area a ON r.id=a.region_id
				JOIN sector s ON a.id=s.area_id
				JOIN problem p ON s.id=p.sector_id
				JOIN user u ON p.trash_by=u.id
				LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=req.auth_user_id
				WHERE a.trash IS NULL 
				  AND s.trash IS NULL 
				  AND p.trash IS NOT NULL 
				  AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=req.region_id) 
				  AND (r.id=req.region_id OR ur.user_id IS NOT NULL) 
				  AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by

				UNION ALL

				-- Media (Area)
				SELECT a.id area_id, null sector_id, null problem_id, m.id media_id, a.name, DATE_FORMAT(m.deleted_timestamp,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by
				FROM req
				CROSS JOIN region r
				JOIN region_type rt ON r.id=rt.region_id
				JOIN area a ON r.id=a.region_id
				JOIN media_area ma ON a.id=ma.area_id
				JOIN media m ON ma.media_id=m.id
				JOIN user u ON m.deleted_user_id=u.id
				LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=req.auth_user_id
				WHERE m.deleted_user_id IS NOT NULL 
				  AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=req.region_id) 
				  AND (r.id=req.region_id OR ur.user_id IS NOT NULL) 
				  AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by

				UNION ALL

				-- Media (Sector)
				SELECT null area_id, s.id sector_id, null problem_id, m.id media_id, CONCAT(s.name,' (',a.name,')') name, DATE_FORMAT(m.deleted_timestamp,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by
				FROM req
				CROSS JOIN region r
				JOIN region_type rt ON r.id=rt.region_id
				JOIN area a ON r.id=a.region_id
				JOIN sector s ON a.id=s.area_id
				JOIN media_sector ms ON s.id=ms.sector_id
				JOIN media m ON ms.media_id=m.id
				JOIN user u ON m.deleted_user_id=u.id
				LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=req.auth_user_id
				WHERE m.deleted_user_id IS NOT NULL 
				  AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=req.region_id) 
				  AND (r.id=req.region_id OR ur.user_id IS NOT NULL) 
				  AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by

				UNION ALL

				-- Media (Problem)
				SELECT null area_id, null sector_id, p.id problem_id, m.id media_id, CONCAT(p.name,' (',a.name,'/',s.name,')') name, DATE_FORMAT(m.deleted_timestamp,'%Y.%m.%d-%k:%i:%s') trash, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) trash_by
				FROM req
				CROSS JOIN region r
				JOIN region_type rt ON r.id=rt.region_id
				JOIN area a ON r.id=a.region_id
				JOIN sector s ON a.id=s.area_id
				JOIN problem p ON s.id=p.sector_id
				JOIN media_problem mp ON p.id=mp.problem_id
				JOIN media m ON mp.media_id=m.id
				JOIN user u ON m.deleted_user_id=u.id
				LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=req.auth_user_id
				WHERE m.deleted_user_id IS NOT NULL 
				  AND rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=req.region_id) 
				  AND (r.id=req.region_id OR ur.user_id IS NOT NULL) 
				  AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				GROUP BY area_id, sector_id, problem_id, media_id, name, trash, trash_by

				ORDER BY trash DESC
				""";

		return jdbcClient.sql(sqlStr)
				.param(authUserId.orElseThrow())
				.param(setup.idRegion())
				.query((rs, _) -> new Trash(
						rs.getInt("area_id"),
						rs.getInt("sector_id"),
						rs.getInt("problem_id"),
						rs.getInt("media_id"),
						rs.getString("name"),
						rs.getString("trash"),
						rs.getString("trash_by")
						)).list();
	}

	@Transactional
	public void trashRecover(int idArea, int idSector, int idProblem, int idMedia) {
		long positiveCount = Stream.of(idArea, idSector, idProblem, idMedia)
				.filter(id -> id > 0)
				.count();

		if (positiveCount != 1) {
			throw new IllegalArgumentException("Exactly one parameter must be positive.");
		}

		String sqlStr = null;
		int id = 0;

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

		if (sqlStr != null) {
			jdbcClient.sql(sqlStr)
			.param(id)
			.update();
		}
	}
}