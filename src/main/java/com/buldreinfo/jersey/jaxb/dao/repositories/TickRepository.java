package com.buldreinfo.jersey.jaxb.dao.repositories;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.dao.Dao;
import com.buldreinfo.jersey.jaxb.dao.JdbcUtils;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.helpers.GradeConverter;
import com.buldreinfo.jersey.jaxb.infrastructure.DatabaseContext;
import com.buldreinfo.jersey.jaxb.model.PublicAscent;
import com.buldreinfo.jersey.jaxb.model.Tick;
import com.buldreinfo.jersey.jaxb.model.Tick.TickRepeat;
import com.buldreinfo.jersey.jaxb.model.Ticks;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;

public record TickRepository(Dao dao) {
	private static final Logger logger = LogManager.getLogger();
	
	public Ticks getTicks(Optional<Integer> authUserId, Setup setup, int page) throws SQLException {
		var stopwatch = Stopwatch.createStarted();
		final var take = 200;
		var skip = (page - 1) * take;
		var c = DatabaseContext.getConnection();
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
		var ticks = new ArrayList<PublicAscent>();
		var totalCount = 0;
		try (var ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, authUserId.orElse(0));
			ps.setInt(3, take);
			ps.setInt(4, skip);
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					if (totalCount == 0) {
						totalCount = rst.getInt("total_count");
					}
					var areaId = rst.getInt("area_id");
					var areaName = rst.getString("area_name");
					var areaLockedAdmin = rst.getBoolean("area_locked_admin");
					var areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					var sectorId = rst.getInt("sector_id");
					var sectorName = rst.getString("sector_name");
					var sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
					var sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
					var problemId = rst.getInt("problem_id");
					var problemGrade = rst.getString("problem_grade");
					if (problemGrade == null) {
						problemGrade = GradeConverter.NO_PERSONAL_GRADE;
					}
					var problemName = rst.getString("problem_name");
					var problemLockedAdmin = rst.getBoolean("problem_locked_admin");
					var problemLockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
					var date = rst.getString("ts");
					var name = rst.getString("name");
					ticks.add(new PublicAscent(
							areaId, areaName, areaLockedAdmin, areaLockedSuperadmin,
							sectorId, sectorName, sectorLockedAdmin, sectorLockedSuperadmin,
							problemId, problemGrade,
							problemName, problemLockedAdmin, problemLockedSuperadmin, date, name
							));
				}
			}
		}
		var numPages = (int) Math.ceil((double) totalCount / take);
		logger.debug("getTicks(page={}) - totalCount={}, duration={}", page, totalCount, stopwatch);
		return new Ticks(ticks, page, numPages);
	}
	
	public void setTick(Setup setup, Optional<Integer> authUserId, Tick t) throws SQLException {
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
		var c = DatabaseContext.getConnection();
		try (var ps = c.prepareStatement("DELETE FROM todo WHERE user_id=? AND problem_id=?")) {
			ps.setInt(1, authUserId.orElseThrow());
			ps.setInt(2, t.idProblem());
			ps.execute();
		}
		final var dt = Strings.isNullOrEmpty(t.date()) ? null : LocalDate.parse(t.date(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		logger.debug("setTick(authUserId={}, dt={}, t={}", authUserId, dt, t);
		if (t.delete()) {
			Preconditions.checkArgument(t.id() > 0, "Cannot delete a tick without id");
			try (var ps = c.prepareStatement("DELETE FROM tick WHERE id=? AND user_id=? AND problem_id=?")) {
				ps.setInt(1, t.id());
				ps.setInt(2, authUserId.orElseThrow());
				ps.setInt(3, t.idProblem());
				var res = ps.executeUpdate();
				if (res != 1) {
					throw new SQLException("Invalid tick=" + t + ", authUserId=" + authUserId);
				}
			}
		}
		else if (t.id() == -1) {
			try (var ps = c.prepareStatement("INSERT INTO tick (problem_id, user_id, date, grade_id, comment, stars) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, t.idProblem());
				ps.setInt(2, authUserId.orElseThrow());
				ps.setObject(3, dt);
				JdbcUtils.setNullablePositiveInteger(ps, 4, setup.gradeConverter().getIdGradeFromGrade(t.grade()));
				ps.setString(5, GlobalFunctions.stripString(t.comment()));
				ps.setDouble(6, t.stars());
				ps.executeUpdate();
				try (var rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						var idTick = rst.getInt(1);
						upsertTickRepeats(idTick, t.repeats());
					}
				}
			}
		}
		else if (t.id() > 0) {
			try (var ps = c.prepareStatement("UPDATE tick SET date=?, grade_id=?, comment=?, stars=? WHERE id=? AND problem_id=? AND user_id=?")) {
				ps.setObject(1, dt);
				JdbcUtils.setNullablePositiveInteger(ps, 2, setup.gradeConverter().getIdGradeFromGrade(t.grade()));
				ps.setString(3, GlobalFunctions.stripString(t.comment()));
				ps.setDouble(4, t.stars());
				ps.setInt(5, t.id());
				ps.setInt(6, t.idProblem());
				ps.setInt(7, authUserId.orElseThrow());
				var res = ps.executeUpdate();
				if (res != 1) {
					throw new SQLException("Invalid tick=" + t + ", authUserId=" + authUserId);
				}
				upsertTickRepeats(t.id(), t.repeats());
			}
		} else {
			throw new SQLException("Invalid tick=" + t + ", authUserId=" + authUserId);
		}
		dao.getActivityRepo().fillActivity(t.idProblem());
		dao.getProblemRepo().updateProblemConsensusGrade(t.idProblem());
	}
	
	private void upsertTickRepeats(int idTick, List<TickRepeat> repeats) throws SQLException {
		var c = DatabaseContext.getConnection();
		var idsToKeep = repeats == null ? List.<Integer>of() : repeats.stream().filter(x -> x.id() > 0).map(TickRepeat::id).toList();
		if (idsToKeep.isEmpty()) {
			try (var ps = c.prepareStatement("DELETE FROM tick_repeat WHERE tick_id=?")) {
				ps.setInt(1, idTick);
				ps.execute();
			}
		} else {
			var placeholders = String.join(",", Collections.nCopies(idsToKeep.size(), "?"));
			var sqlStr = "DELETE FROM tick_repeat WHERE tick_id=? AND id NOT IN (" + placeholders + ")";
			try (var ps = c.prepareStatement(sqlStr)) {
				ps.setInt(1, idTick);
				for (var i = 0; i < idsToKeep.size(); i++) {
					ps.setInt(i + 2, idsToKeep.get(i));
				}
				ps.execute();
			}
		}
		if (repeats != null && !repeats.isEmpty()) {
			for (var r : repeats) {
				final var dt = Strings.isNullOrEmpty(r.date()) ? null : LocalDate.parse(r.date(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
				if (r.id() > 0) {
					try (var ps = c.prepareStatement("UPDATE tick_repeat SET date=?, comment=? WHERE id=?")) {
						ps.setObject(1, dt);
						ps.setString(2, GlobalFunctions.stripString(r.comment()));
						ps.setInt(3, r.id());
						var res = ps.executeUpdate();
						if (res != 1) {
							throw new SQLException("Invalid repeat=" + r);
						}
					}
				}
				else {
					try (var ps = c.prepareStatement("INSERT INTO tick_repeat (tick_id, date, comment) VALUES (?, ?, ?)")) {
						ps.setInt(1, idTick);
						ps.setObject(2, dt);
						ps.setString(3, GlobalFunctions.stripString(r.comment()));
						ps.execute();
					}
				}
			}
		}
	}
}