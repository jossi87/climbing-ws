package com.buldreinfo.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.exception.ValidationFailedException;
import com.buldreinfo.helpers.GradeConverter;
import com.buldreinfo.infrastructure.CacheConstants;
import com.buldreinfo.model.Grade;
import com.buldreinfo.model.GradeSystem;

@Repository
public class GradeSystemRepository {
	private final JdbcClient jdbcClient;

	public GradeSystemRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Transactional(readOnly = true)
	public Optional<Integer> getAreaGradeSystem(int areaId) {
		if (areaId <= 0) return Optional.empty();
		return jdbcClient.sql("SELECT a.grade_system_id FROM area a WHERE a.id=?")
				.param(areaId)
				.query(Integer.class)
				.optional();
	}

	@Transactional(readOnly = true)
	public Optional<Integer> getSectorGradeSystem(int sectorId) {
		if (sectorId <= 0) return Optional.empty();
		return jdbcClient.sql("SELECT a.grade_system_id FROM sector s JOIN area a ON s.area_id=a.id WHERE s.id=?")
				.param(sectorId)
				.query(Integer.class)
				.optional();
	}

	@Transactional(readOnly = true)
	public Optional<Integer> getProblemGradeSystem(int problemId) {
		if (problemId <= 0) return Optional.empty();
		return jdbcClient.sql("SELECT a.grade_system_id FROM problem p JOIN sector s ON p.sector_id=s.id JOIN area a ON s.area_id=a.id WHERE p.id=?")
				.param(problemId)
				.query(Integer.class)
				.optional();
	}

	@Transactional(readOnly = true)
	public List<Integer> getRegionGradeSystemIds(int regionId) {
		return jdbcClient.sql("SELECT grade_system_id FROM region_grade_system WHERE region_id=? ORDER BY grade_system_id")
				.param(regionId)
				.query(Integer.class)
				.list();
	}

	@Transactional(readOnly = true)
	public List<Integer> getActivityGradeSystemIds(int regionId, int authUserId) {
		return jdbcClient.sql("""
				SELECT rgs.grade_system_id
				FROM region r
				JOIN region_type rt ON r.id = rt.region_id
				JOIN region_grade_system rgs ON rgs.region_id = r.id
				LEFT JOIN user_region ur ON (r.id = ur.region_id AND ur.user_id = ?)
				WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id = ?)
				  AND (r.id = ? OR ur.user_id IS NOT NULL)
				GROUP BY rgs.grade_system_id
				ORDER BY MAX(CASE WHEN r.id = ? THEN 1 ELSE 0 END) DESC, rgs.grade_system_id
				""")
				.params(authUserId, regionId, regionId, regionId)
				.query(Integer.class)
				.list();
	}

	@Transactional(readOnly = true)
	public int resolveAreaGradeSystemId(int regionId, int requestedGradeSystemId) {
		var declared = getRegionGradeSystemIds(regionId);
		if (requestedGradeSystemId > 0) {
			if (!declared.contains(requestedGradeSystemId)) {
				throw new ValidationFailedException("Grade system id=" + requestedGradeSystemId + " is not declared for region id=" + regionId);
			}
			return requestedGradeSystemId;
		}
		if (declared.size() == 1) {
			return declared.get(0);
		}
		throw new ValidationFailedException("Region id=" + regionId + " declares " + declared.size()
				+ " grade systems, so every area must pick one.");
	}

	@Cacheable(value = CacheConstants.REGION_CACHE_NAME, key = "'grade-systems:' + #idLanguage", sync = true)
	@Transactional(readOnly = true)
	public List<GradeSystem> getGradeSystems(int idLanguage) {
		Map<Integer, List<Grade>> gradesBySystem = new HashMap<>();
		jdbcClient.sql("""
				SELECT g.grade_system_id, g.id, g.grade, g.label_major, g.label_compact, g.weight, c.hex_code color
				FROM grade g
				JOIN grade_color c ON g.grade_color_id=c.id
				ORDER BY g.grade_system_id, g.weight
				""")
				.query(rs -> {
					gradesBySystem.computeIfAbsent(rs.getInt("grade_system_id"), _ -> new ArrayList<>())
							.add(new Grade(rs.getInt("id"), rs.getString("grade"), rs.getString("label_major"), rs.getString("label_compact"), rs.getInt("weight"), rs.getString("color")));
				});

		Map<Integer, GradeSystem> systems = new LinkedHashMap<>();
		jdbcClient.sql("""
				SELECT gs.id, COALESCE(gst.name, gs.name) name
				FROM grade_system gs
				LEFT JOIN grade_system_translation gst ON gst.grade_system_id = gs.id AND gst.language_id = ?
				ORDER BY gs.id
				""")
				.param(idLanguage)
				.query(rs -> {
					systems.put(rs.getInt("id"), new GradeSystem(rs.getInt("id"), rs.getString("name"), gradesBySystem.getOrDefault(rs.getInt("id"), List.of())));
				});
		return new ArrayList<>(systems.values());
	}

	/** The grade systems each region declares, in a stable order. */
	@Transactional(readOnly = true)
	public Map<Integer, List<Integer>> getRegionGradeSystemIdsByRegion() {
		Map<Integer, List<Integer>> res = new LinkedHashMap<>();
		jdbcClient.sql("SELECT region_id, grade_system_id FROM region_grade_system ORDER BY region_id, grade_system_id")
				.query(rs -> {
					res.computeIfAbsent(rs.getInt("region_id"), _ -> new ArrayList<>())
							.add(rs.getInt("grade_system_id"));
				});
		return res;
	}

	@Transactional(readOnly = true)
	public List<Grade> getGrades(int gradeSystemId) {
		return jdbcClient.sql("""
				SELECT g.id, g.grade, g.label_major, g.label_compact, g.weight, c.hex_code color
				FROM grade g
				JOIN grade_color c ON g.grade_color_id=c.id
				WHERE g.grade_system_id=? ORDER BY g.weight
				""")
				.param(gradeSystemId)
				.query((rs, _) -> new Grade(rs.getInt("id"), rs.getString("grade"), rs.getString("label_major"), rs.getString("label_compact"), rs.getInt("weight"), rs.getString("color")))
				.list();
	}

	public GradeConverter getConverterForSector(int sectorId) {
		return getConverter(getSectorGradeSystem(sectorId)
				.orElseThrow(() -> new ValidationFailedException("Unknown sector id=" + sectorId)));
	}

	public GradeConverter getConverterForProblem(int problemId) {
		return getConverter(getProblemGradeSystem(problemId)
				.orElseThrow(() -> new ValidationFailedException("Unknown problem id=" + problemId)));
	}

	private GradeConverter getConverter(int gradeSystemId) {
		var grades = getGrades(gradeSystemId);
		if (grades.isEmpty()) {
			throw new ValidationFailedException("No grades found for grade system id=" + gradeSystemId);
		}
		return new GradeConverter(grades);
	}
}
