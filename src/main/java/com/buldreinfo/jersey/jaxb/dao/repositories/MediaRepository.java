package com.buldreinfo.jersey.jaxb.dao.repositories;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.imgscalr.Scalr.Rotation;

import com.buldreinfo.jersey.jaxb.beans.S3KeyGenerator;
import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.beans.StorageType;
import com.buldreinfo.jersey.jaxb.dao.Dao;
import com.buldreinfo.jersey.jaxb.dao.JdbcUtils;
import com.buldreinfo.jersey.jaxb.helpers.ApifyInstagramResolver;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.io.ImageHelper;
import com.buldreinfo.jersey.jaxb.io.StorageManager;
import com.buldreinfo.jersey.jaxb.io.VideoHelper;
import com.buldreinfo.jersey.jaxb.model.Media;
import com.buldreinfo.jersey.jaxb.model.Media.Association;
import com.buldreinfo.jersey.jaxb.model.Media.MediaProblem;
import com.buldreinfo.jersey.jaxb.model.MediaSvgElement;
import com.buldreinfo.jersey.jaxb.model.MediaSvgElementType;
import com.buldreinfo.jersey.jaxb.model.Svg;
import com.buldreinfo.jersey.jaxb.model.User;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.LocalizedObjectAnnotation;
import com.google.cloud.vision.v1.NormalizedVertex;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public record MediaRepository(Dao dao) {
	private static Logger logger = LogManager.getLogger();

	public int addMediaImage(Connection c, Optional<Integer> authUserId, Media m, StorageType storageType, Supplier<InputStream> inputStreamSupplier) throws Exception {
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
		Preconditions.checkNotNull(storageType, "StorageType is required");
		Preconditions.checkNotNull(inputStreamSupplier, "InputStreamSupplier is required");
		Preconditions.checkArgument(!storageType.isMovie(), "Use the video endpoints for video uploads");
		
		Association associations = m.ensureCorrectMediaAssociations(authUserId);
		int idMedia = insertMediaMetadata(c, authUserId.get(), m, storageType);
		saveMediaContext(c, idMedia, associations, m, false);
		if (associations == Association.PROBLEMS) {
			for (Media.MediaProblem problem : m.problems()) {
				dao.getActivityRepo().fillActivity(c, problem.problemId());
			}
		}
		try (InputStream is = inputStreamSupplier.get()) {
			byte[] bytes = StorageManager.getInstance().readBoundedStream(is);
			ImageHelper.saveImage(dao, c, idMedia, bytes);
		}
		return idMedia;
	}
	
	public int addMediaVideoEmbed(Connection c, Optional<Integer> authUserId, Media m, StorageType storageType) throws Exception {
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
		Association associations = m.ensureCorrectMediaAssociations(authUserId);
		int idMedia = insertMediaMetadata(c, authUserId.get(), m, storageType);
		saveMediaContext(c, idMedia, associations, m, false);
		if (associations == Association.PROBLEMS) {
			for (Media.MediaProblem problem : m.problems()) {
				dao.getActivityRepo().fillActivity(c, problem.problemId());
			}
		}
		return idMedia;
	}
	
	public int addMediaVideoPlaceholder(Connection c, Optional<Integer> authUserId, Media m, StorageType storageType) throws Exception {
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
		Association associations = m.ensureCorrectMediaAssociations(authUserId);
		int idMedia = insertMediaMetadata(c, authUserId.get(), m, storageType);
		saveMediaContext(c, idMedia, associations, m, false);
		if (associations == Association.PROBLEMS) {
			for (Media.MediaProblem problem : m.problems()) {
				dao.getActivityRepo().fillActivity(c, problem.problemId());
			}
		}
		return idMedia;
	}
	
	public void deleteMedia(Connection c, Optional<Integer> authUserId, int idMedia) throws SQLException {
		ensureMediaUploadedByMeOrConnectedToRegionWhereIAmAdmin(c, authUserId, idMedia);
		List<Integer> idProblems = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT problem_id FROM media_problem WHERE media_id=?")) {
			ps.setInt(1, idMedia);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					idProblems.add(rst.getInt("problem_id"));
				}
			}
		}
		try (PreparedStatement ps = c.prepareStatement("UPDATE media SET deleted_user_id=?, deleted_timestamp=NOW() WHERE id=?")) {
			ps.setInt(1, authUserId.orElseThrow());
			ps.setInt(2, idMedia);
			ps.execute();
		}

		for (int idProblem : idProblems) {
			dao.getActivityRepo().fillActivity(c, idProblem);
		}
	}
	
	public void deleteMediaAnalysis(Connection c, int idMedia) throws SQLException {
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_ml_label WHERE media_id=?")) {
			ps.setInt(1, idMedia);
			ps.executeUpdate();
		}
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_ml_object WHERE media_id=?")) {
			ps.setInt(1, idMedia);
			ps.executeUpdate();
		}
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_ml_analysis WHERE media_id=?")) {
			ps.setInt(1, idMedia);
			ps.executeUpdate();
		}
		logger.debug("Deleted existing AI analysis for idMedia={}", idMedia);
	}
	
	public int getDailyInstagramScrapeCount(Connection c, Optional<Integer> authUserId) throws SQLException {
		String sql = "SELECT COUNT(*) FROM instagram_scrape_log WHERE user_id = ? AND created_at >= NOW() - INTERVAL 1 DAY";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, authUserId.orElseThrow());
			try (ResultSet rst = ps.executeQuery()) {
				if (rst.next()) {
					return rst.getInt(1);
				}
			}
		}
		return 0;
	}
	
	public Media getMedia(Connection c, Optional<Integer> authUserId, int id) throws SQLException {
		String sql = """
				WITH req AS (
				    SELECT ? auth_user_id, ? media_id
				)
				SELECT m.id, m.uploader_user_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex,
				       m.description, m.width, m.height, m.is_movie, m.is_360, m.embed_url, m.thumbnail_seconds,
				       DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken,
				       p.id photographer_id, TRIM(CONCAT(p.firstname, ' ', COALESCE(p.lastname,''))) photographer_name,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT('id', tu.id, 'name', TRIM(CONCAT(tu.firstname, ' ', COALESCE(tu.lastname,'')))))
				           FROM media_user tmu
				           JOIN user tu ON tmu.user_id = tu.id
				           WHERE tmu.media_id = m.id
				       ) tagged_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT('areaId', ma.area_id, 'areaName', a.name, 'trivia', ma.trivia))
				           FROM media_area ma
				           JOIN area a ON ma.area_id = a.id
				           WHERE ma.media_id = m.id
				       ) areas_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT('areaId', a.id, 'areaName', a.name, 'sectorId', ms.sector_id, 'sectorName', s.name, 'trivia', ms.trivia))
				           FROM media_sector ms
				           JOIN sector s ON ms.sector_id = s.id
				           JOIN area a ON s.area_id = a.id
				           WHERE ms.media_id = m.id
				       ) sectors_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
				               'problemId', p.id, 'problemName', p.name, 'problemGrade', g.grade, 'problemPitch', mp.pitch,
				               'problemNumPitches', (SELECT COUNT(*) FROM problem_section ps WHERE ps.problem_id = p.id),
				               'milliseconds', mp.milliseconds, 'areaId', a.id, 'areaName', a.name, 'sectorId', s.id, 'sectorName', s.name, 'trivia', mp.trivia
				           ))
				           FROM media_problem mp
				           JOIN problem p ON mp.problem_id = p.id
				           JOIN sector s ON p.sector_id = s.id
				           JOIN area a ON s.area_id = a.id
				           JOIN grade g ON p.consensus_grade_id = g.id
				           LEFT JOIN user_region ur ON a.region_id = ur.region_id AND ur.user_id = req.auth_user_id
				           WHERE mp.media_id = m.id
				             AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				       ) problems_json,
				       (
						    SELECT JSON_ARRAYAGG(JSON_OBJECT(
						        'trailId', t9.id,
						        'trailTitle', t9.title,
						        'sectors', (
						            SELECT JSON_ARRAYAGG(JSON_OBJECT(
						                'areaId', a9_sub.id,
						                'areaName', a9_sub.name,
						                'sectorId', s9_sub.id,
						                'sectorName', s9_sub.name
						            ))
						            FROM sector_trail st9_sub
						            JOIN sector s9_sub ON st9_sub.sector_id = s9_sub.id
						            JOIN area a9_sub ON s9_sub.area_id = a9_sub.id
						            WHERE st9_sub.trail_id = t9.id
						        )
						    ))
						    FROM media_trail mt9
						    JOIN trail t9 ON mt9.trail_id = t9.id
						    WHERE mt9.media_id = m.id
						) trails_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
				               'id', id, 'path', path, 'rappelX', rappel_x, 'rappelY', rappel_y, 'rappelBolted', rappel_bolted
				           ))
				           FROM media_svg
				           WHERE media_id = m.id
				       ) svgs_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
				               'id', s3.id,
				               'problemId', p3.id,
				               'problemName', p3.name,
				               'problemGrade', CASE WHEN s3.pitch IS NULL OR s3.pitch = 0 THEN g3.grade ELSE COALESCE(g_sect3.grade, g3.grade) END,
				               'problemGradeColor', CASE WHEN s3.pitch IS NULL OR s3.pitch = 0 THEN clr3.hex_code ELSE COALESCE(clr_sect3.hex_code, clr3.hex_code) END,
				               'problemSubtype', ty3.subtype,
				               'nr', p3.nr,
				               'pitch', COALESCE(ps3.nr, 0),
				               'path', s3.path,
				               'hasAnchor', s3.has_anchor,
				               'texts', s3.texts,
				               'anchors', s3.anchors,
				               'tradBelayStations', s3.trad_belay_stations,
				               'primary', CASE WHEN p3.type_id IN (1,2) THEN true ELSE false END,
				               'ticked', CASE WHEN (SELECT 1 FROM tick tk3 WHERE tk3.problem_id = p3.id AND tk3.user_id = req.auth_user_id LIMIT 1) IS NOT NULL OR (SELECT 1 FROM fa fa3 WHERE fa3.problem_id = p3.id AND fa3.user_id = req.auth_user_id LIMIT 1) IS NOT NULL THEN true ELSE false END,
				               'todo', CASE WHEN (SELECT 1 FROM todo t3 WHERE t3.problem_id = p3.id AND t3.user_id = req.auth_user_id) IS NOT NULL THEN true ELSE false END,
				               'dangerous', COALESCE((
				                   SELECT gb3.danger 
				                   FROM guestbook gb3 
				                   WHERE gb3.problem_id = p3.id AND (gb3.danger = 1 OR gb3.resolved = 1) 
				                   ORDER BY gb3.id DESC LIMIT 1
				               ), 0) = 1
				           ))
				           FROM svg s3
				           JOIN problem p3 ON s3.problem_id = p3.id
				           JOIN grade g3 ON p3.consensus_grade_id = g3.id
				           JOIN grade_color clr3 ON g3.grade_color_id = clr3.id
				           JOIN type ty3 ON p3.type_id = ty3.id
				           JOIN sector sec3 ON p3.sector_id = sec3.id
				           JOIN area a5 ON sec3.area_id = a5.id
				           LEFT JOIN problem_section ps3 ON ps3.problem_id = p3.id AND ps3.nr = s3.pitch
				           LEFT JOIN grade g_sect3 ON ps3.grade_id = g_sect3.id
				           LEFT JOIN grade_color clr_sect3 ON g_sect3.grade_color_id = clr_sect3.id
				           LEFT JOIN user_region ur3 ON ur3.user_id = req.auth_user_id AND ur3.region_id = a5.region_id
				           WHERE s3.media_id = m.id
				             AND p3.trash IS NULL AND ((p3.locked_admin=0 AND p3.locked_superadmin=0) OR (ur3.superadmin_read=1) OR (ur3.admin_read=1 AND p3.locked_superadmin=0))
				       ) svgs_table_json,
				       COALESCE((SELECT mg.guestbook_id FROM media_guestbook mg WHERE mg.media_id = m.id LIMIT 1), 0) guestbook_id
				FROM req
				JOIN media m ON m.id = req.media_id
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				LEFT JOIN user p ON m.photographer_user_id = p.id
				""";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			int currentAuthUserId = authUserId.orElse(0);
			int idx = 1;
			ps.setInt(idx++, currentAuthUserId);
			ps.setInt(idx++, id);
			try (ResultSet rst = ps.executeQuery()) {
				if (rst.next()) {
					return Media.fromResultSet(rst, currentAuthUserId);
				}
			}
		}
		throw new NoSuchElementException("Could not find media with id=" + id);
	}
	
	public List<Media> getProfileMedia(Connection c, Optional<Integer> authUserId, int reqId, boolean captured) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		List<Media> res = new ArrayList<>();
		int currentAuthUserId = authUserId.orElse(0);

		String targetFilter = captured 
				? "m.photographer_user_id = req.target_user_id" 
						: "EXISTS (SELECT 1 FROM media_user mu WHERE mu.media_id = m.id AND mu.user_id = req.target_user_id)";

		String sql = """
				WITH req AS (
				    SELECT ? target_user_id, ? auth_user_id
				)
				SELECT m.id, m.uploader_user_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, m.description,
				       m.width, m.height, m.is_movie, m.is_360, m.embed_url, m.thumbnail_seconds,
				       DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken,
				       ph.id photographer_id, TRIM(CONCAT(ph.firstname, ' ', COALESCE(ph.lastname,''))) photographer_name,
				       NULL url,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT('id', tu.id, 'name', TRIM(CONCAT(tu.firstname, ' ', COALESCE(tu.lastname,'')))))
				           FROM media_user tmu
				           JOIN user tu ON tmu.user_id = tu.id
				           WHERE tmu.media_id = m.id
				       ) tagged_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT('areaId', ma.area_id, 'areaName', a2.name, 'trivia', ma.trivia))
				           FROM media_area ma
				           JOIN area a2 ON ma.area_id = a2.id
				           WHERE ma.media_id = m.id
				       ) areas_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT('areaId', a3.id, 'areaName', a3.name, 'sectorId', ms.sector_id, 'sectorName', s2.name, 'trivia', ms.trivia))
				           FROM media_sector ms
				           JOIN sector s2 ON ms.sector_id = s2.id
				           JOIN area a3 ON s2.area_id = a3.id
				           WHERE ms.media_id = m.id
				       ) sectors_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
				               'problemId', p2.id, 'problemName', p2.name, 'problemGrade', g.grade, 'problemPitch', mp2.pitch,
				               'problemNumPitches', (SELECT COUNT(*) FROM problem_section ps WHERE ps.problem_id = p2.id),
				               'milliseconds', mp2.milliseconds, 'areaId', a4.id, 'areaName', a4.name, 'sectorId', s2.id, 'sectorName', s2.name, 'trivia', mp2.trivia
				           ))
				           FROM media_problem mp2
				           JOIN problem p2 ON mp2.problem_id = p2.id
				           JOIN sector s2 ON p2.sector_id = s2.id
				           JOIN area a4 ON s2.area_id = a4.id
				           JOIN grade g ON p2.consensus_grade_id = g.id
				           LEFT JOIN user_region ur ON a4.region_id = ur.region_id AND ur.user_id = req.auth_user_id
				           WHERE mp2.media_id = m.id
				             AND p2.trash IS NULL AND ((p2.locked_admin=0 AND p2.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p2.locked_superadmin=0))
				       ) problems_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
						        'trailId', t9.id,
						        'trailTitle', t9.title,
						        'sectors', (
						            SELECT JSON_ARRAYAGG(JSON_OBJECT(
						                'areaId', a9_sub.id,
						                'areaName', a9_sub.name,
						                'sectorId', s9_sub.id,
						                'sectorName', s9_sub.name
						            ))
						            FROM sector_trail st9_sub
						            JOIN sector s9_sub ON st9_sub.sector_id = s9_sub.id
						            JOIN area a9_sub ON s9_sub.area_id = a9_sub.id
						            WHERE st9_sub.trail_id = t9.id
						        )
						    ))
						    FROM media_trail mt9
						    JOIN trail t9 ON mt9.trail_id = t9.id
						    WHERE mt9.media_id = m.id
				       ) trails_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
				               'id', id, 'path', path, 'rappelX', rappel_x, 'rappelY', rappel_y, 'rappelBolted', rappel_bolted
				           ))
				           FROM media_svg
				           WHERE media_id = m.id
				       ) svgs_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
				               'id', s3.id,
				               'problemId', p3.id,
				               'problemName', p3.name,
				               'problemGrade', CASE WHEN s3.pitch IS NULL OR s3.pitch = 0 THEN g3.grade ELSE COALESCE(g_sect3.grade, g3.grade) END,
				               'problemGradeColor', CASE WHEN s3.pitch IS NULL OR s3.pitch = 0 THEN clr3.hex_code ELSE COALESCE(clr_sect3.hex_code, clr3.hex_code) END,
				               'problemSubtype', ty3.subtype,
				               'nr', p3.nr,
				               'pitch', COALESCE(ps3.nr, 0),
				               'path', s3.path,
				               'hasAnchor', s3.has_anchor,
				               'texts', s3.texts,
				               'anchors', s3.anchors,
				               'tradBelayStations', s3.trad_belay_stations,
				               'primary', CASE WHEN p3.type_id IN (1,2) THEN true ELSE false END,
				               'ticked', CASE WHEN (SELECT 1 FROM tick tk3 WHERE tk3.problem_id = p3.id AND tk3.user_id = req.auth_user_id LIMIT 1) IS NOT NULL OR (SELECT 1 FROM fa fa3 WHERE fa3.problem_id = p3.id AND fa3.user_id = req.auth_user_id LIMIT 1) IS NOT NULL THEN true ELSE false END,
				               'todo', CASE WHEN (SELECT 1 FROM todo t3 WHERE t3.problem_id = p3.id AND t3.user_id = req.auth_user_id) IS NOT NULL THEN true ELSE false END,
				               'dangerous', COALESCE((
				                   SELECT gb3.danger 
				                   FROM guestbook gb3 
				                   WHERE gb3.problem_id = p3.id AND (gb3.danger = 1 OR gb3.resolved = 1) 
				                   ORDER BY gb3.id DESC LIMIT 1
				               ), 0) = 1
				           ))
				           FROM svg s3
				           JOIN problem p3 ON s3.problem_id = p3.id
				           JOIN grade g3 ON p3.consensus_grade_id = g3.id
				           JOIN grade_color clr3 ON g3.grade_color_id = clr3.id
				           JOIN type ty3 ON p3.type_id = ty3.id
				           JOIN sector sec3 ON p3.sector_id = sec3.id
				           JOIN area a5 ON sec3.area_id = a5.id
				           LEFT JOIN problem_section ps3 ON ps3.problem_id = p3.id AND ps3.nr = s3.pitch
				           LEFT JOIN grade g_sect3 ON ps3.grade_id = g_sect3.id
				           LEFT JOIN grade_color clr_sect3 ON g_sect3.grade_color_id = clr_sect3.id
				           LEFT JOIN user_region ur3 ON ur3.user_id = req.auth_user_id AND ur3.region_id = a5.region_id
				           WHERE s3.media_id = m.id
				             AND p3.trash IS NULL AND ((p3.locked_admin=0 AND p3.locked_superadmin=0) OR (ur3.superadmin_read=1) OR (ur3.admin_read=1 AND p3.locked_superadmin=0))
				       ) svgs_table_json,
				       COALESCE((SELECT mg.guestbook_id FROM media_guestbook mg WHERE mg.media_id = m.id LIMIT 1), 0) guestbook_id
				FROM req
				JOIN media m ON __TARGET_FILTER__ AND m.deleted_user_id IS NULL
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				LEFT JOIN user ph ON m.photographer_user_id = ph.id

				LEFT JOIN media_problem mp ON m.id = mp.media_id
				LEFT JOIN problem p ON mp.problem_id = p.id
				LEFT JOIN sector sp ON p.sector_id = sp.id
				LEFT JOIN area ap ON sp.area_id = ap.id
				LEFT JOIN user_region urp ON ap.region_id = urp.region_id AND urp.user_id = req.auth_user_id

				LEFT JOIN media_sector ms ON m.id = ms.media_id
				LEFT JOIN sector ss ON ms.sector_id = ss.id
				LEFT JOIN area asec ON ss.area_id = asec.id
				LEFT JOIN user_region urs ON asec.region_id = urs.region_id AND urs.user_id = req.auth_user_id

				LEFT JOIN media_area ma ON m.id = ma.media_id
				LEFT JOIN area am ON ma.area_id = am.id
				LEFT JOIN user_region ura ON am.region_id = ura.region_id AND ura.user_id = req.auth_user_id

				LEFT JOIN media_trail mt ON m.id = mt.media_id
				LEFT JOIN sector_trail st ON mt.trail_id = st.trail_id
				LEFT JOIN sector s_tr ON st.sector_id = s_tr.id
				LEFT JOIN area a_tr ON s_tr.area_id = a_tr.id
				LEFT JOIN user_region urt ON a_tr.region_id = urt.region_id AND urt.user_id = req.auth_user_id

				WHERE (mp.media_id IS NULL OR (p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (urp.superadmin_read=1) OR (urp.admin_read=1 AND p.locked_superadmin=0))))
				  AND (ms.media_id IS NULL OR (ss.trash IS NULL AND ((ss.locked_admin=0 AND ss.locked_superadmin=0) OR (urs.superadmin_read=1) OR (urs.admin_read=1 AND ss.locked_superadmin=0))))
				  AND (ma.media_id IS NULL OR (am.trash IS NULL AND ((am.locked_admin=0 AND am.locked_superadmin=0) OR (ura.superadmin_read=1) OR (ura.admin_read=1 AND am.locked_superadmin=0))))
				  AND (mt.media_id IS NULL OR (s_tr.trash IS NULL AND ((s_tr.locked_admin=0 AND a_tr.locked_superadmin=0) OR (urt.superadmin_read=1) OR (urt.admin_read=1 AND a_tr.locked_superadmin=0))))

				GROUP BY req.auth_user_id, m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.updated_at, m.description, m.width, m.height, m.is_movie, m.is_360, m.embed_url, m.thumbnail_seconds, m.date_created, m.date_taken, ph.id, ph.firstname, ph.lastname
				ORDER BY m.id DESC
				""".replace("__TARGET_FILTER__", targetFilter);

		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, reqId);
			ps.setInt(2, currentAuthUserId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res.add(Media.fromResultSet(rst, currentAuthUserId));
				}
			}
		}
		logger.debug("getProfileMedia(reqId={}, captured={}, duration={})", reqId, captured, stopwatch);
		return res;
	}
	
	public void logInstagramScrape(Connection c, Optional<Integer> authUserId, String originalUrl, int slideCount) throws SQLException {
		String sql = "INSERT INTO instagram_scrape_log (user_id, shortcode, original_url, slide_count) VALUES (?, ?, ?, ?)";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, authUserId.orElseThrow());
			ps.setString(2, ApifyInstagramResolver.extractInstagramShortcode(originalUrl));
			ps.setString(3, originalUrl);
			ps.setInt(4, slideCount);
			ps.executeUpdate();
		}
	}
	
	public void rotateMedia(Connection c, Optional<Integer> authUserId, int idMedia, int degrees) throws IOException, SQLException, InterruptedException {
		ensureMediaUploadedByMeOrConnectedToRegionWhereIAmAdmin(c, authUserId, idMedia);
		Rotation r = switch (degrees) {
		case 90 -> Rotation.CW_90;
		case 180 -> Rotation.CW_180;
		case 270 -> Rotation.CW_270;
		default -> throw new IllegalArgumentException("Cannot rotate image " + degrees + " degrees (legal degrees = 90, 180, 270)");
		};
		ImageHelper.rotateImage(dao, c, idMedia, r);
	}
	
	public void saveMediaAnalysis(Connection c, int mediaId, int imageWidth, int imageHeight, String hexColor, List<EntityAnnotation> labels, List<LocalizedObjectAnnotation> objects, boolean failed) throws SQLException {
		Preconditions.checkArgument(mediaId > 0, "Media id required");

		boolean exists = false;
		try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM media_ml_analysis WHERE media_id = ?")) {
			ps.setInt(1, mediaId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					exists = true;
				}
			}
		}
		if (exists) {
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_ml_analysis WHERE media_id=?")) {
				ps.setInt(1, mediaId);
				ps.execute();
			}
		}

		boolean hasPersonObject = objects != null && objects.stream().anyMatch(obj -> obj.getName().equalsIgnoreCase("Person"));

		int focusX = 0;
		int focusY = 0;

		if (hasPersonObject) {
			var climber = objects.stream()
					.filter(obj -> obj.getName().equalsIgnoreCase("Person"))
					.min(Comparator.comparing(obj -> obj.getBoundingPoly().getNormalizedVertices(0).getY()))
					.orElse(null);

			if (climber != null) {
				List<NormalizedVertex> v = climber.getBoundingPoly().getNormalizedVerticesList();
				if (v.size() >= 3) {
					float xMin = v.get(0).getX();
					float yMin = v.get(0).getY();
					float xMax = v.get(2).getX();
					float yMax = v.get(2).getY();
					float personHeight = yMax - yMin;

					focusX = Math.round(((xMin + xMax) / 2) * 100);
					if (imageHeight > imageWidth) {
						if (yMax > 0.80f && personHeight < 0.60f) {
							focusY = Math.round(yMax * 100);
						} else {
							focusY = Math.round((yMin + personHeight * 0.85f) * 100);
						}
					} else {
						focusY = Math.round(((yMin + yMax) / 2) * 100);
					}
				}
			}
		}

		try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_ml_analysis (media_id, primary_color_hex, focus_x, focus_y, is_action_shot, failed) VALUES (?, ?, ?, ?, ?, ?)")) {
			ps.setInt(1, mediaId);
			ps.setString(2, hexColor);
			ps.setInt(3, focusX);
			ps.setInt(4, focusY);
			ps.setBoolean(5, hasPersonObject);
			ps.setBoolean(6, failed);
			ps.execute();
		}

		if (!failed) {
			if (labels != null && !labels.isEmpty()) {
				try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_ml_label (media_id, description, score) VALUES (?, ?, ?)")) {
					for (EntityAnnotation l : labels) {
						ps.setInt(1, mediaId);
						ps.setString(2, l.getDescription());
						ps.setFloat(3, l.getScore());
						ps.addBatch();
					}
					ps.executeBatch();
				}
			}
			if (objects != null && !objects.isEmpty()) {
				try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_ml_object (media_id, name, score, x_min, y_min, x_max, y_max) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
					for (LocalizedObjectAnnotation obj : objects) {
						List<NormalizedVertex> v = obj.getBoundingPoly().getNormalizedVerticesList();
						if (v.size() >= 3) {
							ps.setInt(1, mediaId);
							ps.setString(2, obj.getName());
							ps.setFloat(3, obj.getScore());
							ps.setFloat(4, v.get(0).getX());
							ps.setFloat(5, v.get(0).getY());
							ps.setFloat(6, v.get(2).getX());
							ps.setFloat(7, v.get(2).getY());
							ps.addBatch();
						}
					}
					ps.executeBatch();
				}
			}
		}
	}
	
	public void setMediaMetadata(Connection c, int idMedia, int width, int height, LocalDateTime dateTaken, boolean is360) throws SQLException {
		String sqlStr = dateTaken == null ?
				"UPDATE media SET width=?, height=?, is_360=? WHERE id=?" :
					"UPDATE media SET date_taken=?, width=?, height=?, is_360=? WHERE id=?";
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			int ix = 0;
			if (dateTaken != null) {
				ps.setObject(++ix, dateTaken);
			}
			ps.setInt(++ix, width);
			ps.setInt(++ix, height);
			ps.setBoolean(++ix, is360);
			ps.setInt(++ix, idMedia);
			ps.executeUpdate();
		}
		logger.debug("setMediaMetadata(idMedia={}, width={}, height={}, dateTaken={}, is360={}) - success", idMedia, width, height, dateTaken, is360);
	}
	
	public void shiftMediaPosition(Connection c, Optional<Integer> authUserId, int id, boolean left, boolean right) throws SQLException {
		boolean ok = false;
		int areaId = 0;
		int sectorId = 0;
		int problemId = 0;
		int trailId = 0;
		try (PreparedStatement ps = c.prepareStatement("""
				WITH req AS (
				  SELECT ? auth_user_id, ? media_id
				)
				SELECT ur.admin_write, ur.superadmin_write,
				       MAX(ma.area_id) area_id, MAX(ms.sector_id) sector_id, MAX(mp.problem_id) problem_id, MAX(mt.trail_id) trail_id
				FROM req
				JOIN area a ON true
				JOIN sector s ON a.id=s.area_id
				JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=req.auth_user_id
				LEFT JOIN media_area ma ON a.id=ma.area_id AND ma.media_id=req.media_id
				LEFT JOIN media_sector ms ON s.id=ms.sector_id AND ms.media_id=req.media_id
				LEFT JOIN problem p ON s.id=p.sector_id
				LEFT JOIN media_problem mp ON p.id=mp.problem_id AND mp.media_id=req.media_id
				LEFT JOIN sector_trail st ON s.id=st.sector_id
				LEFT JOIN media_trail mt ON st.trail_id=mt.trail_id AND mt.media_id=req.media_id
				WHERE ma.media_id IS NOT NULL OR ms.media_id IS NOT NULL OR mp.media_id IS NOT NULL OR mt.media_id IS NOT NULL
				GROUP BY ur.admin_write, ur.superadmin_write
				""")) {
			ps.setInt(1, authUserId.orElseThrow());
			ps.setInt(2, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					ok = rst.getBoolean("admin_write") || rst.getBoolean("superadmin_write");
					areaId = rst.getInt("area_id");
					sectorId = rst.getInt("sector_id");
					problemId = rst.getInt("problem_id");
					trailId = rst.getInt("trail_id");
				}
			}
		}
		Preconditions.checkArgument(ok, "Insufficient permissions");

		String table = null;
		String column = null;
		String extraOrder = "";
		int columnId = 0;
		if (areaId > 0) {
			table = "media_area";
			column = "area_id";
			columnId = areaId;
		}
		else if (sectorId > 0) {
			table = "media_sector";
			column = "sector_id";
			columnId = sectorId;
		}
		else if (trailId > 0) {
			table = "media_trail";
			column = "trail_id";
			columnId = trailId;
		}
		else if (problemId > 0) {
			table = "media_problem";
			column = "problem_id";
			columnId = problemId;
			extraOrder = "IFNULL(pitch,0), ";
		}
		else {
			throw new UnsupportedOperationException("Could not find media association for left/right move.");
		}
		List<Integer> idMediaList = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT m.id FROM " + table + " x, media m WHERE x." + column + "=? AND x.media_id=m.id AND m.deleted_user_id IS NULL AND m.is_movie=0 ORDER BY " + extraOrder + " -x.sorting DESC, m.id")) {
			ps.setInt(1, columnId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idMedia = rst.getInt("id");
					idMediaList.add(idMedia);
				}
			}
		}
		final int ixToMove = idMediaList.indexOf(id);
		idMediaList.remove(ixToMove);
		Preconditions.checkArgument(ixToMove>=0, "Could not find " + id + " in " + idMediaList);
		if (left) {
			if (ixToMove == 0) {
				idMediaList.add(id); // Move from start to end
			} else {
				idMediaList.add(ixToMove-1, id);
			}
		}
		else if (right) {
			if (ixToMove == idMediaList.size()) {
				idMediaList.addFirst(id); // Move from end to start
			} else {
				idMediaList.add(ixToMove+1, id);
			}
		}
		else {
			throw new UnsupportedOperationException("left=false and right=false");
		}
		try (PreparedStatement ps = c.prepareStatement("UPDATE " + table + " SET sorting=? WHERE " + column + "=? AND media_id=?")) {
			int sorting = 0;
			for (int idMedia : idMediaList) {
				ps.setInt(1, ++sorting);
				ps.setInt(2, columnId);
				ps.setInt(3, idMedia);
				ps.addBatch();
			}
			ps.executeBatch();
		}

		if (problemId > 0) {
			dao.getActivityRepo().fillActivity(c, problemId);
		}
	}

	public void updateMedia(Connection c, Optional<Integer> authUserId, Media m) throws Exception {
		Preconditions.checkArgument(m.identity() != null && m.identity().id() != 0, "Media id required.");
		Preconditions.checkArgument(m.photographer() != null && !Strings.isNullOrEmpty(m.photographer().name()), "A valid photographer must be specified to update media context.");
		Association associations = m.ensureCorrectMediaAssociations(authUserId);
		Stopwatch stopwatch = Stopwatch.createStarted();
		final int mediaId = m.identity().id();
		ensureMediaUploadedByMeOrConnectedToRegionWhereIAmAdmin(c, authUserId, mediaId);
		Media originalMedia = getMedia(c, authUserId, m.identity().id());	    
		boolean thumbnailChanged = originalMedia.thumbnailSeconds() != m.thumbnailSeconds();
		int photographerId = m.photographer().id() > 0 ? m.photographer().id() : dao.getUserRepo().getExistingOrInsertUser(c, m.photographer().name());
		String baseUpdateSql = thumbnailChanged 
				? "UPDATE media SET description=?, photographer_user_id=?, thumbnail_seconds=?, updated_at=NOW() WHERE id=?"
						: "UPDATE media SET description=?, photographer_user_id=?, thumbnail_seconds=? WHERE id=?";
		try (PreparedStatement ps = c.prepareStatement(baseUpdateSql)) {
			ps.setString(1, Strings.emptyToNull(m.description()));
			ps.setInt(2, photographerId);
			ps.setInt(3, m.thumbnailSeconds());
			ps.setInt(4, mediaId);
			ps.execute();
		}
		if (originalMedia.isMovie() && thumbnailChanged) {
			StorageManager storage = StorageManager.getInstance();
			String originalMp4Key = S3KeyGenerator.getOriginalMp4(mediaId);
			Path tempOriginal = Files.createTempFile("original-re-thumb-" + mediaId, ".mp4");
			try {
				storage.downloadFile(originalMp4Key, tempOriginal);
				VideoHelper.extractThumbnail(c, dao, mediaId, tempOriginal, m.thumbnailSeconds());
				S3KeyGenerator.getGeneratedMediaPrefixes(mediaId).forEach(storage::invalidateCache);
			} finally {
				Files.deleteIfExists(tempOriginal);
			}
		}
		saveMediaContext(c, mediaId, associations, m, true);
		Set<Integer> problemIdsToUpdate = Stream.of(originalMedia.problems(), m.problems())
				.filter(Objects::nonNull)
				.flatMap(List::stream)
				.map(MediaProblem::problemId)
				.collect(Collectors.toSet());
		for (int idProblem : problemIdsToUpdate) {
			dao.getActivityRepo().fillActivity(c, idProblem);
		}
		logger.debug("updateMedia(authUserId={}, m={}) duration={}", authUserId, m, stopwatch);
	}
	
	public void upsertMediaSvg(Connection c, Optional<Integer> authUserId, Setup setup, Media m) throws SQLException {
		dao.getRegionRepo().ensureAdminWriteRegion(c, setup, authUserId);
		// Clear existing
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_svg WHERE media_id=?")) {
			ps.setInt(1, m.identity().id());
			ps.execute();
		}
		// Insert
		for (MediaSvgElement element : m.mediaSvgs()) {
			if (element.t().equals(MediaSvgElementType.PATH)) {
				try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_svg (media_id, path) VALUES (?, ?)")) {
					ps.setInt(1, m.identity().id());
					ps.setString(2, element.path());
					ps.execute();
				}
			}
			else if (element.t().equals(MediaSvgElementType.RAPPEL_BOLTED) || element.t().equals(MediaSvgElementType.RAPPEL_NOT_BOLTED)) {
				try (PreparedStatement ps = c.prepareStatement("INSERT INTO media_svg (media_id, rappel_x, rappel_y, rappel_bolted) VALUES (?, ?, ?, ?)")) {
					ps.setInt(1, m.identity().id());
					ps.setInt(2, element.rappelX());
					ps.setInt(3, element.rappelY());
					ps.setBoolean(4, element.t().equals(MediaSvgElementType.RAPPEL_BOLTED));
					ps.execute();
				}
			}
			else {
				throw new RuntimeException("Invalid type: " + element.t());
			}
		}
	}
	
	public void upsertSvg(Connection c, Optional<Integer> authUserId, int problemId, int pitch, int mediaId, Svg svg) throws SQLException {
		dao.getProblemRepo().ensureAdminWriteProblem(c, authUserId, problemId);
		// Delete/Insert/Update
		if (svg.delete() || GlobalFunctions.stripString(svg.path()) == null) {
			if (pitch == 0) {
				try (PreparedStatement ps = c.prepareStatement("DELETE FROM svg WHERE media_id=? AND problem_id=? AND pitch IS NULL")) {
					ps.setInt(1, mediaId);
					ps.setInt(2, problemId);
					ps.execute();
				}
			}
			else {
				try (PreparedStatement ps = c.prepareStatement("DELETE FROM svg WHERE media_id=? AND problem_id=? AND pitch=?")) {
					ps.setInt(1, mediaId);
					ps.setInt(2, problemId);
					ps.setInt(3, pitch);
					ps.execute();
				}
			}
		} else if (svg.id() <= 0) {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO svg (media_id, problem_id, pitch, path, has_anchor, anchors, trad_belay_stations, texts) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
				ps.setInt(1, mediaId);
				ps.setInt(2, problemId);
				JdbcUtils.setNullablePositiveInteger(ps, 3, pitch);
				ps.setString(4, svg.path());
				ps.setBoolean(5, svg.hasAnchor());
				ps.setString(6, svg.anchors());
				ps.setString(7, svg.tradBelayStations());
				ps.setString(8, svg.texts());
				ps.execute();
			}
		} else {
			try (PreparedStatement ps = c.prepareStatement("UPDATE svg SET media_id=?, problem_id=?, pitch=?, path=?, has_anchor=?, anchors=?, trad_belay_stations=?, texts=? WHERE id=?")) {
				ps.setInt(1, mediaId);
				ps.setInt(2, problemId);
				JdbcUtils.setNullablePositiveInteger(ps, 3, pitch);
				ps.setString(4, svg.path());
				ps.setBoolean(5, svg.hasAnchor());
				ps.setString(6, svg.anchors());
				ps.setString(7, svg.tradBelayStations());
				ps.setString(8, svg.texts());
				ps.setInt(9, svg.id());
				ps.execute();
			}
		}
	}
	
	private void ensureMediaUploadedByMeOrConnectedToRegionWhereIAmAdmin(Connection c, Optional<Integer> authUserId, int idMedia) throws SQLException {
		Media m = getMedia(c, authUserId, idMedia);
		if (m.uploadedByMe()) {
			return;
		}
		try (PreparedStatement ps = c.prepareStatement("""
				WITH req AS (
					SELECT ? auth_user_id, ? media_id
				)
				SELECT ur.admin_write, ur.superadmin_write, ma.area_id, ms.sector_id, mp.problem_id, g.id guestbook_id, mt.trail_id
				FROM req
				JOIN area a ON true
				JOIN sector s ON a.id=s.area_id
				JOIN user_region ur ON a.region_id=ur.region_id AND ur.user_id=req.auth_user_id
				LEFT JOIN media_area ma ON a.id=ma.area_id AND ma.media_id=req.media_id
				LEFT JOIN media_sector ms ON s.id=ms.sector_id AND ms.media_id=req.media_id
				LEFT JOIN problem p ON s.id=p.sector_id
				LEFT JOIN media_problem mp ON p.id=mp.problem_id AND mp.media_id=req.media_id
				LEFT JOIN guestbook g ON p.id=g.problem_id
				LEFT JOIN media_guestbook mg ON g.id=mg.guestbook_id AND mg.media_id=req.media_id
				LEFT JOIN sector_trail st ON s.id=st.sector_id
				LEFT JOIN media_trail mt ON st.trail_id=mt.trail_id AND mt.media_id=req.media_id
				WHERE ma.media_id IS NOT NULL 
				   OR ms.media_id IS NOT NULL 
				   OR mp.media_id IS NOT NULL 
				   OR mg.media_id IS NOT NULL 
				   OR mt.media_id IS NOT NULL
				GROUP BY ur.admin_write, ur.superadmin_write, ma.area_id, ms.sector_id, mp.problem_id, g.id, mt.trail_id
				""")) {
			ps.setInt(1, authUserId.orElseThrow());
			ps.setInt(2, idMedia);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					boolean adminWrite = rst.getBoolean("admin_write");
					boolean superAdminWrite = rst.getBoolean("superadmin_write");
					int areaId = rst.getInt("area_id");
					int sectorId = rst.getInt("sector_id");
					int problemId = rst.getInt("problem_id");
					int guestbookId = rst.getInt("guestbook_id");
					int trailId = rst.getInt("trail_id");
					if ((adminWrite || superAdminWrite) && (areaId > 0 || sectorId > 0 || problemId > 0 || guestbookId > 0 || trailId > 0)) {
						return;
					}
				}
			}
		}
		throw new IllegalArgumentException("Insufficient permissions");
	}
	
	private int insertMediaMetadata(Connection c, int uploaderId, Media m, StorageType storageType) throws Exception {
		String photographerName = (m.photographer() != null) ? m.photographer().name() : null;
		int photographerId = (m.photographer() != null && m.photographer().id() > 0) ? m.photographer().id() : dao.getUserRepo().getExistingOrInsertUser(c, photographerName);

		String insertMediaSql = "INSERT INTO media (is_movie, suffix, photographer_user_id, uploader_user_id, date_created, description, thumbnail_seconds, embed_url) VALUES (?, ?, ?, ?, NOW(), ?, ?, ?)";
		try (PreparedStatement ps = c.prepareStatement(insertMediaSql, Statement.RETURN_GENERATED_KEYS)) {
			ps.setBoolean(1, storageType.isMovie());
			ps.setString(2, storageType.getExtension());
			ps.setInt(3, photographerId);
			ps.setInt(4, uploaderId);
			ps.setString(5, GlobalFunctions.stripString(m.description()));
			ps.setInt(6, m.thumbnailSeconds());
			ps.setString(7, m.embedUrl());
			ps.executeUpdate();
			try (ResultSet rst = ps.getGeneratedKeys()) {
				if (rst != null && rst.next()) {
					return rst.getInt(1);
				}
			}
		}
		throw new IllegalStateException("Failed to insert media metadata");
	}
	
	private void saveMediaContext(Connection c, int mediaId, Association associations, Media m, boolean isUpdate) throws SQLException {
		// If this is an update, cleanly wipe out all previous entity and user associations first
		if (isUpdate) {
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_area WHERE media_id=?")) { ps.setInt(1, mediaId); ps.execute(); }
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_sector WHERE media_id=?")) { ps.setInt(1, mediaId); ps.execute(); }
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_problem WHERE media_id=?")) { ps.setInt(1, mediaId); ps.execute(); }
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_trail WHERE media_id=?")) { ps.setInt(1, mediaId); ps.execute(); }
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_guestbook WHERE media_id=?")) { ps.setInt(1, mediaId); ps.execute(); }
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM media_user WHERE media_id=?")) { ps.setInt(1, mediaId); ps.execute(); }
		}

		// 1. Sync the Primary Exclusive Association
		switch (associations) {
		case AREAS -> {
			String sql = "INSERT INTO media_area (media_id, area_id, trivia) VALUES (?, ?, ?)";
			try (PreparedStatement ps = c.prepareStatement(sql)) {
				for (Media.MediaArea area : m.areas()) {
					ps.setInt(1, mediaId);
					ps.setInt(2, area.areaId());
					ps.setBoolean(3, area.trivia());
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
		case SECTORS -> {
			String sql = "INSERT INTO media_sector (media_id, sector_id, trivia) VALUES (?, ?, ?)";
			try (PreparedStatement ps = c.prepareStatement(sql)) {
				for (Media.MediaSector sector : m.sectors()) {
					ps.setInt(1, mediaId);
					ps.setInt(2, sector.sectorId());
					ps.setBoolean(3, sector.trivia());
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
		case PROBLEMS -> {
			String sql = "INSERT INTO media_problem (media_id, problem_id, pitch, trivia, milliseconds) VALUES (?, ?, ?, ?, ?)";
			try (PreparedStatement ps = c.prepareStatement(sql)) {
				for (Media.MediaProblem problem : m.problems()) {
					ps.setInt(1, mediaId);
					ps.setInt(2, problem.problemId());
					ps.setInt(3, problem.problemPitch());
					ps.setBoolean(4, problem.trivia());
					ps.setLong(5, problem.milliseconds());
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
		case TRAILS -> {
			String sql = "INSERT INTO media_trail (media_id, trail_id) VALUES (?, ?)";
			try (PreparedStatement ps = c.prepareStatement(sql)) {
				for (Media.MediaTrail trail : m.trails()) {
					ps.setInt(1, mediaId);
					ps.setInt(2, trail.trailId());
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
		case GUESTBOOK -> {
			String sql = "INSERT INTO media_guestbook (media_id, guestbook_id) VALUES (?, ?)";
			try (PreparedStatement ps = c.prepareStatement(sql)) {
				ps.setInt(1, mediaId);
				ps.setInt(2, m.guestbookId());
				ps.execute();
			}
		}
		case USER_AVATAR -> {
			String sql = "UPDATE user SET media_id=? WHERE id=?";
			try (PreparedStatement ps = c.prepareStatement(sql)) {
				ps.setInt(1, mediaId);
				ps.setInt(2, m.userAvatarId());
				ps.execute();
			}
		}
		}

		// 2. Tagged Users
		if (m.tagged() != null && !m.tagged().isEmpty()) {
			String sql = "INSERT INTO media_user (media_id, user_id) VALUES (?, ?)";
			try (PreparedStatement ps = c.prepareStatement(sql)) {
				for (User u : m.tagged()) {
					if (isUpdate) {
						Preconditions.checkArgument(!Strings.isNullOrEmpty(u.name()), "Invalid tagged user: " + u);
					}
					int userId = u.id() > 0 ? u.id() : dao.getUserRepo().getExistingOrInsertUser(c, u.name());
					ps.setInt(1, mediaId);
					ps.setInt(2, userId);
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
	}

	protected List<Media> getMediaArea(Connection c, Optional<Integer> authUserId, int id, boolean inherited) throws SQLException {
		List<Media> res = new ArrayList<>();
		int currentAuthUserId = authUserId.orElse(0);
		String sql = """
				WITH req AS (
				    SELECT ? auth_user_id, ? area_id
				)
				SELECT a.name as area_name, m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, UNIX_TIMESTAMP(m.updated_at) version_stamp, m.description, ma.trivia, m.width, m.height, m.is_movie, m.is_360, m.embed_url, m.thumbnail_seconds,
				       DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, 
				       p.id photographer_id, TRIM(CONCAT(p.firstname, ' ', COALESCE(p.lastname,''))) photographer_name,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT('id', tu.id, 'name', TRIM(CONCAT(tu.firstname, ' ', COALESCE(tu.lastname,'')))))
				           FROM media_user tmu
				           JOIN user tu ON tmu.user_id = tu.id
				           WHERE tmu.media_id = m.id
				       ) tagged_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT('areaId', ma2.area_id, 'areaName', a2.name, 'trivia', ma2.trivia))
				           FROM media_area ma2
				           JOIN area a2 ON ma2.area_id = a2.id
				           WHERE ma2.media_id = m.id
				       ) areas_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT('areaId', a3.id, 'areaName', a3.name, 'sectorId', ms.sector_id, 'sectorName', s3.name, 'trivia', ms.trivia))
				           FROM media_sector ms
				           JOIN sector s3 ON ms.sector_id = s3.id
				           JOIN area a3 ON s3.area_id = a3.id
				           WHERE ms.media_id = m.id
				       ) sectors_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
				               'problemId', p2.id, 'problemName', p2.name, 'problemGrade', g.grade, 'problemPitch', mp2.pitch,
				               'problemNumPitches', (SELECT COUNT(*) FROM problem_section ps WHERE ps.problem_id = p2.id),
				               'milliseconds', mp2.milliseconds, 'areaId', a4.id, 'areaName', a4.name, 'sectorId', s4.id, 'sectorName', s4.name, 'trivia', mp2.trivia
				           ))
				           FROM media_problem mp2
				           JOIN problem p2 ON mp2.problem_id = p2.id
				           JOIN sector s4 ON p2.sector_id = s4.id
				           JOIN area a4 ON s4.area_id = a4.id
				           JOIN grade g ON p2.consensus_grade_id = g.id
				           LEFT JOIN user_region ur ON a4.region_id = ur.region_id AND ur.user_id = req.auth_user_id
				           WHERE mp2.media_id = m.id
				             AND p2.trash IS NULL AND ((p2.locked_admin=0 AND p2.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p2.locked_superadmin=0))
				       ) problems_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
						        'trailId', t9.id,
						        'trailTitle', t9.title,
						        'sectors', (
						            SELECT JSON_ARRAYAGG(JSON_OBJECT(
						                'areaId', a9_sub.id,
						                'areaName', a9_sub.name,
						                'sectorId', s9_sub.id,
						                'sectorName', s9_sub.name
						            ))
						            FROM sector_trail st9_sub
						            JOIN sector s9_sub ON st9_sub.sector_id = s9_sub.id
						            JOIN area a9_sub ON s9_sub.area_id = a9_sub.id
						            WHERE st9_sub.trail_id = t9.id
						        )
						    ))
						    FROM media_trail mt9
						    JOIN trail t9 ON mt9.trail_id = t9.id
						    WHERE mt9.media_id = m.id
				       ) trails_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
				               'id', id, 'path', path, 'rappelX', rappel_x, 'rappelY', rappel_y, 'rappelBolted', rappel_bolted
				           ))
				           FROM media_svg
				           WHERE media_id = m.id
				       ) svgs_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
				               'id', s3.id,
				               'problemId', p3.id,
				               'problemName', p3.name,
				               'problemGrade', CASE WHEN s3.pitch IS NULL OR s3.pitch = 0 THEN g3.grade ELSE COALESCE(g_sect3.grade, g3.grade) END,
				               'problemGradeColor', CASE WHEN s3.pitch IS NULL OR s3.pitch = 0 THEN clr3.hex_code ELSE COALESCE(clr_sect3.hex_code, clr3.hex_code) END,
				               'problemSubtype', ty3.subtype,
				               'nr', p3.nr,
				               'pitch', COALESCE(ps3.nr, 0),
				               'path', s3.path,
				               'hasAnchor', s3.has_anchor,
				               'texts', s3.texts,
				               'anchors', s3.anchors,
				               'tradBelayStations', s3.trad_belay_stations,
				               'primary', CASE WHEN p3.type_id IN (1,2) THEN true ELSE false END,
				               'ticked', CASE WHEN (SELECT 1 FROM tick tk3 WHERE tk3.problem_id = p3.id AND tk3.user_id = req.auth_user_id LIMIT 1) IS NOT NULL OR (SELECT 1 FROM fa fa3 WHERE fa3.problem_id = p3.id AND fa3.user_id = req.auth_user_id LIMIT 1) IS NOT NULL THEN true ELSE false END,
				               'todo', CASE WHEN (SELECT 1 FROM todo t3 WHERE t3.problem_id = p3.id AND t3.user_id = req.auth_user_id) IS NOT NULL THEN true ELSE false END,
				               'dangerous', COALESCE((
				                   SELECT gb3.danger 
				                   FROM guestbook gb3 
				                   WHERE gb3.problem_id = p3.id AND (gb3.danger = 1 OR gb3.resolved = 1) 
				                   ORDER BY gb3.id DESC LIMIT 1
				               ), 0) = 1
				           ))
				           FROM svg s3
				           JOIN problem p3 ON s3.problem_id = p3.id
				           JOIN grade g3 ON p3.consensus_grade_id = g3.id
				           JOIN grade_color clr3 ON g3.grade_color_id = clr3.id
				           JOIN type ty3 ON p3.type_id = ty3.id
				           JOIN sector sec3 ON p3.sector_id = sec3.id
				           JOIN area a5 ON sec3.area_id = a5.id
				           LEFT JOIN problem_section ps3 ON ps3.problem_id = p3.id AND ps3.nr = s3.pitch
				           LEFT JOIN grade g_sect3 ON ps3.grade_id = g_sect3.id
				           LEFT JOIN grade_color clr_sect3 ON g_sect3.grade_color_id = clr_sect3.id
				           LEFT JOIN user_region ur3 ON ur3.user_id = req.auth_user_id AND ur3.region_id = a5.region_id
				           WHERE s3.media_id = m.id
				             AND p3.trash IS NULL AND ((p3.locked_admin=0 AND p3.locked_superadmin=0) OR (ur3.superadmin_read=1) OR (ur3.admin_read=1 AND p3.locked_superadmin=0))
				       ) svgs_table_json,
				       COALESCE((SELECT mg.guestbook_id FROM media_guestbook mg WHERE mg.media_id = m.id LIMIT 1), 0) guestbook_id
				FROM req
				JOIN media_area ma ON ma.area_id = req.area_id
				JOIN media m ON m.id = ma.media_id
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				JOIN area a ON ma.area_id = a.id
				LEFT JOIN user p ON m.photographer_user_id = p.id
				WHERE m.deleted_user_id IS NULL
				GROUP BY req.auth_user_id, a.name, m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.updated_at, ma.trivia, m.description, m.width, m.height, m.is_movie, m.is_360, m.embed_url, m.thumbnail_seconds, ma.sorting, m.date_created, m.date_taken, p.id, p.firstname, p.lastname
				ORDER BY m.is_movie, m.embed_url, -ma.sorting DESC, m.id
				""";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			int idx = 1;
			ps.setInt(idx++, currentAuthUserId);
			ps.setInt(idx++, id);

			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					boolean trivia = rst.getBoolean("trivia");
					if (inherited && trivia) {
						continue; 
					}
					Media m = Media.fromResultSet(rst, currentAuthUserId);
					res.add(new Media(
							m.identity(), m.uploadedByMe(), m.width(), m.height(), m.isMovie(), m.is360(),
							m.dateCreated(), m.dateTaken(), m.photographer(), m.tagged(), m.description(),
							m.mediaSvgs(), m.svgProblemId(), m.svgs(), m.embedUrl(), m.thumbnailSeconds(),
							inherited, m.areas(), m.sectors(), m.problems(), m.trails(), m.guestbookId(), m.userAvatarId()
							));
				}
			}
		}
		return res;
	}

	protected List<Media> getMediaGuestbook(Connection c, Optional<Integer> authUserId, int guestbookId) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		List<Media> res = new ArrayList<>();
		int currentAuthUserId = authUserId.orElse(0);
		String sql = """
				WITH req AS (
				    SELECT ? auth_user_id, ? guestbook_id
				)
				SELECT m.id, m.uploader_user_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, m.description,
				       m.width, m.height, m.is_movie, m.is_360, m.embed_url, m.thumbnail_seconds,
				       DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken,
				       ph.id photographer_id, TRIM(CONCAT(ph.firstname, ' ', COALESCE(ph.lastname,''))) photographer_name,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT('id', tu.id, 'name', TRIM(CONCAT(tu.firstname, ' ', COALESCE(tu.lastname,'')))))
				           FROM media_user tmu
				           JOIN user tu ON tmu.user_id = tu.id
				           WHERE tmu.media_id = m.id
				       ) tagged_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT('areaId', ma.area_id, 'areaName', a2.name, 'trivia', ma.trivia))
				           FROM media_area ma
				           JOIN area a2 ON ma.area_id = a2.id
				           WHERE ma.media_id = m.id
				       ) areas_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT('areaId', a2.id, 'areaName', a2.name, 'sectorId', ms.sector_id, 'sectorName', s2.name, 'trivia', ms.trivia))
				           FROM media_sector ms
				           JOIN sector s2 ON ms.sector_id = s2.id
				           JOIN area a2 ON s2.area_id = a2.id
				           WHERE ms.media_id = m.id
				       ) sectors_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
				               'problemId', p2.id, 'problemName', p2.name, 'problemGrade', g2.grade, 'problemPitch', mp.pitch,
				               'problemNumPitches', (SELECT COUNT(*) FROM problem_section ps WHERE ps.problem_id = p2.id),
				               'milliseconds', mp.milliseconds, 'areaId', a2.id, 'areaName', a2.name, 'sectorId', s2.id, 'sectorName', s2.name, 'trivia', mp.trivia
				           ))
				           FROM media_problem mp
				           JOIN problem p2 ON mp.problem_id = p2.id
				           JOIN sector s2 ON p2.sector_id = s2.id
				           JOIN area a2 ON s2.area_id = a2.id
				           JOIN grade g2 ON p2.consensus_grade_id = g2.id
				           LEFT JOIN user_region ur ON a2.region_id = ur.region_id AND ur.user_id = req.auth_user_id
				           WHERE mp.media_id = m.id
				             AND p2.trash IS NULL AND ((p2.locked_admin=0 AND p2.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p2.locked_superadmin=0))
				       ) problems_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
						        'trailId', t9.id,
						        'trailTitle', t9.title,
						        'sectors', (
						            SELECT JSON_ARRAYAGG(JSON_OBJECT(
						                'areaId', a9_sub.id,
						                'areaName', a9_sub.name,
						                'sectorId', s9_sub.id,
						                'sectorName', s9_sub.name
						            ))
						            FROM sector_trail st9_sub
						            JOIN sector s9_sub ON st9_sub.sector_id = s9_sub.id
						            JOIN area a9_sub ON s9_sub.area_id = a9_sub.id
						            WHERE st9_sub.trail_id = t9.id
						        )
						    ))
						    FROM media_trail mt9
						    JOIN trail t9 ON mt9.trail_id = t9.id
						    WHERE mt9.media_id = m.id
				       ) trails_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
				               'id', id, 'path', path, 'rappelX', rappel_x, 'rappelY', rappel_y, 'rappelBolted', rappel_bolted
				           ))
				           FROM media_svg
				           WHERE media_id = m.id
				       ) svgs_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
				               'id', s3.id,
				               'problemId', p3.id,
				               'problemName', p3.name,
				               'problemGrade', CASE WHEN s3.pitch IS NULL OR s3.pitch = 0 THEN g3.grade ELSE COALESCE(g_sect3.grade, g3.grade) END,
				               'problemGradeColor', CASE WHEN s3.pitch IS NULL OR s3.pitch = 0 THEN clr3.hex_code ELSE COALESCE(clr_sect3.hex_code, clr3.hex_code) END,
				               'problemSubtype', ty3.subtype,
				               'nr', p3.nr,
				               'pitch', COALESCE(ps3.nr, 0),
				               'path', s3.path,
				               'hasAnchor', s3.has_anchor,
				               'texts', s3.texts,
				               'anchors', s3.anchors,
				               'tradBelayStations', s3.trad_belay_stations,
				               'primary', CASE WHEN p3.type_id IN (1,2) THEN true ELSE false END,
				               'ticked', CASE WHEN (SELECT 1 FROM tick tk3 WHERE tk3.problem_id = p3.id AND tk3.user_id = req.auth_user_id LIMIT 1) IS NOT NULL OR (SELECT 1 FROM fa fa3 WHERE fa3.problem_id = p3.id AND fa3.user_id = req.auth_user_id LIMIT 1) IS NOT NULL THEN true ELSE false END,
				               'todo', CASE WHEN (SELECT 1 FROM todo t3 WHERE t3.problem_id = p3.id AND t3.user_id = req.auth_user_id) IS NOT NULL THEN true ELSE false END,
				               'dangerous', COALESCE((
				                   SELECT gb3.danger 
				                   FROM guestbook gb3 
				                   WHERE gb3.problem_id = p3.id AND (gb3.danger = 1 OR gb3.resolved = 1) 
				                   ORDER BY gb3.id DESC LIMIT 1
				               ), 0) = 1
				           ))
				           FROM svg s3
				           JOIN problem p3 ON s3.problem_id = p3.id
				           JOIN grade g3 ON p3.consensus_grade_id = g3.id
				           JOIN grade_color clr3 ON g3.grade_color_id = clr3.id
				           JOIN type ty3 ON p3.type_id = ty3.id
				           JOIN sector sec3 ON p3.sector_id = sec3.id
				           JOIN area a5 ON sec3.area_id = a5.id
				           LEFT JOIN problem_section ps3 ON ps3.problem_id = p3.id AND ps3.nr = s3.pitch
				           LEFT JOIN grade g_sect3 ON ps3.grade_id = g_sect3.id
				           LEFT JOIN grade_color clr_sect3 ON g_sect3.grade_color_id = clr_sect3.id
				           LEFT JOIN user_region ur3 ON ur3.user_id = req.auth_user_id AND ur3.region_id = a5.region_id
				           WHERE s3.media_id = m.id
				             AND p3.trash IS NULL AND ((p3.locked_admin=0 AND p3.locked_superadmin=0) OR (ur3.superadmin_read=1) OR (ur3.admin_read=1 AND p3.locked_superadmin=0))
				       ) svgs_table_json,
				       mg.guestbook_id
				FROM req
				JOIN guestbook g ON g.id = req.guestbook_id
				JOIN media_guestbook mg ON g.id = mg.guestbook_id
				JOIN media m ON (mg.media_id = m.id AND m.deleted_user_id IS NULL)
				JOIN problem p ON g.problem_id = p.id
				JOIN sector s ON p.sector_id = s.id
				JOIN area a ON s.area_id = a.id
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				LEFT JOIN user ph ON m.photographer_user_id = ph.id
				GROUP BY req.auth_user_id, m.id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.uploader_user_id, m.updated_at, m.description, m.width, m.height, m.is_movie, m.is_360, m.embed_url, m.thumbnail_seconds, m.date_created, m.date_taken, ph.id, ph.firstname, ph.lastname, mg.guestbook_id
				ORDER BY m.is_movie, m.embed_url, m.id
				""";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			int idx = 1;
			ps.setInt(idx++, currentAuthUserId);
			ps.setInt(idx++, guestbookId);

			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					res.add(Media.fromResultSet(rst, currentAuthUserId));
				}
			}
		}
		logger.debug("getMediaGuestbook(guestbookId={}) - res.size={}, duration={}", guestbookId, res.size(), stopwatch);
		return res;
	}
	
	protected List<Media> getMediaProblem(Connection c, Setup s, Optional<Integer> authUserId, int areaId, int sectorId, int problemId, boolean showHiddenMedia) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		CompletableFuture<List<Media>> sectorMediaFuture = CompletableFuture.supplyAsync(() -> {
			try {
				return getMediaSector(c, s, authUserId, sectorId, problemId, true, areaId, 0, problemId, showHiddenMedia);
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		});

		List<Media> pMediaList = new ArrayList<>();
		int currentAuthUserId = authUserId.orElse(0);
		String sql = """
				WITH req AS (
				    SELECT ? auth_user_id, ? problem_id
				)
				SELECT m.id, m.uploader_user_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex,
				       m.description, m.width, m.height, m.is_movie, m.is_360, m.embed_url, m.thumbnail_seconds,
				       DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, ROUND(mp.milliseconds/1000) seconds,
				       ph.id photographer_id, TRIM(CONCAT(ph.firstname, ' ', COALESCE(ph.lastname,''))) photographer_name,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT('id', tu.id, 'name', TRIM(CONCAT(tu.firstname, ' ', COALESCE(tu.lastname,'')))))
				           FROM media_user tmu
				           JOIN user tu ON tmu.user_id = tu.id
				           WHERE tmu.media_id = m.id
				       ) tagged_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT('areaId', ma.area_id, 'areaName', a2.name, 'trivia', ma.trivia))
				           FROM media_area ma
				           JOIN area a2 ON ma.area_id = a2.id
				           WHERE ma.media_id = m.id
				       ) areas_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT('areaId', a2.id, 'areaName', a2.name, 'sectorId', ms.sector_id, 'sectorName', s2.name, 'trivia', ms.trivia))
				           FROM media_sector ms
				           JOIN sector s2 ON ms.sector_id = s2.id
				           JOIN area a2 ON s2.area_id = a2.id
				           WHERE ms.media_id = m.id
				       ) sectors_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
				               'problemId', p2.id, 'problemName', p2.name, 'problemGrade', g.grade, 'problemPitch', mp2.pitch,
				               'problemNumPitches', (SELECT COUNT(*) FROM problem_section ps WHERE ps.problem_id = p2.id),
				               'milliseconds', mp2.milliseconds, 'areaId', a2.id, 'areaName', a2.name, 'sectorName', s2.id, 'sectorName', s2.name, 'trivia', mp2.trivia
				           ))
				           FROM media_problem mp2
				           JOIN problem p2 ON mp2.problem_id = p2.id
				           JOIN sector s2 ON p2.sector_id = s2.id
				           JOIN area a2 ON s2.area_id = a2.id
				           JOIN grade g ON p2.consensus_grade_id = g.id
				           LEFT JOIN user_region ur ON a2.region_id = ur.region_id AND ur.user_id = req.auth_user_id
				           WHERE mp2.media_id = m.id
				             AND p2.trash IS NULL AND ((p2.locked_admin=0 AND p2.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p2.locked_superadmin=0))
				       ) problems_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
						        'trailId', t9.id,
						        'trailTitle', t9.title,
						        'sectors', (
						            SELECT JSON_ARRAYAGG(JSON_OBJECT(
						                'areaId', a9_sub.id,
						                'areaName', a9_sub.name,
						                'sectorId', s9_sub.id,
						                'sectorName', s9_sub.name
						            ))
						            FROM sector_trail st9_sub
						            JOIN sector s9_sub ON st9_sub.sector_id = s9_sub.id
						            JOIN area a9_sub ON s9_sub.area_id = a9_sub.id
						            WHERE st9_sub.trail_id = t9.id
						        )
						    ))
						    FROM media_trail mt9
						    JOIN trail t9 ON mt9.trail_id = t9.id
						    WHERE mt9.media_id = m.id
				       ) trails_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
				               'id', id, 'path', path, 'rappelX', rappel_x, 'rappelY', rappel_y, 'rappelBolted', rappel_bolted
				           ))
				           FROM media_svg
				           WHERE media_id = m.id
				       ) svgs_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
				               'id', s3.id,
				               'problemId', p3.id,
				               'problemName', p3.name,
				               'problemGrade', CASE WHEN s3.pitch IS NULL OR s3.pitch = 0 THEN g3.grade ELSE COALESCE(g_sect3.grade, g3.grade) END,
				               'problemGradeColor', CASE WHEN s3.pitch IS NULL OR s3.pitch = 0 THEN clr3.hex_code ELSE COALESCE(clr_sect3.hex_code, clr3.hex_code) END,
				               'problemSubtype', ty3.subtype,
				               'nr', p3.nr,
				               'pitch', COALESCE(ps3.nr, 0),
				               'path', s3.path,
				               'hasAnchor', s3.has_anchor,
				               'texts', s3.texts,
				               'anchors', s3.anchors,
				               'tradBelayStations', s3.trad_belay_stations,
				               'primary', CASE WHEN p3.type_id IN (1,2) THEN true ELSE false END,
				               'ticked', CASE WHEN (SELECT 1 FROM tick tk3 WHERE tk3.problem_id = p3.id AND tk3.user_id = req.auth_user_id LIMIT 1) IS NOT NULL OR (SELECT 1 FROM fa fa3 WHERE fa3.problem_id = p3.id AND fa3.user_id = req.auth_user_id LIMIT 1) IS NOT NULL THEN true ELSE false END,
				               'todo', CASE WHEN (SELECT 1 FROM todo t3 WHERE t3.problem_id = p3.id AND t3.user_id = req.auth_user_id) IS NOT NULL THEN true ELSE false END,
				               'dangerous', COALESCE((
				                   SELECT gb3.danger 
				                   FROM guestbook gb3 
				                   WHERE gb3.problem_id = p3.id AND (gb3.danger = 1 OR gb3.resolved = 1) 
				                   ORDER BY gb3.id DESC LIMIT 1
				               ), 0) = 1
				           ))
				           FROM svg s3
				           JOIN problem p3 ON s3.problem_id = p3.id
				           JOIN grade g3 ON p3.consensus_grade_id = g3.id
				           JOIN grade_color clr3 ON g3.grade_color_id = clr3.id
				           JOIN type ty3 ON p3.type_id = ty3.id
				           JOIN sector sec3 ON p3.sector_id = sec3.id
				           JOIN area a5 ON sec3.area_id = a5.id
				           LEFT JOIN problem_section ps3 ON ps3.problem_id = p3.id AND ps3.nr = s3.pitch
				           LEFT JOIN grade g_sect3 ON ps3.grade_id = g_sect3.id
				           LEFT JOIN grade_color clr_sect3 ON g_sect3.grade_color_id = clr_sect3.id
				           LEFT JOIN user_region ur3 ON ur3.user_id = req.auth_user_id AND ur3.region_id = a5.region_id
				           WHERE s3.media_id = m.id
				             AND p3.trash IS NULL AND ((p3.locked_admin=0 AND p3.locked_superadmin=0) OR (ur3.superadmin_read=1) OR (ur3.admin_read=1 AND p3.locked_superadmin=0))
				       ) svgs_table_json,
				       COALESCE((SELECT mg.guestbook_id FROM media_guestbook mg WHERE mg.media_id = m.id LIMIT 1), 0) guestbook_id
				FROM req
				JOIN problem p ON p.id = req.problem_id
				JOIN sector s ON p.sector_id=s.id
				JOIN area a ON s.area_id=a.id
				JOIN media_problem mp ON p.id=mp.problem_id
				JOIN media m ON (mp.media_id=m.id AND m.deleted_user_id IS NULL)
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				LEFT JOIN user ph ON m.photographer_user_id=ph.id
				GROUP BY req.auth_user_id, m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.updated_at, p.name, s.name, a.name, m.description, m.width, m.height, m.is_movie, m.is_360, m.embed_url, m.thumbnail_seconds, mp.sorting, m.date_created, m.date_taken, mp.pitch, mp.trivia, mp.milliseconds, ph.id, ph.firstname, ph.lastname
				ORDER BY m.is_movie, m.embed_url, -mp.sorting DESC, m.id
				""";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			int idx = 1;
			ps.setInt(idx++, currentAuthUserId);
			ps.setInt(idx++, problemId);

			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String embedUrl = rst.getString("embed_url");
					long seconds = rst.getLong("seconds");
					if (embedUrl != null && seconds > 0) {
						if (embedUrl.contains("youtu")) {
							embedUrl += "?start=" + seconds;
						} else {
							embedUrl += "#t=" + seconds + "s";
						}
					}
					Media m = Media.fromResultSet(rst, currentAuthUserId);
					m = new Media(
							m.identity(), m.uploadedByMe(), m.width(), m.height(), m.isMovie(), m.is360(),
							m.dateCreated(), m.dateTaken(), m.photographer(), m.tagged(), m.description(),
							m.mediaSvgs(), m.svgProblemId(), m.svgs(), embedUrl, m.thumbnailSeconds(),
							m.inherited(), m.areas(), m.sectors(), m.problems(), m.trails(), m.guestbookId(), m.userAvatarId()
							);
					pMediaList.add(m);
				}
			}
		}
		List<Media> media = null;
		try {
			media = sectorMediaFuture.get();
		} catch (Exception e) {
			throw new SQLException("Failed to aggregate parallel sector media items", e);
		}
		if (media == null) {
			media = new ArrayList<>();
		}
		media.addAll(pMediaList);
		if (media.isEmpty()) {
			media = null;
		}
		logger.debug("getMediaProblem(areaId={}, sectorId={}, problemId={}, showHiddenMedia={}) - media.size()={}, duration={}", areaId, sectorId, problemId, showHiddenMedia, media == null ? 0 : media.size(), stopwatch);
		return media;
	}

	protected List<Media> getMediaSector(Connection c, Setup s, Optional<Integer> authUserId, int idSector, int optionalIdProblem, boolean inherited, int enableMoveToIdArea, int enableMoveToIdSector, int enableMoveToIdProblem, boolean showHiddenMedia) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		List<Media> initialList = new ArrayList<>();
		int currentAuthUserId = authUserId.orElse(0);
		String sql = """
				WITH req AS (
				    SELECT ? auth_user_id, ? sector_id
				)
				SELECT m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, UNIX_TIMESTAMP(m.updated_at) version_stamp,
				       ms.trivia, m.description, m.width, m.height, m.is_movie, m.is_360, m.embed_url, m.thumbnail_seconds,
				       DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken, 
				       ph.id photographer_id, TRIM(CONCAT(ph.firstname, ' ', COALESCE(ph.lastname,''))) photographer_name,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT('id', tu.id, 'name', TRIM(CONCAT(tu.firstname, ' ', COALESCE(tu.lastname,'')))))
				           FROM media_user tmu
				           JOIN user tu ON tmu.user_id = tu.id
				           WHERE tmu.media_id = m.id
				       ) tagged_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT('areaId', ma2.area_id, 'areaName', a2.name, 'trivia', ma2.trivia))
				           FROM media_area ma2
				           JOIN area a2 ON ma2.area_id = a2.id
				           WHERE ma2.media_id = m.id
				       ) areas_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT('areaId', a3.id, 'areaName', a3.name, 'sectorId', ms2.sector_id, 'sectorName', s3.name, 'trivia', ms2.trivia))
				           FROM media_sector ms2
				           JOIN sector s3 ON ms2.sector_id = s3.id
				           JOIN area a3 ON s3.area_id = a3.id
				           WHERE ms2.media_id = m.id
				       ) sectors_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
				               'problemId', p2.id, 'problemName', p2.name, 'problemGrade', g.grade, 'problemPitch', mp2.pitch,
				               'problemNumPitches', (SELECT COUNT(*) FROM problem_section ps WHERE ps.problem_id = p2.id),
				               'milliseconds', mp2.milliseconds, 'areaId', a4.id, 'areaName', a4.name, 'sectorId', s4.id, 'sectorName', s4.name, 'trivia', mp2.trivia
				           ))
				           FROM media_problem mp2
				           JOIN problem p2 ON mp2.problem_id = p2.id
				           JOIN sector s4 ON p2.sector_id = s4.id
				           JOIN area a4 ON s4.area_id = a4.id
				           JOIN grade g ON p2.consensus_grade_id = g.id
				           LEFT JOIN user_region ur ON a4.region_id = ur.region_id AND ur.user_id = req.auth_user_id
				           WHERE mp2.media_id = m.id
				             AND p2.trash IS NULL AND ((p2.locked_admin=0 AND p2.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p2.locked_superadmin=0))
				       ) problems_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
						        'trailId', t9.id,
						        'trailTitle', t9.title,
						        'sectors', (
						            SELECT JSON_ARRAYAGG(JSON_OBJECT(
						                'areaId', a9_sub.id,
						                'areaName', a9_sub.name,
						                'sectorId', s9_sub.id,
						                'sectorName', s9_sub.name
						            ))
						            FROM sector_trail st9_sub
						            JOIN sector s9_sub ON st9_sub.sector_id = s9_sub.id
						            JOIN area a9_sub ON s9_sub.area_id = a9_sub.id
						            WHERE st9_sub.trail_id = t9.id
						        )
						    ))
						    FROM media_trail mt9
						    JOIN trail t9 ON mt9.trail_id = t9.id
						    WHERE mt9.media_id = m.id
				       ) trails_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
				               'id', id, 'path', path, 'rappelX', rappel_x, 'rappelY', rappel_y, 'rappelBolted', rappel_bolted
				           ))
				           FROM media_svg
				           WHERE media_id = m.id
				       ) svgs_json,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT(
				               'id', s3.id,
				               'problemId', p3.id,
				               'problemName', p3.name,
				               'problemGrade', CASE WHEN s3.pitch IS NULL OR s3.pitch = 0 THEN g3.grade ELSE COALESCE(g_sect3.grade, g3.grade) END,
				               'problemGradeColor', CASE WHEN s3.pitch IS NULL OR s3.pitch = 0 THEN clr3.hex_code ELSE COALESCE(clr_sect3.hex_code, clr3.hex_code) END,
				               'problemSubtype', ty3.subtype,
				               'nr', p3.nr,
				               'pitch', COALESCE(ps3.nr, 0),
				               'path', s3.path,
				               'hasAnchor', s3.has_anchor,
				               'texts', s3.texts,
				               'anchors', s3.anchors,
				               'tradBelayStations', s3.trad_belay_stations,
				               'primary', CASE WHEN p3.type_id IN (1,2) THEN true ELSE false END,
				               'ticked', CASE WHEN (SELECT 1 FROM tick tk3 WHERE tk3.problem_id = p3.id AND tk3.user_id = req.auth_user_id LIMIT 1) IS NOT NULL OR (SELECT 1 FROM fa fa3 WHERE fa3.problem_id = p3.id AND fa3.user_id = req.auth_user_id LIMIT 1) IS NOT NULL THEN true ELSE false END,
				               'todo', CASE WHEN (SELECT 1 FROM todo t3 WHERE t3.problem_id = p3.id AND t3.user_id = req.auth_user_id) IS NOT NULL THEN true ELSE false END,
				               'dangerous', COALESCE((
				                   SELECT gb3.danger 
				                   FROM guestbook gb3 
				                   WHERE gb3.problem_id = p3.id AND (gb3.danger = 1 OR gb3.resolved = 1) 
				                   ORDER BY gb3.id DESC LIMIT 1
				               ), 0) = 1
				           ))
				           FROM svg s3
				           JOIN problem p3 ON s3.problem_id = p3.id
				           JOIN grade g3 ON p3.consensus_grade_id = g3.id
				           JOIN grade_color clr3 ON g3.grade_color_id = clr3.id
				           JOIN type ty3 ON p3.type_id = ty3.id
				           JOIN sector sec3 ON p3.sector_id = sec3.id
				           JOIN area a5 ON sec3.area_id = a5.id
				           LEFT JOIN problem_section ps3 ON ps3.problem_id = p3.id AND ps3.nr = s3.pitch
				           LEFT JOIN grade g_sect3 ON ps3.grade_id = g_sect3.id
				           LEFT JOIN grade_color clr_sect3 ON g_sect3.grade_color_id = clr_sect3.id
				           LEFT JOIN user_region ur3 ON ur3.user_id = req.auth_user_id AND ur3.region_id = a5.region_id
				           WHERE s3.media_id = m.id
				             AND p3.trash IS NULL AND ((p3.locked_admin=0 AND p3.locked_superadmin=0) OR (ur3.superadmin_read=1) OR (ur3.admin_read=1 AND p3.locked_superadmin=0))
				       ) svgs_table_json,
				       COALESCE((SELECT mg.guestbook_id FROM media_guestbook mg WHERE mg.media_id = m.id LIMIT 1), 0) guestbook_id
				FROM req
				JOIN sector s ON s.id = req.sector_id
				JOIN area a ON a.id=s.area_id
				JOIN media_sector ms ON s.id=ms.sector_id
				JOIN media m ON ms.media_id=m.id AND m.deleted_user_id IS NULL
				LEFT JOIN media_ml_analysis mma ON m.id=mma.media_id
				LEFT JOIN user ph ON m.photographer_user_id=ph.id
				GROUP BY req.auth_user_id, m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.updated_at, ms.trivia, m.description, s.name, a.name, m.width, m.height, m.is_movie, m.is_360, m.embed_url, m.thumbnail_seconds, ms.sorting, m.date_created, m.date_taken, ph.id, ph.firstname, ph.lastname
				ORDER BY m.is_movie, m.embed_url, -ms.sorting DESC, m.id
				""";
		try (PreparedStatement ps = c.prepareStatement(sql)) {
			int idx = 1;
			ps.setInt(idx++, currentAuthUserId);
			ps.setInt(idx++, idSector);

			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					boolean trivia = rst.getBoolean("trivia");
					if (inherited && trivia) continue; 

					Media m = Media.fromResultSet(rst, currentAuthUserId);

					initialList.add(new Media(
							m.identity(), m.uploadedByMe(), m.width(), m.height(), m.isMovie(), m.is360(),
							m.dateCreated(), m.dateTaken(), m.photographer(), m.tagged(), m.description(),
							m.mediaSvgs(), m.svgProblemId(), m.svgs(), m.embedUrl(), m.thumbnailSeconds(),
							inherited, m.areas(), m.sectors(), m.problems(), m.trails(), m.guestbookId(), m.userAvatarId()
							));
				}
			}
		}

		List<Media> allMedia = new ArrayList<>();
		if (!initialList.isEmpty()) {
			Set<Media> mediaWithRequestedTopoLine = new HashSet<>();
			for (Media m : initialList) {
				if (optionalIdProblem != 0 && m.svgs() != null && m.svgs().stream().anyMatch(svg -> svg.problemId() == optionalIdProblem)) {
					mediaWithRequestedTopoLine.add(m);
				}
				allMedia.add(m);
			}
			if (!showHiddenMedia && !mediaWithRequestedTopoLine.isEmpty()) {
				allMedia = allMedia.stream().filter(m -> m.svgs() == null || m.svgs().isEmpty() || mediaWithRequestedTopoLine.contains(m)).collect(Collectors.toList());
			}
			else if (!showHiddenMedia && s.isBouldering() && optionalIdProblem != 0) {
				allMedia = allMedia.stream().filter(m -> m.svgs() == null || m.svgs().isEmpty()).collect(Collectors.toList());
			}
		}
		logger.debug("getMediaSector(idSector={}, optionalIdProblem={}, inherited={}, enableMoveToIdArea={}, enableMoveIdSector={}, enableMoveIdProblem={}, showHiddenMedia={}) - allMedia.size()={}, duration={}", idSector, optionalIdProblem, inherited, enableMoveToIdArea, enableMoveToIdSector, enableMoveToIdProblem, showHiddenMedia, allMedia.size(), stopwatch);
		return allMedia;
	}
	
	protected Multimap<Integer, Media> getMediaTrails(Connection c, Optional<Integer> authUserId, Collection<Integer> trailIds) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		Multimap<Integer, Media> res = ArrayListMultimap.create();
		if (trailIds.isEmpty()) {
			return res;
		}

		int currentAuthUserId = authUserId.orElse(0);
		String inClause = ",?".repeat(trailIds.size()).substring(1);
		String sql = """
				WITH req AS (
				    SELECT ? auth_user_id
				)
				SELECT mt.trail_id, m.id, m.uploader_user_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, m.description,
					   m.width, m.height, m.is_movie, m.is_360, m.embed_url, m.thumbnail_seconds,
					   DATE_FORMAT(m.date_created,'%Y.%m.%d') date_created, DATE_FORMAT(m.date_taken,'%Y.%m.%d') date_taken,
					   ph.id photographer_id, TRIM(CONCAT(ph.firstname, ' ', COALESCE(ph.lastname,''))) photographer_name,
					   (
						   SELECT JSON_ARRAYAGG(JSON_OBJECT('id', tu.id, 'name', TRIM(CONCAT(tu.firstname, ' ', COALESCE(tu.lastname,'')))))
						   FROM media_user tmu
						   JOIN user tu ON tmu.user_id = tu.id
						   WHERE tmu.media_id = m.id
					   ) tagged_json,
					   (
						   SELECT JSON_ARRAYAGG(JSON_OBJECT('areaId', ma.area_id, 'areaName', a2.name, 'trivia', ma.trivia))
						   FROM media_area ma
						   JOIN area a2 ON ma.area_id = a2.id
						   WHERE ma.media_id = m.id
					   ) areas_json,
					   (
						   SELECT JSON_ARRAYAGG(JSON_OBJECT('areaId', a2.id, 'areaName', a2.name, 'sectorId', ms.sector_id, 'sectorName', s2.name, 'trivia', ms.trivia))
						   FROM media_sector ms
						   JOIN sector s2 ON ms.sector_id = s2.id
						   JOIN area a2 ON s2.area_id = a2.id
						   WHERE ms.media_id = m.id
					   ) sectors_json,
					   (
						   SELECT JSON_ARRAYAGG(JSON_OBJECT(
							   'problemId', p2.id, 'problemName', p2.name, 'problemGrade', g.grade, 'problemPitch', mp.pitch,
							   'problemNumPitches', (SELECT COUNT(*) FROM problem_section ps WHERE ps.problem_id = p2.id),
							   'milliseconds', mp.milliseconds, 'areaId', a2.id, 'areaName', a2.name, 'sectorId', s2.id, 'sectorName', s2.name, 'trivia', mp.trivia
						   ))
						   FROM media_problem mp
						   JOIN problem p2 ON mp.problem_id = p2.id
						   JOIN sector s2 ON p2.sector_id = s2.id
						   JOIN area a2 ON s2.area_id = a2.id
						   JOIN grade g ON p2.consensus_grade_id = g.id
						   LEFT JOIN user_region ur ON a2.region_id = ur.region_id AND ur.user_id = req.auth_user_id
						   WHERE mp.media_id = m.id
							 AND p2.trash IS NULL AND ((p2.locked_admin=0 AND p2.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p2.locked_superadmin=0))
					   ) problems_json,
					   (
						   SELECT JSON_ARRAYAGG(JSON_OBJECT(
						        'trailId', t9.id,
						        'trailTitle', t9.title,
						        'sectors', (
						            SELECT JSON_ARRAYAGG(JSON_OBJECT(
						                'areaId', a9_sub.id,
						                'areaName', a9_sub.name,
						                'sectorId', s9_sub.id,
						                'sectorName', s9_sub.name
						            ))
						            FROM sector_trail st9_sub
						            JOIN sector s9_sub ON st9_sub.sector_id = s9_sub.id
						            JOIN area a9_sub ON s9_sub.area_id = a9_sub.id
						            WHERE st9_sub.trail_id = t9.id
						        )
						    ))
						    FROM media_trail mt9
						    JOIN trail t9 ON mt9.trail_id = t9.id
						    WHERE mt9.media_id = m.id
					   ) trails_json,
					   (
						   SELECT JSON_ARRAYAGG(JSON_OBJECT(
							   'id', id, 'path', path, 'rappelX', rappel_x, 'rappelY', rappel_y, 'rappelBolted', rappel_bolted
						   ))
						   FROM media_svg
						   WHERE media_id = m.id
					   ) svgs_json,
					   (
						   SELECT JSON_ARRAYAGG(JSON_OBJECT(
							   'id', s3.id,
							   'problemId', p3.id,
							   'problemName', p3.name,
								   'problemGrade', CASE WHEN s3.pitch IS NULL OR s3.pitch = 0 THEN g3.grade ELSE COALESCE(g_sect3.grade, g3.grade) END,
							   'problemGradeColor', CASE WHEN s3.pitch IS NULL OR s3.pitch = 0 THEN clr3.hex_code ELSE COALESCE(clr_sect3.hex_code, clr3.hex_code) END,
							   'problemSubtype', ty3.subtype,
							   'nr', p3.nr,
							   'pitch', COALESCE(ps3.nr, 0),
							   'path', s3.path,
							   'hasAnchor', s3.has_anchor,
							   'texts', s3.texts,
							   'anchors', s3.anchors,
							   'tradBelayStations', s3.trad_belay_stations,
							   'primary', CASE WHEN p3.type_id IN (1,2) THEN true ELSE false END,
							   'ticked', CASE WHEN (SELECT 1 FROM tick tk3 WHERE tk3.problem_id = p3.id AND tk3.user_id = req.auth_user_id LIMIT 1) IS NOT NULL OR (SELECT 1 FROM fa fa3 WHERE fa3.problem_id = p3.id AND fa3.user_id = req.auth_user_id LIMIT 1) IS NOT NULL THEN true ELSE false END,
							   'todo', CASE WHEN (SELECT 1 FROM todo t3 WHERE t3.problem_id = p3.id AND t3.user_id = req.auth_user_id) IS NOT NULL THEN true ELSE false END,
							   'dangerous', COALESCE((
								   SELECT gb3.danger 
								   FROM guestbook gb3 
								   WHERE gb3.problem_id = p3.id AND (gb3.danger = 1 OR gb3.resolved = 1) 
								   ORDER BY gb3.id DESC LIMIT 1
								), 0) = 1
						   ))
						   FROM svg s3
						   JOIN problem p3 ON s3.problem_id = p3.id
						   JOIN grade g3 ON p3.consensus_grade_id = g3.id
						   JOIN grade_color clr3 ON g3.grade_color_id = clr3.id
						   JOIN type ty3 ON p3.type_id = ty3.id
						   JOIN sector sec3 ON p3.sector_id = sec3.id
						   JOIN area a5 ON sec3.area_id = a5.id
						   LEFT JOIN problem_section ps3 ON ps3.problem_id = p3.id AND ps3.nr = s3.pitch
						   LEFT JOIN grade g_sect3 ON ps3.grade_id = g_sect3.id
						   LEFT JOIN grade_color clr_sect3 ON g_sect3.grade_color_id = clr_sect3.id
						   LEFT JOIN user_region ur3 ON ur3.user_id = req.auth_user_id AND ur3.region_id = a5.region_id
						   WHERE s3.media_id = m.id
							 AND p3.trash IS NULL AND ((p3.locked_admin=0 AND p3.locked_superadmin=0) OR (ur3.superadmin_read=1) OR (ur3.admin_read=1 AND p3.locked_superadmin=0))
					   ) svgs_table_json,
					   COALESCE((SELECT mg.guestbook_id FROM media_guestbook mg WHERE mg.media_id = m.id LIMIT 1), 0) guestbook_id
				FROM req
				CROSS JOIN media_trail mt
				JOIN media m ON (mt.media_id = m.id AND m.deleted_user_id IS NULL)
				LEFT JOIN media_ml_analysis mma ON m.id = mma.media_id
				LEFT JOIN user ph ON m.photographer_user_id = ph.id
				WHERE mt.trail_id IN (__IN_CLAUSE__)
				GROUP BY req.auth_user_id, mt.trail_id, m.id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.uploader_user_id, m.updated_at, m.description, m.width, m.height, m.is_movie, m.is_360, m.embed_url, m.thumbnail_seconds, m.date_created, m.date_taken, ph.id, ph.firstname, ph.lastname, mt.sorting
				ORDER BY m.is_movie, m.embed_url, -mt.sorting DESC, m.id
				""".replace("__IN_CLAUSE__", inClause);

		try (PreparedStatement ps = c.prepareStatement(sql)) {
			int idx = 1;
			ps.setInt(idx++, currentAuthUserId);
			for (int trailId : trailIds) {
				ps.setInt(idx++, trailId);
			}
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int trailId = rst.getInt("trail_id");
					res.put(trailId, Media.fromResultSet(rst, currentAuthUserId));
				}
			}
		}
		logger.debug("getMediaTrails(trailIds.size()={}) - res.size()={}, duration={}", trailIds.size(), res.size(), stopwatch);
		return res;
	}
}