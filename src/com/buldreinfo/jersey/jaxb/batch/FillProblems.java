//package com.buldreinfo.jersey.jaxb.batch;
//
//import java.io.IOException;
//import java.security.NoSuchAlgorithmException;
//import java.sql.SQLException;
//import java.text.ParseException;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//
//import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
//import com.buldreinfo.jersey.jaxb.db.DbConnection;
//import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
//import com.buldreinfo.jersey.jaxb.metadata.MetaHelper;
//import com.buldreinfo.jersey.jaxb.metadata.beans.Setup;
//import com.buldreinfo.jersey.jaxb.model.Area;
//import com.buldreinfo.jersey.jaxb.model.FaUser;
//import com.buldreinfo.jersey.jaxb.model.Problem;
//import com.buldreinfo.jersey.jaxb.model.Sector;
//import com.buldreinfo.jersey.jaxb.model.Type;
//import com.buldreinfo.jersey.jaxb.model.User;
//import com.google.common.base.Preconditions;
//import com.google.common.base.Strings;
//
//public class FillProblems {
//	private static Logger logger = LogManager.getLogger();
//	public static enum T {BOLT, TRAD, MIXED, TOPROPE, AID, AIDTRAD, ICE};
//
//	private class Data {
//		private final int typeId;
//		private final int nr;
//		private final String area;
//		private final String sector;
//		private final String problem;
//		private final String comment;
//		private final int numPitches;
//		private final String grade;
//		private final String fa;
//		private final String faDate;
//		private final double lat;
//		private final double lng;
//		public Data(int nr, String area, String sector, String problem, T t, String comment, int numPitches, String grade, String fa, String faDate, double lat, double lng) {
//			if (t.equals(T.BOLT)) {
//				this.typeId = 2;
//			}
//			else if (t.equals(T.TRAD)) {
//				this.typeId = 3;
//			}
//			else if (t.equals(T.MIXED)) {
//				this.typeId = 4;
//			}
//			else if (t.equals(T.TOPROPE)) {
//				this.typeId = 5;
//			}
//			else if (t.equals(T.AID)) {
//				this.typeId = 6;
//			}
//			else if (t.equals(T.AIDTRAD)) {
//				this.typeId = 7;
//			}
//			else if (t.equals(T.ICE)) {
//				this.typeId = 10;	
//			}
//			else {
//				throw new RuntimeException("Invalid t=" + t);
//			}
//			this.nr = nr;
//			this.area = area;
//			this.sector = sector;
//			this.problem = problem.replace(" (nat)", "").replace(" (miks)", "");
//			this.comment = comment;
//			this.numPitches = numPitches;
//			this.grade = grade;
//			this.fa = fa;
//			this.faDate = faDate;
//			this.lat = lat;
//			this.lng = lng;
//		}
//		public double getLng() {
//			return lng;
//		}
//		public double getLat() {
//			return lat;
//		}
//		public int getTypeId() {
//			return typeId;
//		}
//		public String getArea() {
//			return area;
//		}
//		public String getComment() {
//			return comment;
//		}
//		public int getNumPitches() {
//			return numPitches;
//		}
//		public String getFa() {
//			return fa;
//		}
//		public String getFaDate() {
//			return faDate;
//		}
//		public String getGrade() {
//			return grade;
//		}
//		public int getNr() {
//			return nr;
//		}
//		public String getProblem() {
//			return problem;
//		}
//		public String getSector() {
//			return sector;
//		}
//		@Override
//		public String toString() {
//			return "Data [typeId=" + typeId + ", nr=" + nr + ", area=" + area + ", sector=" + sector + ", problem="
//					+ problem + ", comment=" + comment + ", grade=" + grade + ", fa=" + fa + ", faDate=" + faDate + "]";
//		}
//	}
//	private final static int AUTH_USER_ID = 1;
//	private final static int REGION_ID = 4;
//	private final Setup setup;
//
//	public static void main(String[] args) {
//		new FillProblems();
//	}
//
//	public FillProblems() {
//		this.setup = new MetaHelper().getSetup(REGION_ID);
//		List<Data> data = new ArrayList<>();
//		// FA-date: yyyy-MM-dd
//		data.add(new Data(1, "Skeisfjell", "Skeisfjell", "Svada", T.TRAD, "160m - Lite tiltalende, dårlig sikra og grufsete dieder på 3. tau. Fint riss i 4. taulengde", 4, "6-", "Leif Jensen,Eimhjellen", "1991-01-01", 0, 0));
//		data.add(new Data(2, "Skeisfjell", "Skeisfjell", "Discovery", T.TRAD, "160m", 4, "5+", "Nils Engelstad,Thomas", "1980-06-01", 0, 0));
//		data.add(new Data(3, "Skeisfjell", "Skeisfjell", "Discovery direkte avslutning", T.TRAD, "160m", 4, "5+", "Per Markestad,Harald Bjørgen", "1982-06-01", 0, 0));
//		data.add(new Data(4, "Skeisfjell", "Skeisfjell", "Nogbad the bad", T.TRAD, "160m - Noe skuffende rute som krysser \"Nogging the Nog\"", 4, "6+", "Lee,Morley", "1987-08-01", 0, 0));
//		data.add(new Data(5, "Skeisfjell", "Skeisfjell", "Nogging the nog", T.TRAD, "160m - Skeisfjells hardeste rute. Sentral linje opp midt på veggen. Starter med innlysende tynn sprekk, til venstre for et løst flak ca. 6m fra bakken.", 4, "7+", "Lee,Woodley", "1987-08-01", 0, 0));
//		data.add(new Data(6, "Skeisfjell", "Skeisfjell", "Recovery", T.TRAD, "160m", 3, "5+", "Mike Blenkinsop", "1979-07-01", 0, 0));
//		data.add(new Data(6, "Skeisfjell", "Skeisfjell", "Deflection", T.TRAD, "160m", 3, "5+", "Mike Blenkinsop,Holman", "1979-06-01", 0, 0));
//		data.add(new Data(7, "Skeisfjell", "Skeisfjell", "Svenneprøven", T.TRAD, "Ny avslutning på Deflection (se detalj)", 3, "6-", "Leif Jensen,Bjordal", "1987-09-01", 0, 0));
//		data.add(new Data(8, "Skeisfjell", "Skeisfjell", "Dragsug", T.TRAD, "90m - Starter under overheng. Fin bratt rute. På 2. tau er cruxet å komme inn i diederet. Fordel med flexi Friend no: 1.", 2, "7-", "Olav Båsen,Knutsen,Elder", "1989-07-01", 0, 0));
//		data.add(new Data(9, "Skeisfjell", "Skeisfjell", "Trodde noen at friklatring var tøffest??", T.AIDTRAD, "120m (A4) - Start sammen med Rainstorm", 4, "5+", "Olav Båsen,Leif Jensen", "1990-10-01", 0, 0));
//		data.add(new Data(10, "Skeisfjell", "Skeisfjell", "Rainstorm", T.TRAD, "130m", 3, "5+", "Mike Blenkinsop,John Fivelsdal", "1980-07-01", 0, 0));
//
//		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
//			for (Data d : data) {
//				final int idArea = upsertArea(c, d);
//				final int idSector = upsertSector(c, idArea, d);
//				insertProblem(c, idArea, idSector, d);
//			}
//			c.setSuccess();
//		} catch (Exception e) {
//			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
//		}
//	}
//
//	private List<FaUser> getFas(DbConnection c, String fa) throws SQLException {
//		List<FaUser> res = new ArrayList<>();
//		if (!Strings.isNullOrEmpty(fa)) {
//			String splitter = fa.contains("&")? "&" : ",";
//			for (String user : fa.split(splitter)) {
//				user = user.trim();
//				int id = -1;
//				List<User> users = c.getBuldreinfoRepo().getUserSearch(AUTH_USER_ID, user);
//				if (!users.isEmpty()) {
//					id = users.get(0).getId();
//				}
//				res.add(new FaUser(id, user, null));
//			}
//		}
//		return res;
//	}
//
//	private void insertProblem(DbConnection c, int idArea, int idSector, Data d) throws IOException, SQLException, NoSuchAlgorithmException, InterruptedException, ParseException {
//		logger.debug("insert {}", d);
//		List<FaUser> fa = getFas(c, d.getFa());
//		Type t = c.getBuldreinfoRepo().getTypes(REGION_ID).stream().filter(x -> x.getId() == d.getTypeId()).findFirst().get();
//		Problem p = new Problem(idArea, false, false, null, idSector, false, false, null, 0, 0, null, null, -1, -1, null, -1, false, false, d.getNr(), d.getProblem(), d.getComment(), null, d.getGrade().replaceAll(" ", ""), d.getFaDate(), null, fa, d.getLat(), d.getLng(), null, 0, 0, false, null, t, false, 0);
//		if (d.getNumPitches() > 1) {
//			for (int nr = 1; nr <= d.getNumPitches(); nr++) {
//				p.addSection(-1, nr, null, "n/a", new ArrayList<>());
//			}
//		}
//		c.getBuldreinfoRepo().setProblem(AUTH_USER_ID, setup, p, null);
//	}
//
//	private int upsertArea(DbConnection c, Data d) throws IOException, SQLException, NoSuchAlgorithmException, InterruptedException {
//		for (Area a : c.getBuldreinfoRepo().getAreaList(AUTH_USER_ID, REGION_ID)) {
//			if (a.getName().equals(d.getArea())) {
//				return a.getId();
//			}
//		}
//		Area a = new Area(REGION_ID, null, -1, false, false, false, d.getArea(), null, 0, 0, 0, 0, null, null, 0);
//		a = c.getBuldreinfoRepo().setArea(new MetaHelper().getSetup(REGION_ID), AUTH_USER_ID, a, null);
//		return a.getId();
//	}
//
//	private int upsertSector(DbConnection c, int idArea, Data d) throws IOException, SQLException, NoSuchAlgorithmException, InterruptedException {
//		Area a = Preconditions.checkNotNull(c.getBuldreinfoRepo().getArea(new MetaHelper().getSetup(REGION_ID), AUTH_USER_ID, idArea));
//		for (Area.Sector s : a.getSectors()) {
//			if (s.getName().equals(d.getSector())) {
//				return s.getId();
//			}
//		}
//		Sector s = new Sector(false, idArea, false, false, a.getName(), null, -1, false, false, d.getSector(), null, 0, 0, null, null, null, null, 0);
//		s = c.getBuldreinfoRepo().setSector(AUTH_USER_ID, false, new MetaHelper().getSetup(REGION_ID), s, null);
//		return s.getId();
//	}
//}