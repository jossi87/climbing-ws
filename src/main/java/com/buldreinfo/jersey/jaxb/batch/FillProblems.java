package com.buldreinfo.jersey.jaxb.batch;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.Server;
import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.db.Dao;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Area.AreaSector;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Redirect;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.Type;
import com.buldreinfo.jersey.jaxb.model.User;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class FillProblems {
	private static Logger logger = LogManager.getLogger();
	public static enum T {BOULDER, BOLT, TRAD, MIXED, TOPROPE, AID, AIDTRAD, ICE}
	private final boolean shouldUpdateHits = false;

	private final Map<String, Integer> areaCache = new HashMap<>();
	private final Map<Integer, Map<String, Integer>> sectorCache = new HashMap<>();
	private final Map<String, User> userCache = new HashMap<>();

	private class Data {
		private final int typeId;
		private final int nr;
		private final String area;
		private final String sector;
		private final String problem;
		private final String comment;
		private final int numPitches;
		private final String grade;
		private final String fa;
		private final String faDate;
		private final String trivia;
		public Data(int nr, String area, String sector, String problem, T t, String comment, int numPitches, String grade, String fa, String faDate, String trivia) {
			this.typeId = switch (t) {
			case BOULDER -> 1;
			case BOLT -> 2;
			case TRAD -> 3;
			case MIXED -> 4;
			case TOPROPE -> 5;
			case AID -> 6;
			case AIDTRAD -> 7;
			case ICE -> 10;
			};
			this.nr = nr;
			this.area = area;
			this.sector = sector;
			this.problem = problem.replace(" (nat)", "").replace(" (miks)", "");
			this.comment = comment;
			this.numPitches = numPitches;
			this.grade = grade;
			this.fa = fa;
			this.faDate = faDate;
			this.trivia = trivia;
		}
		public int getTypeId() {
			return typeId;
		}
		public String getArea() {
			return area;
		}
		public String getComment() {
			return comment;
		}
		public int getNumPitches() {
			return numPitches;
		}
		public String getFa() {
			return fa;
		}
		public String getFaDate() {
			return faDate;
		}
		public String getGrade() {
			return grade;
		}
		public int getNr() {
			return nr;
		}
		public String getProblem() {
			return problem;
		}
		public String getSector() {
			return sector;
		}
		public String getTrivia() {
			return trivia;
		}
		@Override
		public String toString() {
			return "Data [typeId=" + typeId + ", nr=" + nr + ", area=" + area + ", sector=" + sector + ", problem="
					+ problem + ", comment=" + comment + ", grade=" + grade + ", fa=" + fa + ", faDate=" + faDate + ", trivia=" + trivia + "]";
		}
	}
	private final static Optional<Integer> AUTH_USER_ID = Optional.of(1);
	private final static int REGION_ID = -1; 
	private final Setup setup;

	public static void main(String[] args) {
		new FillProblems();
	}

	public FillProblems() {
		Preconditions.checkArgument(REGION_ID > 0, "Invalid REGION_ID=" + REGION_ID);
		this.setup = Server.getSetups().stream()
				.filter(x -> x.idRegion() == REGION_ID)
				.findAny()
				.orElseThrow(() -> new RuntimeException("Invalid regionId=" + REGION_ID));
		List<Data> data = new ArrayList<>();
		data.add(new Data(1, "AREA", "SECTOR", "NAME", T.TRAD, "DESCRIPTION", 1, "6+", "USER_1,USER_2&USER_3", "9999-12-31", null)); // TODO
		Preconditions.checkArgument(data.size() > 1, "Invalid data");
		Server.runSql((dao, c) -> {
			for (Data d : data) {
				final int idArea = upsertArea(dao, c, d);
				final int idSector = upsertSector(dao, c, idArea, d);
				insertProblem(dao, c, idArea, idSector, d);
			}
		});
	}

	private List<User> getFas(Dao dao, Connection c, String fa) throws SQLException {
		List<User> res = new ArrayList<>();
		if (!Strings.isNullOrEmpty(fa)) {
			String splitter = fa.contains("&")? "&" : ",";
			for (String userName : fa.split(splitter)) {
				userName = userName.trim();
				if (userCache.containsKey(userName)) {
					res.add(userCache.get(userName));
					continue;
				}
				int id = -1;
				List<User> users = dao.getUserSearch(c, AUTH_USER_ID, userName);
				if (!users.isEmpty()) {
					id = users.getFirst().id();
				}
				User user = User.from(id, userName);
				userCache.put(userName, user);
				res.add(user);
			}
		}
		return res;
	}

	private void insertProblem(Dao dao, Connection c, int idArea, int idSector, Data d) throws IOException, SQLException, InterruptedException {
		logger.debug("insert {}", d);
		List<User> fa = getFas(dao, c, d.getFa());
		Type t = dao.getTypes(c, REGION_ID).stream().filter(x -> x.id() == d.getTypeId()).findFirst().get();
		Problem p = new Problem(null, idArea, false, false, null, null, null, false, -1, -1, idSector, false, false, null, null, null, -1, -1, null, null, null, null, null, null, null, null, -1, null, false, false, false, d.getNr(), d.getProblem(), null, d.getComment(), null, d.getGrade().replaceAll(" ", ""), d.getFaDate(), null, fa, null, null, -1, 0, false, null, t, false, null, null, d.getTrivia(), null, null, null, null, null);
		if (d.getNumPitches() > 1) {
			for (int nr = 1; nr <= d.getNumPitches(); nr++) {
				p.addSection(-1, nr, null, "n/a", new ArrayList<>());
			}
		}
		dao.setProblem(c, AUTH_USER_ID, setup, p, null);
	}

	private int upsertArea(Dao dao, Connection c, Data d) throws IOException, SQLException, InterruptedException {
		if (areaCache.containsKey(d.getArea())) {
			return areaCache.get(d.getArea());
		}
		for (Area a : dao.getAreaList(c, AUTH_USER_ID, REGION_ID)) {
			if (a.getName().equals(d.getArea())) {
				areaCache.put(d.getArea(), a.getId());
				return a.getId();
			}
		}
		Area a = new Area(null, null, -1, false, false, false, false, null, null, false, 0, 0, d.getArea(), null, null, 0, 0, null, null, null, null, null);
		Redirect r = dao.setArea(c, setup, AUTH_USER_ID, a, null);
		areaCache.put(d.getArea(), r.idArea());
		return r.idArea();
	}

	private int upsertSector(Dao dao, Connection c, int idArea, Data d) throws IOException, SQLException, InterruptedException {
		Map<String, Integer> areaSectors = sectorCache.computeIfAbsent(idArea, _ -> new HashMap<>());
		if (areaSectors.containsKey(d.getSector())) {
			return areaSectors.get(d.getSector());
		}
		Area a = Objects.requireNonNull(dao.getArea(c, setup, AUTH_USER_ID, idArea, shouldUpdateHits));
		for (AreaSector s : a.getSectors()) {
			if (s.getName().equals(d.getSector())) {
				areaSectors.put(d.getSector(), s.getId());
				return s.getId();
			}
		}
		Sector s = new Sector(null, false, idArea, false, false, null, null, false, -1, -1, null, -1, false, false, false, d.getSector(), null, null, null, -1, -1, null, null, null, null, null, null, null, null, null, null, null);
		Redirect r = dao.setSector(c, AUTH_USER_ID, setup, s, null);
		areaSectors.put(d.getSector(), r.idSector());
		return r.idSector();
	}
}