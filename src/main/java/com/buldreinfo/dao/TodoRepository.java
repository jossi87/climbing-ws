package com.buldreinfo.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.model.Todo;
import com.buldreinfo.model.Todo.TodoProblem;
import com.buldreinfo.model.Todo.TodoSector;
import com.buldreinfo.model.User;
import com.google.common.base.Preconditions;

@Repository
public class TodoRepository extends BaseRepository {
	private static final Logger logger = LogManager.getLogger();
	
	public TodoRepository(ClimbingTransactionManager txManager) {
		super(txManager);
	}
	
	public Todo getTodo(Optional<Integer> authUserId, Setup setup, int idArea, int idSector) throws SQLException {
		var res = new Todo(new ArrayList<>());
		var sectorLookup = new HashMap<Integer, TodoSector>();
		var problemLookup = new HashMap<Integer, TodoProblem>();
		var c = txManager.getConnection();
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
		var sqlStr = """
				SELECT s.id sector_id, s.name sector_name, s.locked_admin sector_locked_admin, s.locked_superadmin sector_locked_superadmin, t.id todo_id, p.id problem_id, p.nr problem_nr, p.name problem_name, g.grade problem_grade, p.locked_admin problem_locked_admin, p.locked_superadmin problem_locked_superadmin,
				       u.id user_id, TRIM(CONCAT(u.firstname, ' ', COALESCE(u.lastname,''))) user_name
				FROM region r
				JOIN area a ON r.id=a.region_id
				JOIN sector s ON a.id=s.area_id
				JOIN problem p ON s.id=p.sector_id
				JOIN grade g ON p.consensus_grade_id=g.id
				JOIN todo t ON p.id=t.problem_id
				JOIN user u ON t.user_id=u.id
				LEFT JOIN user_region ur ON r.id=ur.region_id AND ur.user_id=?
				WHERE %s
				AND a.trash IS NULL AND ((a.locked_admin=0 AND a.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND a.locked_superadmin=0))
				AND s.trash IS NULL AND ((s.locked_admin=0 AND s.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND s.locked_superadmin=0))
				AND p.trash IS NULL AND ((p.locked_admin=0 AND p.locked_superadmin=0) OR (ur.superadmin_read=1) OR (ur.admin_read=1 AND p.locked_superadmin=0))
				ORDER BY a.name, s.sorting, s.name, p.nr, u.firstname, u.lastname
				""".formatted(condition);
		try (var ps = c.prepareStatement(sqlStr)) {
			ps.setInt(1, authUserId.orElse(0));
			ps.setInt(2, id);
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					int sectorId = rst.getInt("sector_id");
					var s = sectorLookup.get(sectorId);
					if (s == null) {
						var sectorName = rst.getString("sector_name");
						var sectorLockedAdmin = rst.getBoolean("sector_locked_admin");
						var sectorLockedSuperadmin = rst.getBoolean("sector_locked_superadmin");
						s = new TodoSector(sectorId, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, new ArrayList<>());
						res.sectors().add(s);
						sectorLookup.put(sectorId, s);
					}
					int problemId = rst.getInt("problem_id");
					var p = problemLookup.get(problemId);
					if (p == null) {
						int problemNr = rst.getInt("problem_nr");
						var problemName = rst.getString("problem_name");
						var problemGrade = rst.getString("problem_grade");
						var problemLockedAdmin = rst.getBoolean("problem_locked_admin");
						var problemLockedSuperadmin = rst.getBoolean("problem_locked_superadmin");
						p = new TodoProblem(problemId, problemLockedAdmin, problemLockedSuperadmin, problemNr, problemName, problemGrade, new ArrayList<>());
						s.problems().add(p);
						problemLookup.put(problemId, p);
					}
					int userId = rst.getInt("user_id");
					var userName = rst.getString("user_name");
					p.partners().add(User.from(userId, userName));
				}
			}
		}
		logger.debug("getTodo(authUserId={}, idArea={}, idSector)={}) - res={}", authUserId, setup.idRegion(), idArea, idSector, res);
		return res;
	}
	
	public void toggleTodo(Optional<Integer> authUserId, int problemId) throws SQLException {
		Preconditions.checkArgument(authUserId.isPresent(), "User not logged in");
		Preconditions.checkArgument(problemId > 0, "Problem id not set");
		var c = txManager.getConnection();
		int todoId = -1;
		try (var ps = c.prepareStatement("SELECT id FROM todo WHERE user_id=? AND problem_id=?")) {
			ps.setInt(1, authUserId.orElseThrow());
			ps.setInt(2, problemId);
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					todoId = rst.getInt("id");
				}
			}
		}
		if (todoId > 0) {
			try (var ps = c.prepareStatement("DELETE FROM todo WHERE id=?")) {
				ps.setInt(1, todoId);
				ps.execute();
			}
		} else {
			try (var ps = c.prepareStatement("INSERT INTO todo (user_id, problem_id, created) VALUES (?, ?, now())")) {
				ps.setInt(1, authUserId.orElseThrow());
				ps.setInt(2, problemId);
				ps.execute();
			}
		}
	}
}