package com.buldreinfo.jersey.jaxb.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.jersey.jaxb.model.SisProblem;
import com.buldreinfo.jersey.jaxb.model.SisProblem.Shape;
import com.buldreinfo.jersey.jaxb.model.SisTick;
import com.buldreinfo.jersey.jaxb.model.SisUser;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import jersey.repackaged.com.google.common.base.Preconditions;

/**
 * @author <a href="mailto:jostein.oygarden@gmail.com">Jostein Oeygarden</a>
 */
public class SisRepository {
	private final DbConnection c;
	private final Gson gson = new Gson();	

	protected SisRepository(DbConnection c) {
		this.c = c;
	}

	public List<SisProblem> getProblems(int optionalId) throws SQLException {
		List<SisProblem> res = new ArrayList<>();
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT p.id, p.image, p.grade, DATE_FORMAT(p.created,'%Y.%m.%d') created, p.type, p.creator, p.shapes FROM sis_problem p WHERE p.deleted IS NULL GROUP BY p.id, p.grade, p.created, p.type, p.creator, p.shapes ORDER BY p.id DESC");
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			int id = rst.getInt("id");
			if (optionalId == 0 || id == optionalId) {
				String image = rst.getString("image");
				String grade = rst.getString("grade");
				String created = rst.getString("created");
				String type = rst.getString("type");
				String creator = rst.getString("creator");
				List<Shape> shapes = gson.fromJson(rst.getString("shapes"), new TypeToken<ArrayList<Shape>>(){}.getType());
				boolean deleted = false;
				res.add(new SisProblem(id, image, grade, created, type, creator, shapes, deleted, getTicks(id)));
			}
		}
		rst.close();
		ps.close();
		return res;
	}

	public SisUser getUser(String facebookUserId, String email, String name) throws SQLException {
		SisUser res = null;
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT id, name, email, is_admin, facebook_user_id FROM sis_user WHERE (? IS NOT NULL AND email=?) OR facebook_user_id=?");
		ps.setString(1, email);
		ps.setString(2, email);
		ps.setString(3, facebookUserId);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			int id = rst.getInt("id");
			name = rst.getString("name");
			email = rst.getString("email");
			boolean isAdmin = rst.getBoolean("is_admin");
			if (Strings.isNullOrEmpty(rst.getString("facebook_user_id"))) {
				PreparedStatement ps2 = c.getConnection().prepareStatement("UPDATE sis_user SET facebook_user_id=? WHERE id=?");
				ps2.setString(1, facebookUserId);
				ps2.setInt(2, id);
				ps2.execute();
				ps2.close();
			}
			res = new SisUser(id, name, email, isAdmin);
		}
		rst.close();
		ps.close();
		if (res == null) {
			ps = c.getConnection().prepareStatement("INSERT INTO sis_user (name, email, facebook_user_id) VALUES (?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
			ps.setString(1, name);
			ps.setString(2, email);
			ps.setString(3, facebookUserId);
			ps.executeUpdate();
			rst = ps.getGeneratedKeys();
			if (rst != null && rst.next()) {
				int id = rst.getInt(1);
				res = new SisUser(id, name, email, false);
			}
			rst.close();
			ps.close();
		}
		return Preconditions.checkNotNull(res);
	}

	public SisProblem setProblem(SisProblem p) throws SQLException {
		int id = p.getId();
		if (p.getId() > 0) {
			PreparedStatement ps = c.getConnection().prepareStatement("UPDATE sis_problem SET image=?, grade=?, type=?, creator=?, shapes=?, deleted=? WHERE id=?");
			ps.setString(1, p.getImage());
			ps.setString(2, p.getGrade());
			ps.setString(3, p.getType());
			ps.setString(4, p.getCreator());
			ps.setString(5, gson.toJson(p.getShapes()));
			ps.setTimestamp(6, p.isDeleted()? new Timestamp(System.currentTimeMillis()) : null);
			ps.setInt(7, p.getId());
			ps.execute();
			ps.close();
		}
		else {
			PreparedStatement ps = c.getConnection().prepareStatement("INSERT INTO sis_problem (created, image, grade, type, creator, shapes) VALUES (now(), ?, ?, ?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
			ps.setString(1, p.getImage());
			ps.setString(2, p.getGrade());
			ps.setString(3, p.getType());
			ps.setString(4, p.getCreator());
			ps.setString(5, gson.toJson(p.getShapes()));
			ps.executeUpdate();
			ResultSet rst = ps.getGeneratedKeys();
			if (rst != null && rst.next()) {
				id = rst.getInt(1);
			}
			rst.close();
			ps.close();
		}
		if (p.isDeleted()) {
			return p; // JavaScript will look for deleted=true and remove this from state
		}
		Preconditions.checkArgument(id > 0);
		return getProblems(id).get(0);
	}

	public SisProblem setTick(SisTick t) throws SQLException {
		PreparedStatement ps = c.getConnection().prepareStatement("DELETE FROM sis_tick WHERE user_id=? AND problem_id=?");
		ps.setInt(1, t.getUserId());
		ps.setInt(2, t.getProblemId());
		ps.executeUpdate();
		ps.close();
		if (t.getStars() >= 0) {
			ps = c.getConnection().prepareStatement("INSERT INTO sis_tick (user_id, problem_id, stars) VALUES (?, ?, ?)");
			ps.setInt(1, t.getUserId());
			ps.setInt(2, t.getProblemId());
			ps.setInt(3, t.getStars());
			ps.execute();
			ps.close();
		}
		return getProblems(t.getProblemId()).get(0);
	}

	private List<SisTick> getTicks(int idProblem) throws SQLException {
		List<SisTick> res = new ArrayList<>();
		PreparedStatement ps = c.getConnection().prepareStatement("SELECT t.user_id, u.name, t.stars FROM sis_tick t, sis_user u WHERE t.user_id=u.id AND t.problem_id=? ORDER BY u.name");
		ps.setInt(1, idProblem);
		ResultSet rst = ps.executeQuery();
		while (rst.next()) {
			int userId = rst.getInt("user_id");
			String name = rst.getString("name");
			int stars = rst.getInt("stars");
			res.add(new SisTick(idProblem, userId, name, stars));
		}
		rst.close();
		ps.close();
		return res;
	}
}