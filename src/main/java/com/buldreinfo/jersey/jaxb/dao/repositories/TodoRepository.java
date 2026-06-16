package com.buldreinfo.jersey.jaxb.dao.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.model.Todo;
import com.buldreinfo.jersey.jaxb.model.Todo.TodoProblem;
import com.buldreinfo.jersey.jaxb.model.Todo.TodoSector;
import com.buldreinfo.jersey.jaxb.model.User;
import com.google.common.base.Preconditions;

public record TodoRepository() {
	private static Logger logger = LogManager.getLogger();
	
	public Todo getTodo(Connection c, Optional<Integer> authUserId, Setup setup, int idArea, int idSector) throws SQLException {
		Todo res = new Todo(new ArrayList<>());
		Map<Integer, TodoSector> sectorLookup = new HashMap<>();
		Map<Integer, TodoProblem> problemLookup = new HashMap<>();
		String condition = null;
		int id = 0;
		if (idSector > 0) {
			condition = "s.id=?";
			id = idSector;
		}
		else if (idArea > 0) {
			condition = "a.id=?";
			id = idArea;
		}
		else {
			throw new RuntimeException("Invalid arguments");
		}
		String sqlStr = """
				SELECT s.id sector_id, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, t.id todo_id, p.id problem_id, p.nr problem_nr, p.name problem_name, g.grade problem_grade, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin,
				       u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) user_name
				FROM region r
				JOIN area a ON r.id=a.region_id
				JOIN sector s ON a.id=s.area_id
				JOIN problem p ON s.id=p.sector_id
				JOIN grade g ON p.grade_id=g.id
				JOIN todo t ON p.id=t.problem_id
				JOIN user u ON t.user_id=u.id
				LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?
				WHERE %s
				AND a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0))
				AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				ORDER BY a.name, s.sorting, s.name, p.nr, u.firstname, u.lastname
				""".formatted(condition);
		try (PreparedStatement ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, id);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					// Sector
					int sectorId = rst.getInt("sector_id");
					TodoSector s = sectorLookup.get(sectorId);
					if (s == null) {
						String sectorName = rst.getString("sector_name");
						boolean sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
						boolean sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
						s = new TodoSector(sectorId, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, new ArrayList<>());
						res.sectors().add(s);
						sectorLookup.put(sectorId, s);
					}
					// Problem
					int problemId = rst.getInt("problem_id");
					TodoProblem p = problemLookup.get(problemId);
					if (p == null) {
						int problemNr = rst.getInt("problem_nr");
						String problemName = rst.getString("problem_name");
						String problemGrade = rst.getString("problem_grade");
						boolean problemLockedAdmin = rst.getBoolean("problem_locked_admin");
						boolean problemLockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
						p = new TodoProblem(problemId, problemLockedAdmin, problemLockedSuperadmin, problemNr, problemName, problemGrade, new ArrayList<>());
						s.problems().add(p);
						problemLookup.put(problemId, p);
					}
					// Partner
					int userId = rst.getInt("user_id");
					String userName = rst.getString("user_name");
					p.partners().add(User.from(userId, userName));
				}
			}
		}
		logger.debug("getTodo(authUserId={}, idArea={}, idSector)={}) - res={}", authUserId, setup.idRegion(), idArea, idSector, res);
		return res;
	}
	
	public void toggleTodo(Connection c, Optional<Integer> authUserId, int problemId) throws SQLException {
		Preconditions.checkArgument(authUserId.isPresent(), "User not logged in");
		Preconditions.checkArgument(problemId > 0, "Problem id not set");
		int todoId = -1;
		try (PreparedStatement ps = c.prepareStatement("SELECT id FROM todo WHERE user_id=? AND problem_id=?")) {
			ps.setInt(1, authUserId.orElseThrow());
			ps.setInt(2, problemId);
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					todoId = rst.getInt("id");
				}
			}
		}
		if (todoId > 0) {
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM todo WHERE id=?")) {
				ps.setInt(1, todoId);
				ps.execute();
			}
		} else {
			try (PreparedStatement ps = c.prepareStatement("INSERT INTO todo (user_id, problem_id, created) VALUES (?, ?, now())")) {
				ps.setInt(1, authUserId.orElseThrow());
				ps.setInt(2, problemId);
				ps.execute();
			}
		}
	}
}