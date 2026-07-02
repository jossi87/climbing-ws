package com.buldreinfo.dao;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.beans.StorageType;
import com.buldreinfo.exception.ForbiddenException;
import com.buldreinfo.model.Media;
import com.buldreinfo.model.Media.Association;
import com.buldreinfo.model.MediaSvgElementType;
import com.buldreinfo.model.Svg;
import com.buldreinfo.service.ImageClassifierService;
import com.buldreinfo.service.ImageClassifierService.MediaObject.NormalizedVertex;
import com.buldreinfo.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class MediaRepository {
	public record MediaPendingAnalysis(int id, int width, int height) {}
	public record EmbeddedVideo(int id, String suffix, String embedUrl) {}
	public record MediaAssociation(String table, String column, int columnId, boolean hasPitch) {}

	private static final Logger logger = LogManager.getLogger();
	private final JdbcClient jdbcClient;
	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public MediaRepository(
			JdbcClient jdbcClient,
			JdbcTemplate jdbcTemplate,
			ObjectMapper objectMapper) {
		this.jdbcClient = jdbcClient;
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public void deleteMediaAnalysis(int idMedia) {
		jdbcClient.sql("DELETE FROM media_ml_label WHERE media_id=?").param(1, idMedia).update();
		jdbcClient.sql("DELETE FROM media_ml_object WHERE media_id=?").param(1, idMedia).update();
		jdbcClient.sql("DELETE FROM media_ml_analysis WHERE media_id=?").param(1, idMedia).update();

		logger.debug("Deleted existing AI analysis for idMedia={}", idMedia);
	}

	@Transactional(readOnly = true)
	public int getDailyInstagramScrapeCount(Optional<Integer> authUserId) {
		return jdbcClient.sql("SELECT COUNT(*) FROM instagram_scrape_log WHERE user_id = ? AND created_at >= NOW() - INTERVAL 1 DAY")
				.param(1, authUserId.orElseThrow())
				.query(Integer.class)
				.single();
	}

	@Transactional(readOnly = true)
	public List<EmbeddedVideo> getEmbeddedVideos() {
		return jdbcClient.sql("SELECT id, suffix, embed_url FROM media WHERE is_movie=1 AND embed_url IS NOT NULL")
				.query((rs, _) -> new EmbeddedVideo(rs.getInt("id"), rs.getString("suffix"), rs.getString("embed_url")))
				.list();
	}

	@Transactional(readOnly = true)
	public Media getMedia(Optional<Integer> authUserId, int id) {
		var sql = """
				WITH req AS (
				    SELECT ? auth_user_id, ? media_id
				)
				SELECT m.id, m.uploader_user_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex,
				       m.description, m.width, m.height, m.is_movie, m.suffix, m.is_360, m.embed_url, m.thumbnail_seconds,
				       m.date_created, m.date_taken,
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
		return jdbcClient.sql(sql)
				.params(authUserId.orElse(0), id)
				.query((rs, _) -> Media.fromResultSet(objectMapper, rs, authUserId))
				.optional()
				.orElseThrow(() -> new NoSuchElementException("Could not find media with id=" + id));
	}

	@Transactional(readOnly = true)
	public List<Media> getMediaArea(Optional<Integer> authUserId, int id, boolean inherited) {
		var sql = """
				WITH req AS (
				    SELECT ? auth_user_id, ? area_id
				)
				SELECT a.name as area_name, m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, UNIX_TIMESTAMP(m.updated_at) version_stamp, m.description, ma.trivia, m.width, m.height, m.is_movie, m.suffix, m.is_360, m.embed_url, m.thumbnail_seconds,
				       m.date_created, m.date_taken, 
				       p.id photographer_id, TRIM(CONCAT(p.firstname, ' ', COALESCE(p.lastname,''))) photographer_name,
				       (
				           SELECT JSON_ARRAYAGG(JSON_OBJECT('id', tu.id, 'name', TRIM(CONCAT(tu.firstname, ' ', COALESCE(tu.lastname,'')))))
				           FROM media_user tmu
				           JOIN user tu ON tmu.media_id = tu.id
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
				GROUP BY req.auth_user_id, a.name, m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.updated_at, ma.trivia, m.description, m.width, m.height, m.is_movie, m.suffix, m.is_360, m.embed_url, m.thumbnail_seconds, ma.sorting, m.date_created, m.date_taken, p.id, p.firstname, p.lastname
				ORDER BY m.is_movie, m.embed_url, -ma.sorting DESC, m.id
				""";
		return jdbcClient.sql(sql)
				.params(authUserId.orElse(0), id)
				.query((rs, _) -> {
					if (inherited && rs.getBoolean("trivia")) {
						return null;
					}
					Media m = Media.fromResultSet(objectMapper, rs, authUserId);
					return new Media(
							m.identity(), m.uploadedByMe(), m.width(), m.height(), m.isMovie(), m.suffix(), m.is360(),
							m.dateCreated(), m.dateTaken(), m.photographer(), m.tagged(), m.description(),
							m.mediaSvgs(), m.svgProblemId(), m.svgs(), m.embedUrl(), m.thumbnailSeconds(),
							inherited, m.areas(), m.sectors(), m.problems(), m.trails(), m.guestbookId(), m.userAvatarId()
							);
				})
				.list()
				.stream()
				.filter(Objects::nonNull)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<Media> getMediaGuestbook(Optional<Integer> authUserId, int guestbookId) {
		var startNanos = System.nanoTime();
		var sql = """
				WITH req AS (
				    SELECT ? auth_user_id, ? guestbook_id
				)
				SELECT m.id, m.uploader_user_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, m.description,
				       m.width, m.height, m.is_movie, m.suffix, m.is_360, m.embed_url, m.thumbnail_seconds,
				       m.date_created, m.date_taken,
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
				GROUP BY req.auth_user_id, m.id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.uploader_user_id, m.updated_at, m.description, m.width, m.height, m.is_movie, m.suffix, m.is_360, m.embed_url, m.thumbnail_seconds, m.date_created, m.date_taken, ph.id, ph.firstname, ph.lastname, mg.guestbook_id
				ORDER BY m.is_movie, m.embed_url, m.id
				""";
		List<Media> res = jdbcClient.sql(sql)
				.params(authUserId.orElse(0), guestbookId)
				.query((rs, _) -> Media.fromResultSet(objectMapper, rs, authUserId))
				.list();

		logger.debug("getMediaGuestbook(guestbookId={}) - res.size={}, duration={}", guestbookId, res.size(), Duration.ofNanos(System.nanoTime() - startNanos));
		return res;
	}

	@Transactional(readOnly = true)
	public List<MediaPendingAnalysis> getMediaPendingAnalysis() {
		return jdbcClient.sql("""
				SELECT id, width, height
				FROM media m
				WHERE NOT EXISTS (SELECT x.media_id FROM media_ml_analysis x WHERE x.media_id=m.id)
				GROUP BY id, width, height
				""")
				.query((rs, _) -> new MediaPendingAnalysis(
						rs.getInt("id"), 
						rs.getInt("width"), 
						rs.getInt("height")
						))
				.list();
	}

	@Transactional(readOnly = true)
	public List<Media> getMediaProblemRaw(Optional<Integer> authUserId, int problemId) {
		var sql = """
				 		WITH req AS (
				    SELECT ? auth_user_id, ? problem_id
				)
				SELECT m.id, m.uploader_user_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex,
				       m.description, m.width, m.height, m.is_movie, m.suffix, m.is_360, m.embed_url, m.thumbnail_seconds,
				       m.date_created, m.date_taken, ROUND(mp.milliseconds/1000) seconds,
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
				GROUP BY req.auth_user_id, m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.updated_at, p.name, s.name, a.name, m.description, m.width, m.height, m.is_movie, m.suffix, m.is_360, m.embed_url, m.thumbnail_seconds, mp.sorting, m.date_created, m.date_taken, mp.pitch, mp.trivia, mp.milliseconds, ph.id, ph.firstname, ph.lastname
				ORDER BY m.is_movie, m.embed_url, -mp.sorting DESC, m.id
				 		""";
		return jdbcClient.sql(sql)
				.params(authUserId.orElse(0), problemId)
				.query((rs, _) -> {
					var embedUrl = rs.getString("embed_url");
					long seconds = rs.getLong("seconds");
					if (embedUrl != null && seconds > 0) {
						embedUrl += embedUrl.contains("youtu") ? "?start=" + seconds : "#t=" + seconds + "s";
					}
					var m = Media.fromResultSet(objectMapper, rs, authUserId);
					return new Media(
							m.identity(), m.uploadedByMe(), m.width(), m.height(), m.isMovie(), m.suffix(), m.is360(),
							m.dateCreated(), m.dateTaken(), m.photographer(), m.tagged(), m.description(),
							m.mediaSvgs(), m.svgProblemId(), m.svgs(), embedUrl, m.thumbnailSeconds(),
							m.inherited(), m.areas(), m.sectors(), m.problems(), m.trails(), m.guestbookId(), m.userAvatarId()
							);
				}).list();
	}

	@Transactional(readOnly = true)
	public List<Media> getMediaSectorRaw(Optional<Integer> authUserId, int idSector, boolean inherited) {
		var sql = """
				WITH req AS (
				    SELECT ? auth_user_id, ? sector_id
				)
				SELECT m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, UNIX_TIMESTAMP(m.updated_at) version_stamp,
				       ms.trivia, m.description, m.width, m.height, m.is_movie, m.suffix, m.is_360, m.embed_url, m.thumbnail_seconds,
				       m.date_created, m.date_taken, 
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
				GROUP BY req.auth_user_id, m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.updated_at, ms.trivia, m.description, s.name, a.name, m.width, m.height, m.is_movie, m.suffix, m.is_360, m.embed_url, m.thumbnail_seconds, ms.sorting, m.date_created, m.date_taken, ph.id, ph.firstname, ph.lastname
				ORDER BY m.is_movie, m.embed_url, -ms.sorting DESC, m.id
				""";
		return jdbcClient.sql(sql)
				.params(authUserId.orElse(0), idSector)
				.query((rs, _) -> {
					var trivia = rs.getBoolean("trivia");
					if (!(inherited && trivia)) {
						var m = Media.fromResultSet(objectMapper, rs, authUserId);
						return new Media(
								m.identity(), m.uploadedByMe(), m.width(), m.height(), m.isMovie(), m.suffix(), m.is360(),
								m.dateCreated(), m.dateTaken(), m.photographer(), m.tagged(), m.description(),
								m.mediaSvgs(), m.svgProblemId(), m.svgs(), m.embedUrl(), m.thumbnailSeconds(),
								inherited, m.areas(), m.sectors(), m.problems(), m.trails(), m.guestbookId(), m.userAvatarId()
								);
					}
					return null;
				})
				.list()
				.stream()
				.filter(Objects::nonNull)
				.toList();
	}

	@Transactional(readOnly = true)
	public Map<Integer, List<Media>> getMediaTrails(Optional<Integer> authUserId, Collection<Integer> trailIds) {
		var startNanos = System.nanoTime();
		var res = new HashMap<Integer, List<Media>>();
		if (trailIds.isEmpty()) {
			return res;
		}

		var inClause = String.join(",", Collections.nCopies(trailIds.size(), "?"));
		var sql = """
				WITH req AS (
				    SELECT ? auth_user_id
				)
				SELECT mt.trail_id, m.id, m.uploader_user_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, m.description,
				       m.width, m.height, m.is_movie, m.suffix, m.is_360, m.embed_url, m.thumbnail_seconds,
				       m.date_created, m.date_taken,
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
				WHERE mt.trail_id IN (%s)
				GROUP BY req.auth_user_id, mt.trail_id, m.id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.uploader_user_id, m.updated_at, m.description, m.width, m.height, m.is_movie, m.suffix, m.is_360, m.embed_url, m.thumbnail_seconds, m.date_created, m.date_taken, ph.id, ph.firstname, ph.lastname, mt.sorting
				ORDER BY m.is_movie, m.embed_url, -mt.sorting DESC, m.id
				""".formatted(inClause);

		List<Object> args = new ArrayList<>();
		args.add(authUserId.orElse(0));
		args.addAll(trailIds);

		jdbcClient.sql(sql)
		.params(args)
		.query((rs) -> {
			var trailId = rs.getInt("trail_id");
			res.computeIfAbsent(trailId, _ -> new ArrayList<>()).add(Media.fromResultSet(objectMapper, rs, authUserId));
		});
		logger.debug("getMediaTrails(trailIds.size()={}) - res.size()={}, duration={}", trailIds.size(), res.size(), Duration.ofNanos(System.nanoTime() - startNanos));
		return res;
	}

	@Transactional(readOnly = true)
	public List<Media> getProfileMedia(Optional<Integer> authUserId, int reqId, boolean captured) {
		var startNanos = System.nanoTime();
		var targetFilter = captured 
				? "m.photographer_user_id = req.target_user_id" 
						: "EXISTS (SELECT 1 FROM media_user mu WHERE mu.media_id = m.id AND mu.user_id = req.target_user_id)";
		var sql = """
				WITH req AS (
				    SELECT ? target_user_id, ? auth_user_id
				)
				SELECT m.id, m.uploader_user_id, UNIX_TIMESTAMP(m.updated_at) version_stamp, mma.focus_x, mma.focus_y, mma.primary_color_hex media_primary_color_hex, m.description,
				       m.width, m.height, m.is_movie, m.suffix, m.is_360, m.embed_url, m.thumbnail_seconds,
				       m.date_created, m.date_taken,
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

				GROUP BY req.auth_user_id, m.id, m.uploader_user_id, mma.focus_x, mma.focus_y, mma.primary_color_hex, m.updated_at, m.description, m.width, m.height, m.is_movie, m.suffix, m.is_360, m.embed_url, m.thumbnail_seconds, m.date_created, m.date_taken, ph.id, ph.firstname, ph.lastname
				ORDER BY m.id DESC
				""".replace("__TARGET_FILTER__", targetFilter);

		List<Media> res = jdbcClient.sql(sql)
				.params(reqId, authUserId.orElse(0))
				.query((rs, _) -> Media.fromResultSet(objectMapper, rs, authUserId))
				.list();

		logger.debug("getProfileMedia(reqId={}, captured={}, duration={})", reqId, captured, Duration.ofNanos(System.nanoTime() - startNanos));
		return res;
	}

	@Transactional
	public void logInstagramScrape(Optional<Integer> authUserId, String shortcode, String originalUrl, int slideCount) {
		jdbcClient.sql("INSERT INTO instagram_scrape_log (user_id, shortcode, original_url, slide_count) VALUES (?, ?, ?, ?)")
		.params(authUserId.orElseThrow(), shortcode, originalUrl, slideCount)
		.update();
	}

	@Transactional
	public void saveMediaAnalysis(int mediaId, int imageWidth, int imageHeight, String hexColor, List<String> labels, List<ImageClassifierService.MediaObject> objects, boolean failed) {
		if (mediaId <= 0) throw new IllegalArgumentException("Media id required");

		jdbcClient.sql("DELETE FROM media_ml_analysis WHERE media_id = ?").param(mediaId).update();
		jdbcClient.sql("DELETE FROM media_ml_label WHERE media_id = ?").param(mediaId).update();
		jdbcClient.sql("DELETE FROM media_ml_object WHERE media_id = ?").param(mediaId).update();

		boolean hasPersonObject = objects != null && objects.stream().anyMatch(obj -> obj.name().equalsIgnoreCase("Person"));
		int focusX = 0;
		int focusY = 0;

		if (hasPersonObject) {
			var climber = objects.stream()
					.filter(obj -> obj.name().equalsIgnoreCase("Person"))
					.min(Comparator.comparing(obj -> obj.boundingPoly().normalizedVertices().stream()
							.map(NormalizedVertex::y)
							.min(Float::compare).orElse(1.0f)))
					.orElse(null);

			if (climber != null) {
				var vertices = climber.boundingPoly().normalizedVertices();

				float xMin = vertices.stream().map(NormalizedVertex::x).min(Float::compare).orElse(0.0f);
				float xMax = vertices.stream().map(NormalizedVertex::x).max(Float::compare).orElse(0.0f);
				float yMin = vertices.stream().map(NormalizedVertex::y).min(Float::compare).orElse(0.0f);
				float yMax = vertices.stream().map(NormalizedVertex::y).max(Float::compare).orElse(0.0f);

				float personHeight = yMax - yMin;

				focusX = Math.round(((xMin + xMax) / 2) * 100);

				if (imageHeight > imageWidth) {
					focusY = (yMax > 0.80f && personHeight < 0.60f) 
							? Math.round(yMax * 100) 
									: Math.round((yMin + (personHeight * 0.85f)) * 100);
				} else {
					focusY = Math.round(((yMin + yMax) / 2) * 100);
				}
			}
		}

		jdbcClient.sql("INSERT INTO media_ml_analysis (media_id, primary_color_hex, focus_x, focus_y, is_action_shot, failed) VALUES (?, ?, ?, ?, ?, ?)")
		.params(mediaId, hexColor, focusX, focusY, hasPersonObject, failed)
		.update();

		if (!failed) {
			if (labels != null) {
				var labelSql = "INSERT INTO media_ml_label (media_id, description, score) VALUES (?, ?, ?)";
				for (String label : labels) {
					jdbcClient.sql(labelSql).params(mediaId, label, 0f).update();
				}
			}
			if (objects != null) {
				var objSql = "INSERT INTO media_ml_object (media_id, name, score, x_min, y_min, x_max, y_max) VALUES (?, ?, ?, ?, ?, ?, ?)";
				for (var obj : objects) {
					float xMin = obj.boundingPoly().normalizedVertices().stream().map(NormalizedVertex::x).min(Float::compare).orElse(0f);
					float xMax = obj.boundingPoly().normalizedVertices().stream().map(NormalizedVertex::x).max(Float::compare).orElse(0f);
					float yMin = obj.boundingPoly().normalizedVertices().stream().map(NormalizedVertex::y).min(Float::compare).orElse(0f);
					float yMax = obj.boundingPoly().normalizedVertices().stream().map(NormalizedVertex::y).max(Float::compare).orElse(0f);

					jdbcClient.sql(objSql).params(mediaId, obj.name(), obj.score(), xMin, yMin, xMax, yMax).update();
				}
			}
		}
	}

	@Transactional
	public void setMediaMetadata(int idMedia, int width, int height, LocalDateTime dateTaken, boolean is360) {
		if (dateTaken == null) {
			jdbcClient.sql("UPDATE media SET width=?, height=?, is_360=? WHERE id=?")
			.params(width, height, is360, idMedia)
			.update();
		} else {
			jdbcClient.sql("UPDATE media SET date_taken=?, width=?, height=?, is_360=? WHERE id=?")
			.params(dateTaken, width, height, is360, idMedia)
			.update();
		}
		logger.debug("setMediaMetadata(idMedia={}, width={}, height={}, dateTaken={}, is360={}) - success", idMedia, width, height, dateTaken, is360);
	}

	@Transactional
	public void updateMediaFocusAndActionStatus() {
		jdbcClient.sql("""
				UPDATE media_ml_analysis mla
				JOIN media m ON mla.media_id = m.id
				LEFT JOIN (
				    SELECT media_id, x_min, x_max, y_min, y_max
				    FROM (
				        SELECT media_id, x_min, x_max, y_min, y_max,
				               ROW_NUMBER() OVER (PARTITION BY media_id ORDER BY y_min ASC) rn
				        FROM media_ml_object
				        WHERE name = 'Person'
				    ) t
				    WHERE rn = 1
				) best_person ON mla.media_id = best_person.media_id
				SET 
				    mla.focus_x = CASE 
				        WHEN best_person.y_min IS NOT NULL THEN ROUND(((best_person.x_min + best_person.x_max) / 2) * 100)
				        ELSE mla.focus_x
				    END,

				    mla.focus_y = CASE 
				        WHEN best_person.y_min IS NOT NULL THEN 
				            CASE 
				                WHEN m.height > m.width THEN 
				                    CASE 
				                        WHEN best_person.y_max > 0.80 AND (best_person.y_max - best_person.y_min) < 0.60 
				                            THEN ROUND(best_person.y_max * 100)
				                        ELSE ROUND((best_person.y_min + (best_person.y_max - best_person.y_min) * 0.85) * 100)
				                    END
				                ELSE ROUND(((best_person.y_min + best_person.y_max) / 2) * 100)
				            END
				        ELSE mla.focus_y
				    END,

				    mla.is_action_shot = IF(best_person.y_min IS NOT NULL, 1, 0)
				WHERE mla.media_id > 0
				""")
		.update();
	}

	@Transactional
	public void upsertMediaSvg(Media m) {
		jdbcClient.sql("DELETE FROM media_svg WHERE media_id = ?").param(m.identity().id()).update();

		for (var element : m.mediaSvgs()) {
			if (element.t().equals(MediaSvgElementType.PATH)) {
				jdbcClient.sql("INSERT INTO media_svg (media_id, path) VALUES (?, ?)")
				.params(m.identity().id(), element.path())
				.update();
			} else if (element.t().equals(MediaSvgElementType.RAPPEL_BOLTED) || element.t().equals(MediaSvgElementType.RAPPEL_NOT_BOLTED)) {
				jdbcClient.sql("INSERT INTO media_svg (media_id, rappel_x, rappel_y, rappel_bolted) VALUES (?, ?, ?, ?)")
				.params(
						m.identity().id(), 
						element.rappelX(), 
						element.rappelY(), 
						element.t().equals(MediaSvgElementType.RAPPEL_BOLTED)
						)
				.update();
			} else {
				throw new RuntimeException("Invalid type: " + element.t());
			}
		}
	}

	@Transactional
	public void upsertSvgDb(int problemId, int pitch, int mediaId, Svg svg) {
		if (svg.delete() || StringUtils.stripToNull(svg.path()) == null) {
			if (pitch == 0) {
				jdbcClient.sql("DELETE FROM svg WHERE media_id = ? AND problem_id = ? AND pitch IS NULL")
				.params(mediaId, problemId)
				.update();
			} else {
				jdbcClient.sql("DELETE FROM svg WHERE media_id = ? AND problem_id = ? AND pitch = ?")
				.params(mediaId, problemId, pitch)
				.update();
			}
		} else if (svg.id() <= 0) {
			jdbcClient.sql("INSERT INTO svg (media_id, problem_id, pitch, path, has_anchor, anchors, trad_belay_stations, texts) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
			.params(mediaId, problemId, pitch > 0 ? pitch : null, svg.path(), svg.hasAnchor(), svg.anchors(), svg.tradBelayStations(), svg.texts())
			.update();
		} else {
			jdbcClient.sql("UPDATE svg SET media_id = ?, problem_id = ?, pitch = ?, path = ?, has_anchor = ?, anchors = ?, trad_belay_stations = ?, texts = ? WHERE id = ?")
			.params(mediaId, problemId, pitch > 0 ? pitch : null, svg.path(), svg.hasAnchor(), svg.anchors(), svg.tradBelayStations(), svg.texts(), svg.id())
			.update();
		}
	}

	@Transactional(readOnly = true)
	public List<Integer> getProblemIdsForMedia(int idMedia) {
		return jdbcClient.sql("SELECT problem_id FROM media_problem WHERE media_id=?")
				.param(1, idMedia)
				.query((rs, _) -> rs.getInt("problem_id"))
				.list();
	}

	@Transactional
	public void markMediaDeleted(int authUserId, int idMedia) {
		jdbcClient.sql("UPDATE media SET deleted_user_id=?, deleted_timestamp=NOW() WHERE id=?")
		.params(authUserId, idMedia)
		.update();
	}

	@Transactional(readOnly = true)
	public boolean isMediaAuthorizedForUser(int authUserId, int idMedia) {
		var sql = """
				WITH req AS (
					SELECT ? auth_user_id, ? media_id
				)
				SELECT ur.admin_write, ur.superadmin_write, ma.area_id, ms.sector_id, mp.problem_id, mg.guestbook_id, mt.trail_id
				FROM req
				JOIN user_region ur ON ur.user_id = req.auth_user_id
				JOIN area a ON a.region_id = ur.region_id
				LEFT JOIN media_area ma ON a.id = ma.area_id AND ma.media_id = req.media_id
				LEFT JOIN media_sector ms ON a.id = (SELECT area_id FROM sector WHERE id = ms.sector_id) AND ms.media_id = req.media_id
				LEFT JOIN media_problem mp ON mp.media_id = req.media_id
				LEFT JOIN media_guestbook mg ON mg.media_id = req.media_id
				LEFT JOIN media_trail mt ON mt.media_id = req.media_id
				WHERE ma.media_id IS NOT NULL OR ms.media_id IS NOT NULL OR mp.media_id IS NOT NULL OR mg.media_id IS NOT NULL OR mt.media_id IS NOT NULL
				""";

		return jdbcClient.sql(sql)
				.params(authUserId, idMedia)
				.query((rs, _) -> {
					boolean isAdmin = rs.getBoolean("admin_write") || rs.getBoolean("superadmin_write");
					boolean hasContext = rs.getInt("area_id") > 0 || rs.getInt("sector_id") > 0 || 
							rs.getInt("problem_id") > 0 || rs.getInt("guestbook_id") > 0 || 
							rs.getInt("trail_id") > 0;
							return isAdmin && hasContext;
				})
				.stream()
				.anyMatch(Boolean::booleanValue);
	}

	@Transactional(readOnly = true)
	public MediaAssociation getMediaAssociation(int authId, int id) {
		return jdbcClient.sql("""
				WITH req AS (SELECT ? auth_user_id, ? media_id)
				SELECT ur.admin_write, ur.superadmin_write, m.uploader_user_id,
				       MAX(ma.area_id) area_id, MAX(ms.sector_id) sector_id, MAX(mp.problem_id) problem_id, MAX(mt.trail_id) trail_id
				FROM req
				JOIN media m ON m.id = req.media_id
				JOIN area a ON true
				JOIN sector s ON a.id = s.area_id
				JOIN user_region ur ON a.region_id = ur.region_id AND ur.user_id = req.auth_user_id
				LEFT JOIN media_area ma ON a.id = ma.area_id AND m.id = ma.media_id
				LEFT JOIN media_sector ms ON s.id = ms.sector_id AND m.id = ms.media_id
				LEFT JOIN problem p ON s.id = p.sector_id
				LEFT JOIN media_problem mp ON p.id = mp.problem_id AND mp.media_id = req.media_id
				LEFT JOIN sector_trail st ON s.id = st.sector_id
				LEFT JOIN media_trail mt ON st.trail_id = mt.trail_id AND m.id = mt.media_id
				WHERE ma.media_id IS NOT NULL 
				   OR ms.media_id IS NOT NULL 
				   OR mp.media_id IS NOT NULL 
				   OR mt.media_id IS NOT NULL
				GROUP BY ur.admin_write, ur.superadmin_write, m.uploader_user_id
				""")
				.params(authId, id)
				.query((rs, _) -> {
					boolean ok = rs.getBoolean("admin_write") || rs.getBoolean("superadmin_write") || rs.getInt("uploader_user_id") == authId;
					if (!ok) throw new ForbiddenException("Insufficient permissions");

					int areaId = rs.getInt("area_id");
					int sectorId = rs.getInt("sector_id");
					int problemId = rs.getInt("problem_id");
					int trailId = rs.getInt("trail_id");

					if (areaId > 0) return new MediaAssociation("media_area", "area_id", areaId, false);
					if (sectorId > 0) return new MediaAssociation("media_sector", "sector_id", sectorId, false);
					if (trailId > 0) return new MediaAssociation("media_trail", "trail_id", trailId, false);
					if (problemId > 0) return new MediaAssociation("media_problem", "problem_id", problemId, true);
					throw new UnsupportedOperationException("Could not find media association");
				})
				.single();
	}

	@Transactional(readOnly = true)
	public List<Integer> getMediaIdsForSorting(MediaAssociation result) {
		var orderClause = result.hasPitch() ? "IFNULL(x.pitch,0), " : "";
		var sql = "SELECT m.id FROM %s x, media m WHERE x.%s=? AND x.media_id=m.id AND m.deleted_user_id IS NULL AND m.is_movie=0 ORDER BY %s -x.sorting DESC, m.id"
				.formatted(result.table(), result.column(), orderClause);

		return jdbcClient.sql(sql)
				.param(result.columnId())
				.query((rs, _) -> rs.getInt("id"))
				.list();
	}

	@Transactional
	public void batchUpdateSorting(MediaAssociation result, List<Integer> idMediaList) {
		var updateSql = "UPDATE %s SET sorting=? WHERE %s=? AND media_id=?".formatted(result.table(), result.column());
		jdbcTemplate.batchUpdate(updateSql, idMediaList, idMediaList.size(), (ps, mediaId) -> {
			int i = idMediaList.indexOf(mediaId) + 1;
			ps.setInt(1, i);
			ps.setInt(2, result.columnId());
			ps.setInt(3, mediaId);
		});
	}

	@Transactional
	public void updateMediaMetadata(String description, int photographerId, double thumbnailSeconds, int mediaId, boolean thumbnailChanged) {
		var sql = thumbnailChanged 
				? "UPDATE media SET description=?, photographer_user_id=?, thumbnail_seconds=?, updated_at=NOW() WHERE id=?"
						: "UPDATE media SET description=?, photographer_user_id=?, thumbnail_seconds=? WHERE id=?";

		jdbcClient.sql(sql)
		.params(description != null && !description.isBlank() ? description : null, photographerId, thumbnailSeconds, mediaId)
		.update();
	}

	@Transactional
	public int insertMediaMetadata(int uploaderId, Media m, StorageType storageType, int photographerId) {
		var keyHolder = new GeneratedKeyHolder();

		int affectedRows = jdbcClient.sql("""
				INSERT INTO media (is_movie, suffix, photographer_user_id, uploader_user_id, date_created, description, thumbnail_seconds, embed_url) 
				VALUES (?, ?, ?, ?, NOW(), ?, ?, ?)
				""")
				.params(
						storageType.isMovie(),
						storageType.getExtension(),
						photographerId,
						uploaderId,
						StringUtils.stripToNull(m.description()),
						m.thumbnailSeconds(),
						m.embedUrl()
						)
				.update(keyHolder);

		if (affectedRows > 0 && keyHolder.getKey() != null) {
			return keyHolder.getKey().intValue();
		}

		throw new IllegalStateException("Failed to insert media metadata");
	}

	@Transactional
	public void saveMediaContext(int mediaId, Association associations, Media m, boolean isUpdate, List<Integer> resolvedTaggedUserIds) {
		if (isUpdate) {
			List.of("media_area", "media_sector", "media_problem", "media_trail", "media_guestbook", "media_user")
			.forEach(table -> jdbcTemplate.update("DELETE FROM " + table + " WHERE media_id=?", mediaId));
		}

		switch (associations) {
		case AREAS -> jdbcTemplate.batchUpdate("INSERT INTO media_area (media_id, area_id, trivia) VALUES (?, ?, ?)",
				m.areas(), 50, (ps, a) -> { ps.setInt(1, mediaId); ps.setInt(2, a.areaId()); ps.setBoolean(3, a.trivia()); });
		case SECTORS -> jdbcTemplate.batchUpdate("INSERT INTO media_sector (media_id, sector_id, trivia) VALUES (?, ?, ?)",
				m.sectors(), 50, (ps, s) -> { ps.setInt(1, mediaId); ps.setInt(2, s.sectorId()); ps.setBoolean(3, s.trivia()); });
		case PROBLEMS -> jdbcTemplate.batchUpdate("INSERT INTO media_problem (media_id, problem_id, pitch, trivia, milliseconds) VALUES (?, ?, ?, ?, ?)",
				m.problems(), 50, (ps, p) -> { ps.setInt(1, mediaId); ps.setInt(2, p.problemId()); ps.setInt(3, p.problemPitch()); ps.setBoolean(4, p.trivia()); ps.setLong(5, p.milliseconds()); });
		case TRAILS -> jdbcTemplate.batchUpdate("INSERT INTO media_trail (media_id, trail_id) VALUES (?, ?)",
				m.trails(), 50, (ps, t) -> { ps.setInt(1, mediaId); ps.setInt(2, t.trailId()); });
		case GUESTBOOK -> jdbcTemplate.update("INSERT INTO media_guestbook (media_id, guestbook_id) VALUES (?, ?)", mediaId, m.guestbookId());
		case USER_AVATAR -> jdbcTemplate.update("UPDATE user SET media_id=? WHERE id=?", mediaId, m.userAvatarId());
		}

		if (resolvedTaggedUserIds != null && !resolvedTaggedUserIds.isEmpty()) {
			jdbcTemplate.batchUpdate("INSERT INTO media_user (media_id, user_id) VALUES (?, ?)",
					resolvedTaggedUserIds, 50, (ps, userId) -> {
						ps.setInt(1, mediaId);
						ps.setInt(2, userId);
					});
		}
	}
}