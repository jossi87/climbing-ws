package com.buldreinfo.jersey.jaxb.dao.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import com.buldreinfo.jersey.jaxb.model.PublicAscent;
import com.buldreinfo.jersey.jaxb.model.Tick;
import com.buldreinfo.jersey.jaxb.model.Tick.TickRepeat;
import com.buldreinfo.jersey.jaxb.model.Ticks;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;

public record TickRepository(Dao dao) {
	private static Logger logger = LogManager.getLogger();
	
	public Ticks getTicks(Connection c, Optional<Integer> authUserId, Setup setup, int page) throws SQLException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		final int take = 200;
		int skip = (page - 1) * take;
		String sqlStr = """
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
				  AND is_readable(ur.admin_read, ur.superadmin_read, a.locked_admin, a.locked_superadmin, a.trash)=1
				  AND is_readable(ur.admin_read, ur.superadmin_read, s.locked_admin, s.locked_superadmin, s.trash)=1
				  AND is_readable(ur.admin_read, ur.superadmin_read, p.locked_admin, p.locked_superadmin, p.trash)=1
				GROUP BY a.id, s.id, p.id, t.id, u.id, t.date, p.name, u.firstname, u.lastname
				ORDER BY t.date DESC, p.name, name
				LIMIT ? OFFSET ?
				""";
		List<PublicAscent> ticks = new ArrayList<>();
		int totalCount = 0;
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, setup.idRegion());
			ps.setInt(2, authUserId.orElse(0));
			ps.setInt(3, take);
			ps.setInt(4, skip);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					if (totalCount == 0) {
						totalCount = rst.getInt("total_count");
					}
					int areaId = rst.getInt("area_id");
					String areaName = rst.getString("area_name");
					boolean areaLockedAdmin = rst.getBoolean("area_locked_admin");
					boolean areaLockedSuperadmin = rst.getBoolean("area_locked_superadmin");
					int sectorId = rst.getInt("sector_id");
					String sectorName = rst.getString("sector_name");
					boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
					boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
					int problemId = rst.getInt("problem_id");
					String problemGrade = rst.getString("problem_grade");
					if (problemGrade == null) {
						problemGrade = GradeConverter.NO_PERSONAL_GRADE;
					}
					String problemName = rst.getString("problem_name");
					boolean problemLockedAdmin = rst.getBoolean("problem_locked_admin");
					boolean problemLockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
					String date = rst.getString("ts");
					String name = rst.getString("name");
					ticks.add(new PublicAscent(
							areaId, areaName, areaLockedAdmin, areaLockedSuperadmin,
							sectorId, sectorName, sectorLockedAdmin, sectorLockedSuperadmin,
							problemId, problemGrade,
							problemName, problemLockedAdmin, problemLockedSuperadmin, date, name
							));
				}
			}
		}
		int numPages = (int) Math.ceil((double) totalCount / take);
		logger.debug("getTicks(page={}) - totalCount={}, duration={}", page, totalCount, stopwatch);
		return new Ticks(ticks, page, numPages);
	}
	
	public void setTick(Connection c, Optional<Integer> authUserId, Setup setup, Tick t) throws SQLException {
		Preconditions.checkArgument(authUserId.isPresent(), "Not logged in");
		// Remove from project list (if existing)
		try (PreparedStatement ps = c.prepareStatement("DELETE FROM todo WHERE user_id=? AND problem_id=?")) {
			ps.setInt(1, authUserId.orElseThrow());
			ps.setInt(2, t.idProblem());
			ps.execute();
		}
		final LocalDate dt = Strings.isNullOrEmpty(t.date())? null : LocalDate.parse(t.date(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		logger.debug("setTick(authUserId={}, dt={}, t={}", authUserId, dt, t);
		if (t.delete()) {
			Preconditions.checkArgument(t.id() > 0, "Cannot delete a tick without id");
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM tick WHERE id=? AND user_id=? AND problem_id=?")) {
				ps.setInt(1, t.id());
				ps.setInt(2, authUserId.orElseThrow());
				ps.setInt(3, t.idProblem());
				int res = ps.executeUpdate();
				if (res != 1) {
					throw new SQLException("Invalid tick=" + t + ", authUserId=" + authUserId);
				}
			}
		}
		else if (t.id() == -1) {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO tick (problem_id, user_id, date, grade_id, comment, stars) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
				ps.setInt(1, t.idProblem());
				ps.setInt(2, authUserId.orElseThrow());
				ps.setObject(3, dt);
				JdbcUtils.setNullablePositiveInteger(ps, 4, setup.gradeConverter().getIdGradeFromGrade(t.grade()));
				ps.setString(5, GlobalFunctions.stripString(t.comment()));
				ps.setDouble(6, t.stars());
				ps.executeUpdate();
				try (ResultSet rst = ps.getGeneratedKeys()) {
					if (rst != null && rst.next()) {
						int idTick = rst.getInt(1);
						upsertTickRepeats(c, idTick, t.repeats());
					}
				}
			}
		}
		else if (t.id() > 0) {
			try (PreparedStatement ps = c.prepareStatement("UPDATE tick SET date=?, grade_id=?, comment=?, stars=? WHERE id=? AND problem_id=? AND user_id=?")) {
				ps.setObject(1, dt);
				JdbcUtils.setNullablePositiveInteger(ps, 2, setup.gradeConverter().getIdGradeFromGrade(t.grade()));
				ps.setString(3, GlobalFunctions.stripString(t.comment()));
				ps.setDouble(4, t.stars());
				ps.setInt(5, t.id());
				ps.setInt(6, t.idProblem());
				ps.setInt(7, authUserId.orElseThrow());
				int res = ps.executeUpdate();
				if (res != 1) {
					throw new SQLException("Invalid tick=" + t + ", authUserId=" + authUserId);
				}
				upsertTickRepeats(c, t.id(), t.repeats());
			}
		} else {
			throw new SQLException("Invalid tick=" + t + ", authUserId=" + authUserId);
		}
		dao.getActivityRepo().fillActivity(c, t.idProblem());
		dao.getProblemRepo().updateProblemConsensusGrade(c, t.idProblem());
	}
	
	private void upsertTickRepeats(Connection c, int idTick, List<TickRepeat> repeats) throws SQLException {
		// Deleted removed ascents
		List<Integer> idsToKeep = repeats == null ? List.of() : repeats.stream().filter(x -> x.id() > 0).map(TickRepeat::id).toList();
		if (idsToKeep.isEmpty()) {
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM tick_repeat WHERE tick_id=?")) {
				ps.setInt(1, idTick);
				ps.execute();
			}
		} else {
			String placeholders = String.join(",", Collections.nCopies(idsToKeep.size(), "?"));
			String sqlStr = "DELETE FROM tick_repeat WHERE tick_id=? AND id NOT IN (" + placeholders + ")";
			try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
				ps.setInt(1, idTick);
				for (int i = 0; i < idsToKeep.size(); i++) {
					ps.setInt(i + 2, idsToKeep.get(i));
				}
				ps.execute();
			}
		}
		// Upsert repeats
		if (repeats != null && !repeats.isEmpty()) {
			for (TickRepeat r : repeats) {
				final LocalDate dt = Strings.isNullOrEmpty(r.date())? null : LocalDate.parse(r.date(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
				if (r.id() > 0) {
					try (PreparedStatement ps = c.prepareStatement("UPDATE tick_repeat SET date=?, comment=? WHERE id=?")) {
						ps.setObject(1, dt);
						ps.setString(2, GlobalFunctions.stripString(r.comment()));
						ps.setInt(3, r.id());
						int res = ps.executeUpdate();
						if (res != 1) {
							throw new SQLException("Invalid repeat=" + r);
						}
					}
				}
				else {
					try (PreparedStatement ps = c.prepareStatement("INSERT INTO tick_repeat (tick_id, date, comment) VALUES (?, ?, ?)")) {
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