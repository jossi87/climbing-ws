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
//		data.add(new Data(1, "Kårstø", "Førsteveggen", "Morsmelk", T.BOLT, "Bolt: 4+2", 1, "4", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(2, "Kårstø", "Førsteveggen", "Espresso", T.BOLT, "Bolt: 5", 1, "6+", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(3, "Kårstø", "Førsteveggen", "Rødsprit", T.BOLT, "Bolt: 5", 1, "6+", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(4, "Kårstø", "Førsteveggen", "Bayer", T.BOLT, "Bolt: 5", 1, "6", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(5, "Kårstø", "Førsteveggen", "Hostesaft", T.BOLT, "Bolt: 5", 1, "6-", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(6, "Kårstø", "Førsteveggen", "Styrepils", T.BOLT, "Bolt: 4", 1, "6-", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(7, "Kårstø", "Førsteveggen", "Litago", T.BOLT, "Bolt: 3+2", 1, "6+", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(1, "Kårstø", "Hovedveggen", "Riskhospitalet", T.BOLT, "Bolt: 4", 1, "7-", "Rudi Schrøder", null, 0, 0));
//		data.add(new Data(2, "Kårstø", "Hovedveggen", "Fis", T.BOLT, "Bolt: 2", 1, "7-", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(3, "Kårstø", "Hovedveggen", "Kujon", T.BOLT, "Bolt: 3", 1, "5-", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(4, "Kårstø", "Hovedveggen", "Knotteliten", T.BOLT, "Bolt: 4", 1, "6", "Anne Olufsen", null, 0, 0));
//		data.add(new Data(5, "Kårstø", "Hovedveggen", "Balle Klorin", T.BOLT, "Bolt: 4", 1, "5+", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(6, "Kårstø", "Hovedveggen", "Epoksy", T.BOLT, "Bolt: 6", 1, "6+", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(7, "Kårstø", "Hovedveggen", "Lueludder", T.BOLT, "Bolt: 4", 1, "6+", "Rudi Schrøder", null, 0, 0));
//		data.add(new Data(8, "Kårstø", "Hovedveggen", "Handikt", T.BOLT, "Bolt: 3", 1, "5+", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(9, "Kårstø", "Hovedveggen", "Grønn mirage", T.BOLT, "Bolt: 5", 1, "6", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(10, "Kårstø", "Hovedveggen", "Den røde baron", T.BOLT, "Bolt: 4", 1, "6", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(11, "Kårstø", "Hovedveggen", "Bakkestart", T.BOLT, "Bolt: 6", 1, "7+", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(12, "Kårstø", "Hovedveggen", "Basketaket", T.BOLT, "Bolt: 6+2", 1, "7", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(13, "Kårstø", "Hovedveggen", "Bråkjekk", T.BOLT, "Bolt: 6+2", 1, "7+", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(14, "Kårstø", "Hovedveggen", "Fallos", T.BOLT, "Bolt: 4", 1, "7-", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(15, "Kårstø", "Hovedveggen", "Den beskjedne melkemannen", T.BOLT, "Bolt: 5", 1, "6-", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(16, "Kårstø", "Hovedveggen", "Mitt Afrika", T.BOLT, "Bolt: 4", 1, "7-", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(17, "Kårstø", "Hovedveggen", "L", T.BOLT, "Bolt: 5", 1, "7-", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(18, "Kårstø", "Hovedveggen", "U", T.BOLT, "Bolt: 6", 1, "7-", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(19, "Kårstø", "Hovedveggen", "Triggerhappy", T.BOLT, "Bolt: 7", 1, "8", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(20, "Kårstø", "Hovedveggen", "Potetmoses", T.BOLT, "Bolt: 6", 1, "7+/8-", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(21, "Kårstø", "Hovedveggen", "Kafé ka faen", T.BOLT, "Bolt: 5", 1, "7/7+", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(22, "Kårstø", "Hovedveggen", "Klamme hender", T.BOLT, "Bolt: 7", 1, "8/8+", "Håkon Hansen", null, 0, 0));
//		data.add(new Data(23, "Kårstø", "Hovedveggen", "Det drypper", T.BOLT, "Bolt: 7", 1, "8+", "Håkon Hansen", null, 0, 0));
//		data.add(new Data(24, "Kårstø", "Hovedveggen", "Blasfeminin", T.BOLT, "Bolt: 7", 1, "8-", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(25, "Kårstø", "Hovedveggen", "Vill, vakker og våt", T.BOLT, "Bolt: 6", 1, "8", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(26, "Kårstø", "Hovedveggen", "I", T.BOLT, "Bolt: 6", 1, "7+/8-", "Are Bjørnsgaard", null, 0, 0));
//		data.add(new Data(27, "Kårstø", "Hovedveggen", "Hylletur", T.BOLT, "Bolt: 7+2", 1, "7+", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(28, "Kårstø", "Hovedveggen", "Litt på kanten", T.BOLT, "Bolt: 6", 1, "7+/8-", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(29, "Kårstø", "Hovedveggen", "Enigma", T.BOLT, "Bolt: 6", 1, "7-", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(30, "Kårstø", "Hovedveggen", "Bulldetektor", T.BOLT, "Bolt: 6", 1, "8-", "Anders Kindlihagen", null, 0, 0));
//		data.add(new Data(31, "Kårstø", "Hovedveggen", "Sadomaskinisten", T.BOLT, "Bolt: 6", 1, "8", "Gunnar Karlsen", null, 0, 0));
//		data.add(new Data(32, "Kårstø", "Hovedveggen", "Hizbollah", T.BOLT, "Bolt: 5", 1, "7+/8-", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(33, "Kårstø", "Hovedveggen", "[åpent prosjekt]", T.BOLT, "Bolt: 5", 1, "9-", "Anders Kindlihagen", null, 0, 0));
//		data.add(new Data(34, "Kårstø", "Hovedveggen", "Kan'kje finna fingen", T.BOLT, "Bolt: 5", 1, "7/7+", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(35, "Kårstø", "Hovedveggen", "På leit", T.BOLT, "Bolt: 6", 1, "7-", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(1, "Kårstø", "Guleveggen", "Ex Undis", T.BOLT, "Bolt: 4", 1, "4+", "Sandra Kristin Svensen", null, 0, 0));
//		data.add(new Data(2, "Kårstø", "Guleveggen", "Steinulv", T.BOLT, "Bolt: 5", 1, "7-", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(3, "Kårstø", "Guleveggen", "Bujumbura", T.BOLT, "Bolt: 6", 1, "8+", "Håkon Hansen", null, 0, 0));
//		data.add(new Data(4, "Kårstø", "Guleveggen", "Jalalabad", T.BOLT, "Bolt: 6", 1, "8-", "Ståle Brokvam", null, 0, 0));
//		data.add(new Data(5, "Kårstø", "Guleveggen", "Fluxus", T.BOLT, "Bolt: 5", 1, "7", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(6, "Kårstø", "Guleveggen", "Bløgg", T.BOLT, "Bolt: 5", 1, "8-", "Anders Kindlihagen", null, 0, 0));
//		data.add(new Data(7, "Kårstø", "Guleveggen", "Judasevangeliet", T.BOLT, "Bolt: 6", 1, "8-", "Ståle Brokvam", null, 0, 0));
//		data.add(new Data(8, "Kårstø", "Guleveggen", "Et svare skrev", T.BOLT, "Bolt: 6", 1, "8-", "Ståle Brokvam", null, 0, 0));
//		data.add(new Data(9, "Kårstø", "Guleveggen", "Bevernylon", T.BOLT, "Bolt: 6", 1, "7", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(10, "Kårstø", "Guleveggen", "Den olympiske sild", T.BOLT, "Bolt: 6", 1, "7+", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(1, "Kårstø", "Gråveggen", "Tamiltigeren", T.BOLT, "Bolt: 7", 1, "5-", "Rudi Schrøder", null, 0, 0));
//		data.add(new Data(2, "Kårstø", "Gråveggen", "Sendero Luminoso", T.BOLT, "Bolt: 5", 1, "6-", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(3, "Kårstø", "Gråveggen", "Summa Sumatra", T.BOLT, "Bolt: 6", 1, "7-", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(4, "Kårstø", "Gråveggen", "Gulebøy", T.BOLT, "Bolt: 5", 1, "6+", "Annika Klippenberg", null, 0, 0));
//		data.add(new Data(5, "Kårstø", "Gråveggen", "AN 31", T.BOLT, "Bolt: 7", 1, "6", "Anlaug Nygard", null, 0, 0));
//		data.add(new Data(6, "Kårstø", "Gråveggen", "Den hissige sauebonden", T.BOLT, "Bolt: 6", 1, "7-", "Bernt Ove Reinertsen", null, 0, 0));
//		data.add(new Data(7, "Kårstø", "Gråveggen", "Ban Thai", T.BOLT, "Bolt: 7", 1, "7-", "Leif Jensen", null, 0, 0));
//		data.add(new Data(8, "Kårstø", "Gråveggen", "Brigate Rossi", T.BOLT, "Bolt: 5", 1, "6+", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(9, "Kårstø", "Gråveggen", "IRA", T.BOLT, "Bolt: 6", 1, "6-", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(10, "Kårstø", "Gråveggen", "Tinnsoldaten", T.BOLT, "Bolt: 5", 1, "6-", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(11, "Kårstø", "Gråveggen", "Den stygge andungen", T.BOLT, "Bolt: 6", 1, "5+", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(12, "Kårstø", "Gråveggen", "Feit og klebrig", T.BOLT, "Bolt: 5", 1, "5-", "Rudi Schrøder", null, 0, 0));
//		data.add(new Data(1, "Kårstø", "Blokkveggen", "Bensin", T.BOLT, "Bolt: 5", 1, "6", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(2, "Kårstø", "Blokkveggen", "Strømlaus", T.BOLT, "Bolt: 5", 1, "6", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(3, "Kårstø", "Blokkveggen", "Tetris", T.BOLT, "Bolt: 5", 1, "6", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(4, "Kårstø", "Blokkveggen", "Tigris", T.BOLT, "Bolt: 3+2", 1, "7", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(5, "Kårstø", "Blokkveggen", "Baris", T.BOLT, "Bolt: 4", 1, "7", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(6, "Kårstø", "Blokkveggen", "Spenning i hverdagen", T.BOLT, "Bolt: 7+2", 1, "7-", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(7, "Kårstø", "Blokkveggen", "Løse rykter", T.BOLT, "Bolt: 6+2", 1, "6-", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(1, "Kårstø", "Kraterveggen", "Krasj", T.BOLT, "Bolt: 4+2", 1, "6-", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(2, "Kårstø", "Kraterveggen", "Krater", T.BOLT, "Bolt: 4+2", 1, "5+", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(3, "Kårstø", "Kraterveggen", "Kråke", T.BOLT, "Bolt: 4+2", 1, "5+", "Lars Audun Nornes", null, 0, 0));
//		data.add(new Data(4, "Kårstø", "Kraterveggen", "Kake", T.BOLT, "Bolt: 4+2", 1, "5-", "Lars Audun Nornes", null, 0, 0));
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
//		Problem p = new Problem(idArea, 0, null, idSector, 0, null, 0, 0, null, null, null, -1, 0, d.getNr(), d.getProblem(), d.getComment(), null, d.getGrade().replaceAll(" ", ""), d.getFaDate(), null, fa, d.getLat(), d.getLng(), null, 0, 0, false, null, t, false, 0);
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
//		Area a = new Area(REGION_ID, null, -1, 0, d.getArea(), null, 0, 0, -1, -1, null, null, 0);
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
//		Sector s = new Sector(false, idArea, 0, a.getName(), null, -1, 0, d.getSector(), null, 0, 0, null, null, null, null, 0);
//		s = c.getBuldreinfoRepo().setSector(AUTH_USER_ID, false, new MetaHelper().getSetup(REGION_ID), s, null);
//		return s.getId();
//	}
//}