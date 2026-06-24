package com.buldreinfo.batch;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

import com.buldreinfo.Application;
import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.AreaRepository;
import com.buldreinfo.dao.ProblemRepository;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.dao.SectorRepository;
import com.buldreinfo.dao.UserRepository;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.model.Area;
import com.buldreinfo.model.Area.AreaSector;
import com.buldreinfo.model.Problem;
import com.buldreinfo.model.Redirect;
import com.buldreinfo.model.Sector;
import com.buldreinfo.model.Type;
import com.buldreinfo.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;

public class FillProblems {
	public static enum T {AID, AIDTRAD, BOLT, BOULDER, ICE, MIXED, TOPROPE, TRAD}
	private class Data {
		private final String area;
		private final String comment;
		private final String fa;
		private final String faDate;
		private final String grade;
		private final int lengthMeter;
		private final int nr;
		private final int numPitches;
		private final String problem;
		private final String sector;
		private final String trivia;
		private final int typeId;
		public Data(int nr, String area, String sector, String problem, T t, String comment, int numPitches, String grade, String fa, String faDate, int lengthMeter, String trivia) {
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
			this.lengthMeter = lengthMeter;
			this.trivia = trivia;
		}
		public String getArea() { return area; }
		public String getComment() { return comment; }
		public String getFa() { return fa; }
		public String getFaDate() { return faDate; }
		public String getGrade() { return grade; }
		public int getLengthMeter() { return lengthMeter; }
		public int getNr() { return nr; }
		public int getNumPitches() { return numPitches; }
		public String getProblem() { return problem; }
		public String getSector() { return sector; }
		public String getTrivia() { return trivia; }
		public int getTypeId() { return typeId; }
	}
	private final static Optional<Integer> AUTH_USER_ID = Optional.of(1);
	private static Logger logger = LogManager.getLogger();
	private final static int REGION_ID = -1;
	public static void main(String[] args) throws Exception {
		var context = new SpringApplicationBuilder(Application.class)
				.web(WebApplicationType.NONE)
				.run(args);
		var txManager = context.getBean(ClimbingTransactionManager.class);
		var areaRepo = context.getBean(AreaRepository.class);
		var problemRepo = context.getBean(ProblemRepository.class);
		var sectorRepo = context.getBean(SectorRepository.class);
		var userRepo = context.getBean(UserRepository.class);
		var regionRepo = context.getBean(RegionRepository.class);
		new FillProblems(txManager, areaRepo, problemRepo, sectorRepo, userRepo, regionRepo);
	}
	private final Map<String, Integer> areaCache = new HashMap<>();
	private final AreaRepository areaRepo;
	private final ProblemRepository problemRepo;
	private final RegionRepository regionRepo;
	private final Map<Integer, Map<String, Integer>> sectorCache = new HashMap<>();
	private final SectorRepository sectorRepo;
	private final Setup setup;
	private final boolean shouldUpdateHits = false; 
	private final Map<String, User> userCache = new HashMap<>();
	private final UserRepository userRepo;

	public FillProblems(ClimbingTransactionManager txManager, AreaRepository areaRepo, ProblemRepository problemRepo, SectorRepository sectorRepo, UserRepository userRepo, RegionRepository regionRepo) throws Exception {
		this.areaRepo = areaRepo;
		this.problemRepo = problemRepo;
		this.sectorRepo = sectorRepo;
		this.userRepo = userRepo;
		this.regionRepo = regionRepo;

		Preconditions.checkArgument(REGION_ID > 0, "Invalid REGION_ID=" + REGION_ID);
		this.setup = regionRepo.getSetups().stream()
				.filter(x -> x.idRegion() == REGION_ID)
				.findAny()
				.orElseThrow(() -> new RuntimeException("Invalid regionId=" + REGION_ID));

		List<Data> data = new ArrayList<>();
		data.add(new Data(1, "AREA", "SECTOR", "NAME", T.TRAD, "DESCRIPTION", 1, "6+", "USER_1,USER_2&USER_3", "9999-12-31", 999, null)); // TODO
		Preconditions.checkArgument(data.size() > 0, "Invalid data");

		txManager.executeInTransaction(() -> {
			for (Data d : data) {
				final int idArea = upsertArea(d);
				final int idSector = upsertSector(idArea, d);
				insertProblem(idArea, idSector, d);
			}
			return null;
		});
	}

	private List<User> getFas(String fa) throws SQLException {
		List<User> res = new ArrayList<>();
		if (fa != null && !fa.isBlank()) {
			String splitter = fa.contains("&")? "&" : ",";
			for (String userName : fa.split(splitter)) {
				userName = userName.trim();
				if (userCache.containsKey(userName)) {
					res.add(userCache.get(userName));
					continue;
				}
				int id = -1;
				List<User> users = userRepo.getUserSearch(AUTH_USER_ID, userName);
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

	private void insertProblem(int idArea, int idSector, Data d) throws SQLException, InterruptedException {
		logger.debug("insert {}", d);
		List<User> fa = getFas(d.getFa());
		Type t = regionRepo.getTypes(REGION_ID).stream().filter(x -> x.id() == d.getTypeId()).findFirst().get();
		Problem p = new Problem(null, idArea, false, false, null, null, null, false, -1, -1, idSector, false, false, null, null, null, -1, -1, null, null, null, null, null, null, -1, null, false, false, false, d.getNr(), d.getProblem(), null, d.getComment(), null, d.getGrade().replaceAll(" ", ""), d.getFaDate(), null, fa, d.getLengthMeter(), null, null, -1, 0, false, null, null, null, t, null, false, null, null, null, d.getTrivia(), null, null, null, null);
		if (d.getNumPitches() > 1) {
			for (int nr = 1; nr <= d.getNumPitches(); nr++) {
				p.addSection(-1, nr, null, "n/a", new ArrayList<>());
			}
		}
		problemRepo.setProblem(AUTH_USER_ID, setup, p);
	}

	private int upsertArea(Data d) throws SQLException, InterruptedException, JsonProcessingException, BeansException {
		if (areaCache.containsKey(d.getArea())) {
			return areaCache.get(d.getArea());
		}
		for (Area a : areaRepo.getAreaList(AUTH_USER_ID, REGION_ID)) {
			if (a.name().equals(d.getArea())) {
				areaCache.put(d.getArea(), a.id());
				return a.id();
			}
		}
		Area a = new Area(null, null, -1, false, false, false, false, null, null, false, 0, 0, d.getArea(), null, null, 0, 0, null, null, null, null, null, null);
		Redirect r = areaRepo.setArea(setup, AUTH_USER_ID, a);
		areaCache.put(d.getArea(), r.idArea());
		return r.idArea();
	}

	private int upsertSector(int idArea, Data d) throws SQLException, InterruptedException, JsonProcessingException, BeansException {
		Map<String, Integer> areaSectors = sectorCache.computeIfAbsent(idArea, _ -> new HashMap<>());
		if (areaSectors.containsKey(d.getSector())) {
			return areaSectors.get(d.getSector());
		}
		Area a = Objects.requireNonNull(areaRepo.getArea(setup, AUTH_USER_ID, idArea, shouldUpdateHits));
		for (AreaSector s : a.sectors()) {
			if (s.name().equals(d.getSector())) {
				areaSectors.put(d.getSector(), s.id());
				return s.id();
			}
		}
		Sector s = new Sector(null, false, idArea, false, false, null, null, false, -1, -1, null, -1, false, false, false, d.getSector(), null, null, null, -1, -1, null, null, null, null, null, null, null, null, null, null, null, null);
		Redirect r = sectorRepo.setSector(AUTH_USER_ID, setup, s);
		areaSectors.put(d.getSector(), r.idSector());
		return r.idSector();
	}
}