package com.buldreinfo.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.model.Todo;
import com.buldreinfo.model.Todo.TodoProblem;
import com.buldreinfo.model.Todo.TodoSector;
import com.buldreinfo.model.User;

@Repository
public class TodoRepository {
	private final JdbcClient jdbcClient;

	public TodoRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Transactional(readOnly = true)
	public Todo getTodo(Optional<Integer> authUserId, int idArea, int idSector) {
		String condition;
		int id;

		if (idSector > 0) {
			condition = "s.id=?";
			id = idSector;
		} else if (idArea > 0) {
			condition = "a.id=?";
			id = idArea;
		} else {
			throw new IllegalArgumentException("Invalid arguments");
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

		var result = new Todo(new ArrayList<>());
		var sectorLookup = new HashMap<Integer, TodoSector>();
		var problemLookup = new HashMap<Integer, TodoProblem>();

		jdbcClient.sql(sqlStr)
		.params(authUserId.orElse(0), id)
		.query(rs -> {
			int sectorId = rs.getInt("sector_id");
			var s = sectorLookup.get(sectorId);
			if (s == null) {
				var sectorName = rs.getString("sector_name");
				var sectorLockedAdmin = rs.getBoolean("sector_locked_admin");
				var sectorLockedSuperadmin = rs.getBoolean("sector_locked_superadmin");
				s = new TodoSector(sectorId, sectorName, sectorLockedAdmin, sectorLockedSuperadmin, new ArrayList<>());
				result.sectors().add(s);
				sectorLookup.put(sectorId, s);
			}

			int problemId = rs.getInt("problem_id");
			var p = problemLookup.get(problemId);
			if (p == null) {
				int problemNr = rs.getInt("problem_nr");
				var problemName = rs.getString("problem_name");
				var problemGrade = rs.getString("problem_grade");
				var problemLockedAdmin = rs.getBoolean("problem_locked_admin");
				var problemLockedSuperadmin = rs.getBoolean("problem_locked_superadmin");
				p = new TodoProblem(problemId, problemLockedAdmin, problemLockedSuperadmin, problemNr, problemName, problemGrade, new ArrayList<>());
				s.problems().add(p);
				problemLookup.put(problemId, p);
			}

			int userId = rs.getInt("user_id");
			var userName = rs.getString("user_name");
			p.partners().add(User.from(userId, userName));
		});

		return result;
	}

	@Transactional
	public void toggleTodo(Optional<Integer> authUserId, int problemId) {
		if (authUserId.isEmpty()) throw new IllegalArgumentException("User not logged in");
		if (problemId <= 0) throw new IllegalArgumentException("Problem id not set");

		var todoId = jdbcClient.sql("SELECT id FROM todo WHERE user_id=? AND problem_id=?")
				.params(authUserId.get(), problemId)
				.query(Integer.class)
				.optional();

		if (todoId.isPresent()) {
			jdbcClient.sql("DELETE FROM todo WHERE id=?")
			.param(todoId.get())
			.update();
		} else {
			jdbcClient.sql("INSERT INTO todo (user_id, problem_id, created) VALUES (?, ?, now())")
			.params(authUserId.get(), problemId)
			.update();
		}
	}
}