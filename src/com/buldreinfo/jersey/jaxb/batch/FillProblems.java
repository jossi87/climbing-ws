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
//	public static enum T {BOLT, TRAD, MIXED, TOPROPE, AID, AIDTRAD};
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
//	private final static int REGION_ID = 9;
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
//		data.add(new Data(1, "Svartdalen", "Skytebanen", "The Asscrack Jammers", T.TRAD, null, 1, "6", "Oskar Roshed, Hans Inga", "1999-01-01", 0, 0));
//		data.add(new Data(2, "Svartdalen", "Skytebanen", "Vårkänsla", T.TRAD, null, 1, "6", "Rick McGregor, Håkan Grudd", "1999-01-01", 0, 0));
//		data.add(new Data(3, "Svartdalen", "Skytebanen", "Rectum Prospector", T.TRAD, null, 1, "4", "Håkan Grudd, Rick McGregor", "1999-01-01", 0, 0));
//		data.add(new Data(4, "Svartdalen", "Skytebanen", "Øvre Skytebanen", T.TRAD, null, 3, "7", "Erik Heyman, Malin Holmberg", "2001-01-01", 0, 0));
//		data.add(new Data(1, "Svartdalen", "Blanke Hælvete", "The Edge", T.TOPROPE, "Climb the obvious arrete. No bolts, just yet..", 1, "6", null, null, 0, 0));
//		data.add(new Data(2, "Svartdalen", "Blanke Hælvete", "Blanke Hælvete", T.BOLT, "15m - Sharp crip fest, unfortunately it take a high toll on the skin and multiple tries in the same week are hard to afford.", 1, "8/8+", "Frode Sobhi, Dagfinn Eilertsen", "2007-01-01", 0, 0));
//		data.add(new Data(3, "Svartdalen", "Blanke Hælvete", "Satanrisset", T.TRAD, "15m - The most obvious crack in the corner on the left side of Blank Hælvete.", 1, "6", "Håkon Wegge", "2019-01-01", 0, 0));
//		data.add(new Data(4, "Svartdalen", "Blanke Hælvete", "Fantarisset", T.TRAD, "15m - Start as Satanrisset, then right", 1, "5+", "Anna Nystedt", "2019-01-01", 0, 0));
//		data.add(new Data(5, "Svartdalen", "Blanke Hælvete", "Jam for rettferdighet", T.TRAD, "15m - One #4 cam can be nice to have", 1, "5+", "Håkon Wegge", "2019-01-01", 0, 0));
//		data.add(new Data(1, "Svartdalen", "Warspite", "Ildflue", T.BOLT, "Glue in bolts", 1, "7", "Tobias Boström, Jan Carlsson", "2008-01-01", 0, 0));
//		data.add(new Data(2, "Svartdalen", "Warspite", "VVS", T.BOLT, "Start on the pile of loose rocks", 1, "7+", "Jan Eirik Holen", "2003-01-01", 0, 0));
//		data.add(new Data(3, "Svartdalen", "Warspite", "Fallos", T.BOLT, "Shares the start and anchor with VVS", 1, "7-", "Jan Eirik Holen", "2003-01-01", 0, 0));
//		data.add(new Data(4, "Svartdalen", "Warspite", "Kill Bill", T.BOLT, null, 1, "8+", "Frode Sobhi, Fredrik Hansson", "2007-01-01", 0, 0));
//		data.add(new Data(5, "Svartdalen", "Warspite", "Warspite", T.MIXED, "You need a cam and a runout on the upper part. For now..", 1, "9-", "Robert Caspersen, Dagfinn Eilertsen", "2000-01-01", 0, 0));
//		data.add(new Data(6, "Svartdalen", "Warspite", "Tore den Sterke Klatreren", T.BOLT, null, 1, "7-", "Dagfinn Eilertsen", "2000-01-01", 0, 0));
//		data.add(new Data(7, "Svartdalen", "Warspite", "Blokka", T.BOLT, null, 1, "6+", null, null, 0, 0));
//		data.add(new Data(1, "Svartdalen", "Baris", "Hjørnet", T.TOPROPE, "Everything is loose. Stay away!", 1, "6+", null, null, 0, 0));
//		data.add(new Data(2, "Svartdalen", "Baris", "Onjerka", T.BOLT, "Ever since the block fell out the route hasn't been climbed. Hangers should be removed", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(3, "Svartdalen", "Baris", "Skitnödig", T.BOLT, null, 1, "6", "Anders Bergwall", "2003-01-01", 0, 0));
//		data.add(new Data(4, "Svartdalen", "Baris", "Sommernatt", T.BOLT, "Shares anchor with route on the left.", 1, "7-", "Dagfinn Eilertsen", "2000-01-01", 0, 0));
//		data.add(new Data(5, "Svartdalen", "Baris", "Baris", T.BOLT, null, 1, "6+", "Tore Dreyer", "2000-01-01", 0, 0));
//		data.add(new Data(6, "Svartdalen", "Baris", "Gangsterparadiset", T.BOLT, null, 1, "6", "Jan Eirik Holen", "2003-01-01", 0, 0));
//		data.add(new Data(7, "Svartdalen", "Baris", "Samejenta", T.BOLT, "Starts from the same ledge as Gangsterparadiset", 1, "7", "Dagfinn Eilertsen", "2000-01-01", 0, 0));
//		data.add(new Data(8, "Svartdalen", "Baris", "Tele Sauvage", T.BOLT, "Follow the crack to 'Fanden på veggen' and follow this line.", 1, "8-", "Jan Eirik Holen", "2003-01-01", 0, 0));
//		data.add(new Data(9, "Svartdalen", "Baris", "Fanden på veggen", T.BOLT, "Chipped!", 1, "8-", "Frode Sobhi, Jan Eirik Holen", "2003-01-01", 0, 0));
//		data.add(new Data(10, "Svartdalen", "Baris", "Mini", T.TRAD, "Short and beautifull. Looks like jamming but there's a way around", 1, "6+", "Dagfinn Eilertsen", "2000-01-01", 0, 0));
//		data.add(new Data(11, "Svartdalen", "Baris", "Barbert Fyrverkeri", T.TRAD, "Take big cams", 1, "6+", null, null, 0, 0));
//		data.add(new Data(1, "Svartdalen", "KiF", "KiF", T.BOLT, "20m - Once was 7b. Very varried climbing", 1, "7+", "Dagfinn Eilertsen", "2000-01-01", 0, 0));
//		data.add(new Data(2, "Svartdalen", "KiF", "Route 22", T.MIXED, "Rather loose crack", 1, "6-", null, null, 0, 0));
//		data.add(new Data(3, "Svartdalen", "KiF", "Route 23", T.MIXED, null, 1, "6", null, null, 0, 0));
//		data.add(new Data(4, "Svartdalen", "KiF", "Slaget om Narvik", T.BOLT, "The black streak. Might need some brushing on top.", 1, "8+", "Frode Sobhi, Fredrik Hansson", "2004-01-01", 0, 0));
//		data.add(new Data(1, "Svartdalen", "Sau-veggen", "Sauen", T.BOLT, "20m - Start on the wall, go through the roof on the right and up the slab", 1, "8-", "Håkon Wegge", "2019-01-01", 0, 0));
//		data.add(new Data(2, "Svartdalen", "Sau-veggen", "Kristenmannsblod", T.BOLT, "20m - Same anchor as Sauen. Two finger crimping at its best.", 1, "9-", "Håkon Wegge", "2019-01-01", 0, 0));
//		data.add(new Data(3, "Svartdalen", "Sau-veggen", "Gakk-Gakk", T.BOLT, "15m - Start in the corner, up to the anchor in the middle of the wall, right above the unnecessery bolt.", 1, "7-", "Stian Bruvoll, Håkon Wegge", "2019-01-01", 0, 0));
//		data.add(new Data(4, "Svartdalen", "Sau-veggen", "Gach-Gach", T.BOLT, "20m - Extension of Gakk-Gakk, climb through the roof up the slab.", 1, "7+", "Håkon Wegge", "2019-01-01", 0, 0));
//		data.add(new Data(1, "Svartdalen", "Millenial-veggen", "Closed Project C U Next Tuesday", T.BOLT, "20m", 1, "n/a", "Steven Van Dijck, Håkon Wegge", "2019-01-01", 0, 0));
//		data.add(new Data(2, "Svartdalen", "Millenial-veggen", "Closed Project Dreamwhipper", T.TRAD, "20m - Shares anchor with CUNT", 1, "n/a", "Håkon Wegge", "2019-01-01", 0, 0));
//		data.add(new Data(1, "Svartdalen", "Counting the Days", "Counting the Days", T.TRAD, "55m", 1, "6+", "Rick McGregor, Fredrik Hansson, A. Stålnacke", "2005-01-01", 0, 0));
//		data.add(new Data(1, "Svartdalen", "Øvre-veggen", "Grus i Ögat", T.TRAD, "Face climb with no protection", 1, "6-", "Gustav Mellgren", "2007-01-01", 0, 0));
//		data.add(new Data(2, "Svartdalen", "Øvre-veggen", "Lärdomen", T.TRAD, null, 1, "7-", "Tobias Boström", "2007-01-01", 0, 0));
//		data.add(new Data(3, "Svartdalen", "Øvre-veggen", "Hjørnedans", T.TRAD, "Crack right of an obvious arete", 1, "5", "Gustav Mellgren", "2007-01-01", 0, 0));
//		data.add(new Data(4, "Svartdalen", "Øvre-veggen", "Uppvarmingsäventyret", T.TRAD, null, 2, "5+", "Tobias Boström, Jan Carlsson", "2007-01-01", 0, 0));
//		data.add(new Data(1, "Stiberg", "Stiberg", "Lady", T.TRAD, null, 1, "6+", "Jørn Bjerk", "2008-01-01", 0, 0));
//		data.add(new Data(2, "Stiberg", "Stiberg", "Landstrykeren", T.TOPROPE, null, 1, "7/7+", "Jan Carlsson", null, 0, 0));
//		data.add(new Data(3, "Stiberg", "Stiberg", "Fem Plus", T.TRAD, "15m", 1, "7-", "Håkon Wegge", "2019-01-01", 0, 0));
//		data.add(new Data(4, "Stiberg", "Stiberg", "Pump-o-rama", T.TRAD, "35m", 1, "8", "Fredrik Hansson", "2009-01-01", 0, 0));
//		data.add(new Data(5, "Stiberg", "Stiberg", "Crack-a-go-go", T.TRAD, "35m", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(6, "Stiberg", "Stiberg", "En ring av selvtillit", T.MIXED, null, 1, "5", "Thorbjørn Enevold", "1980-01-01", 0, 0));
//		data.add(new Data(7, "Stiberg", "Stiberg", "Slaverekken", T.TRAD, "35m", 1, "6+", "Thorbjørn Enevold, Steinulv Aarebrot", "1991-01-01", 0, 0));
//		data.add(new Data(8, "Stiberg", "Stiberg", "La Linja", T.TRAD, "35m", 1, "7+", "Steinulv Aarebrot", "1991-01-01", 0, 0));
//		data.add(new Data(9, "Stiberg", "Stiberg", "Inn i Grannskauen", T.TRAD, "20m", 1, "7-", "Steinulv Aarebrot", "1991-01-01", 0, 0));
//		data.add(new Data(10, "Stiberg", "Stiberg", "Flatfoot Sam", T.TRAD, null, 1, "6", "Thorbjørn Enevold", "1980-01-01", 0, 0));
//		data.add(new Data(11, "Stiberg", "Stiberg", "Peanøtthjerneforbundet", T.TRAD, null, 1, "6", "Ole Ivar Lied", "2004-01-01", 0, 0));
//		data.add(new Data(12, "Stiberg", "Stiberg", "Helkroppsmasage", T.TRAD, null, 1, "6", "Mikael af Ekenstam, Tobias Boström", "2008-01-01", 0, 0));
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
//		Problem p = new Problem(idArea, 0, null, idSector, 0, null, 0, 0, null, null, null, -1, 0, d.getNr(), d.getProblem(), d.getComment(), null, d.getGrade().replaceAll(" ", ""), d.getFaDate(), null, fa, d.getLat(), d.getLng(), null, 0, 0, false, null, t, false);
//		if (d.getNumPitches() > 1) {
//			for (int nr = 1; nr <= d.getNumPitches(); nr++) {
//				p.addSection(-1, nr, null, "n/a");
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
//		Area a = new Area(REGION_ID, null, -1, 0, d.getArea(), null, 0, 0, -1, -1, null, null);
//		a = c.getBuldreinfoRepo().setArea(AUTH_USER_ID, REGION_ID, a, null);
//		return a.getId();
//	}
//
//	private int upsertSector(DbConnection c, int idArea, Data d) throws IOException, SQLException, NoSuchAlgorithmException, InterruptedException {
//		Area a = Preconditions.checkNotNull(c.getBuldreinfoRepo().getArea(AUTH_USER_ID, idArea));
//		for (Area.Sector s : a.getSectors()) {
//			if (s.getName().equals(d.getSector())) {
//				return s.getId();
//			}
//		}
//		Sector s = new Sector(false, idArea, 0, a.getName(), null, -1, 0, d.getSector(), null, 0, 0, null, null, null, null);
//		s = c.getBuldreinfoRepo().setSector(AUTH_USER_ID, false, new MetaHelper().getSetup(REGION_ID), s, null);
//		return s.getId();
//	}
//}