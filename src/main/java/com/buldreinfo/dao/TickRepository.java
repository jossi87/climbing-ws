package com.buldreinfo.dao;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.helpers.GradeConverter;
import com.buldreinfo.model.PublicAscent;
import com.buldreinfo.model.Tick;
import com.buldreinfo.model.Tick.TickRepeat;
import com.buldreinfo.model.Ticks;
import com.buldreinfo.util.StringUtils;

@Repository
public class TickRepository {
	private final JdbcClient jdbcClient;

	public TickRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Transactional(readOnly = true)
	public Ticks getTicks(Optional<Integer> authUserId, Setup setup, int page) {
		final var take = 200;
		var skip = (page - 1) * take;

		var sqlStr = """
				WITH req AS (
				    SELECT ? region_id, ? auth_user_id
				)
				SELECT a.id area_id, a.name area_name, a.locked_admin area_locked_admin, a.locked_superadmin area_locked_superadmin,
				       s.id sector_id, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin,
				       p.id problem_id, g.grade problem_grade, p.name problem_name, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin,
				       DATE_FORMAT(t.date,'%Y.%m.%d') ts, TRIM(CONCAT(u.firstname, ' ', IFNULL(u.lastname,''))) name,
				       COUNT(*) OVER() as total_count
				FROM req
				JOIN region r ON 1=1
				JOIN region_type rt ON r.id=rt.region_id
				JOIN area a ON r.id=a.region_id
				JOIN sector s ON a.id=s.area_id
				JOIN problem p ON s.id=p.sector_id
				JOIN tick t ON p.id=t.problem_id
				LEFT JOIN grade g ON t.grade_id=g.id
				JOIN user u ON t.user_id=u.id
				LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=req.auth_user_id
				WHERE rt.type_id IN (SELECT type_id FROM region_type WHERE region_id=req.region_id)
				  AND (r.id=req.region_id OR ur.user_id IS NOT NULL)
				  AND a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				  AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0))
				  AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				GROUP BY a.id, s.id, p.id, t.id, u.id, t.date, p.name, u.firstname, u.lastname
				ORDER BY t.date DESC, p.name, name
				LIMIT ? OFFSET ?
				""";

		var totalCount = new int[]{0};
		var ticks = jdbcClient.sql(sqlStr)
				.params(setup.idRegion(), authUserId.orElse(0), take, skip)
				.query((rs, _) -> {
					if (totalCount[0] == 0) totalCount[0] = rs.getInt("total_count");

					String grade = rs.getString("problem_grade");
					return new PublicAscent(
							rs.getInt("area_id"), rs.getString("area_name"), rs.getBoolean("area_locked_admin"), rs.getBoolean("area_locked_superadmin"),
							rs.getInt("sector_id"), rs.getString("sector_name"), rs.getBoolean("sector_locked_admin"), rs.getBoolean("sector_locked_superadmin"),
							rs.getInt("problem_id"), grade == null ? GradeConverter.NO_PERSONAL_GRADE : grade,
									rs.getString("problem_name"), rs.getBoolean("problem_locked_admin"), rs.getBoolean("problem_locked_superadmin"),
									rs.getString("ts"), rs.getString("name")
							);
				}).list();

		var numPages = (int) Math.ceil((double) totalCount[0] / take);
		return new Ticks(ticks, page, numPages);
	}

	@Transactional
	public void setTick(Setup setup, Optional<Integer> authUserId, Tick t) {
		int userId = authUserId.orElseThrow(() -> new IllegalArgumentException("Not logged in"));

		jdbcClient.sql("DELETE FROM todo WHERE user_id=? AND problem_id=?")
		.params(userId, t.idProblem())
		.update();

		final var dt = (t.date() == null || t.date().isBlank()) ? null : LocalDate.parse(t.date(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));

		if (t.delete()) {
			if (t.id() <= 0) throw new IllegalArgumentException("Cannot delete tick without id");

			int affected = jdbcClient.sql("DELETE FROM tick WHERE id=? AND user_id=? AND problem_id=?")
					.params(t.id(), userId, t.idProblem())
					.update();
			if (affected != 1) throw new IllegalStateException("Invalid tick=" + t);
		} else if (t.id() == -1) {
			var keyHolder = new GeneratedKeyHolder();
			jdbcClient.sql("INSERT INTO tick (problem_id, user_id, date, grade_id, comment, stars) VALUES (?, ?, ?, ?, ?, ?)")
			.params(t.idProblem(), userId, dt, setup.gradeConverter().getIdGradeFromGrade(t.grade()), StringUtils.stripToNull(t.comment()), t.stars())
			.update(keyHolder, "id");

			upsertTickRepeats(keyHolder.getKey().intValue(), t.repeats());
		} else if (t.id() > 0) {
			int affected = jdbcClient.sql("UPDATE tick SET date=?, grade_id=?, comment=?, stars=? WHERE id=? AND problem_id=? AND user_id=?")
					.params(dt, setup.gradeConverter().getIdGradeFromGrade(t.grade()), StringUtils.stripToNull(t.comment()), t.stars(), t.id(), t.idProblem(), userId)
					.update();
			if (affected != 1) throw new IllegalStateException("Invalid tick=" + t);
			upsertTickRepeats(t.id(), t.repeats());
		}
	}

	private void upsertTickRepeats(int idTick, List<TickRepeat> repeats) {
		var idsToKeep = repeats == null ? List.<Integer>of() : repeats.stream().filter(x -> x.id() > 0).map(TickRepeat::id).toList();

		if (idsToKeep.isEmpty()) {
			jdbcClient.sql("DELETE FROM tick_repeat WHERE tick_id=?").param(idTick).update();
		} else {
			jdbcClient.sql("DELETE FROM tick_repeat WHERE tick_id=? AND id NOT IN (:ids)")
			.param("tick_id", idTick).param("ids", idsToKeep).update();
		}

		if (repeats != null) {
			for (var r : repeats) {
				final var dt = (r.date() == null || r.date().isBlank()) ? null : LocalDate.parse(r.date(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
				if (r.id() > 0) {
					jdbcClient.sql("UPDATE tick_repeat SET date=?, comment=? WHERE id=?")
					.params(dt, StringUtils.stripToNull(r.comment()), r.id())
					.update();
				} else {
					jdbcClient.sql("INSERT INTO tick_repeat (tick_id, date, comment) VALUES (?, ?, ?)")
					.params(idTick, dt, StringUtils.stripToNull(r.comment()))
					.update();
				}
			}
		}
	}
}