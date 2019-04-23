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
//	
//	private class Data {
//		private final int typeId;
//		private final int nr;
//		private final String area;
//		private final String sector;
//		private final String problem;
//		private final String comment;
//		private final String grade;
//		private final String fa;
//		private final String faDate;
//		public Data(int nr, String area, String sector, String problem, String comment, String grade, String fa, String faDate) {
//			if (problem.endsWith(" (nat)")) {
//				this.typeId = 3;
//			}
//			else if (problem.endsWith(" (miks)")) {
//				this.typeId = 4;
//			}
//			else {
//				this.typeId = 2;
//			}
//			this.nr = nr;
//			this.area = area;
//			this.sector = sector;
//			this.problem = problem.replace(" (nat)", "").replace(" (miks)", "");
//			this.comment = comment;
//			this.grade = grade;
//			this.fa = fa;
//			this.faDate = faDate;
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
//	private final static int REGION_ID = 8;
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
//		data.add(new Data(1, "Brensholmen", "Armdreparveggen", "Lillediederet (nat)", "25m", "5", null, null));
//		data.add(new Data(2, "Brensholmen", "Armdreparveggen", "Trollmannen fra åz", "20m", "6", "Audun Igesund", "2004-01-01"));
//		data.add(new Data(3, "Brensholmen", "Armdreparveggen", "Ju-ju", "5m, 5 quickdraws", "7-", "Audun Igesund", "2004-01-01"));
//		data.add(new Data(4, "Brensholmen", "Armdreparveggen", "Met.no", "25m, 9 quickdraws", "7", "Jan-Erik Paulsen", "2000-01-01"));
//		data.add(new Data(5, "Brensholmen", "Armdreparveggen", "Skeive Skriver", "20m, 7 quickdraws", "6+", "Audun Igesund", null));
//		data.add(new Data(6, "Brensholmen", "Armdreparveggen", "Armdreparn (nat)", "30m", "6+", "Sjur Nesheim", "1982-01-01"));
//		data.add(new Data(7, "Brensholmen", "Armdreparveggen", "Project", null, "n/a", "Audun Igesund", null));
//		data.add(new Data(8, "Brensholmen", "Armdreparveggen", "Project: Brenshora", "20m, 6 quickdraws", "9-/9", "Øystein Andresen", null));
//		data.add(new Data(9, "Brensholmen", "Armdreparveggen", "Brent av frost", "20m, 6 quickdraws", "8/8+", "Øystein Andresen", "2005-01-01"));
//		data.add(new Data(10, "Brensholmen", "Armdreparveggen", "La meg vaere tung", "25m, 9 quickdraws", "8-", "Audun Igesund", "1996-01-01"));
//		data.add(new Data(11, "Brensholmen", "Armdreparveggen", "Groove is in the heart", "20m, 8 quickdraws", "8", "Øystein Andresen", "2006-01-01"));
//		data.add(new Data(12, "Brensholmen", "Armdreparveggen", "4 nyanser av brunt", "20m, 9 quickdraws", "8+", "Øystein Andresen", "2005-01-01"));
//		data.add(new Data(13, "Brensholmen", "Armdreparveggen", "Tequila, sex and marihuana", "20m, 9 quickdraws", "8-", "Holger Keilholz", "2004-01-01"));
//		data.add(new Data(14, "Brensholmen", "Armdreparveggen", "Ever b Marley", "20m, 8 quickdraws", "8", "Holger Keilholz", "2004-01-01"));
//		data.add(new Data(15, "Brensholmen", "Armdreparveggen", "Cheezy dipper", "20m, 6 quickdraws", "6-/6", "Øystein Andresen", "2006-01-01"));
//		data.add(new Data(16, "Brensholmen", "Armdreparveggen", "Cherry popper (nat)", "20m", "5-", "Thomas Meling", "2005-01-01"));
//		data.add(new Data(17, "Brensholmen", "Armdreparveggen", "Kiler i magen (nat)", "7m", "7-", "Audun Igesund", "1996-01-01"));
//		data.add(new Data(18, "Brensholmen", "Armdreparveggen", "Trad rules (nat)", "7m", "7-/7", "Øystein Andresen, Thomas Meling", "2005-01-01"));
//		data.add(new Data(19, "Brensholmen", "Armdreparveggen", "Nøtteliten (nat)", "7m", "6-", "Thomas Meling, Ivar Martens", "2005-01-01"));
//		data.add(new Data(20, "Brensholmen", "Armdreparveggen", "Veslenøtt (nat)", "7m", "4-", null, null));
//		data.add(new Data(21, "Brensholmen", "Panoramaveggen", "Juggas", "15m, 5 quickdraws", "6", "Jørgen Drangfelt", "2004-01-01"));
//		data.add(new Data(22, "Brensholmen", "Panoramaveggen", "Haela i taket", "20m, 8 quickdraws", "7-", "Øystein Andresen", "2004-01-01"));
//		data.add(new Data(23, "Brensholmen", "Panoramaveggen", "Patrik lever (nat)", "20m", "6-", "E. Hagan, M. Brox", "1997-01-01"));
//		data.add(new Data(24, "Brensholmen", "Panoramaveggen", "Cafe au lait", "15m, 5 quickdraws", "6+", "Øystein Andresen", "2004-01-01"));
//		data.add(new Data(25, "Brensholmen", "Panoramaveggen", "Smutthullet", "15m, 4 quickdraws", "7", "Øystein Andresen", "2004-01-01"));
//		data.add(new Data(26, "Brensholmen", "Panoramaveggen", "Panorama cafe", "35m, 15 quickdraws", "5", "Øystein Andresen", "2004-01-01"));
//		data.add(new Data(27, "Brensholmen", "Panoramaveggen", "Nesheim-ruta", "15m, 6 quickdraws", "6-", "Sjur Nesheim", "1980-01-01"));
//		data.add(new Data(28, "Brensholmen", "Panoramaveggen", "Psycho 1", "15m, 5 quickdraws", "6+/7-", "Øystein Andresen", "2006-01-01"));
//		data.add(new Data(29, "Brensholmen", "Panoramaveggen", "Psycho 2", "20m, 6 quickdraws", "6+/7-", "Øystein Andresen", "2006-01-01"));
//		data.add(new Data(30, "Brensholmen", "Panoramaveggen", "Drømme eggen (nat)", "20m", "7", "Øystein Andresen, Petter Restorp", "1999-01-01"));
//		data.add(new Data(31, "Brensholmen", "Panoramaveggen", "Stordiedret (nat)", "20m", "5+", null, "1980-01-01"));
//		data.add(new Data(32, "Brensholmen", "Panoramaveggen", "Den botaniske hage (nat)", "20m", "5+", "Lorentz Mandal", "2006-01-01"));
//		data.add(new Data(33, "Brensholmen", "Panoramaveggen", "Barduekspressen (nat)", "20m", "5-", "Thor Henrik Larsen", "2005-01-01"));
//		data.add(new Data(34, "Brensholmen", "Panoramaveggen", "Melings Kamin (nat)", "20m", "5", "Thomas Meling", "2005-01-01"));
//		data.add(new Data(35, "Brensholmen", "Panoramaveggen", "VIP lounge", "20m, 9 quickdraws", "5", "Øystein Andresen", "2004-01-01"));
//		data.add(new Data(1, "Brensholmen", "Zenith", "Mannen som likte å klatre (nat)", "20m", "7-", "Holger Keilholz", "2005-01-01"));
//		data.add(new Data(2, "Brensholmen", "Zenith", "Sommershow", "25m, 9 quickdraws", "8/8+", "holger Keilholz", "2004-01-01"));
//		data.add(new Data(3, "Brensholmen", "Zenith", "Artisk sommer", "25m, 9 quickdraws", "7-", "holger Keilholz", "2004-01-01"));
//		data.add(new Data(4, "Brensholmen", "Zenith", "Blinde ku", "25m, 9 quickdraws", "7-", "Holger Keilholz", "2004-01-01"));
//		data.add(new Data(5, "Brensholmen", "Zenith", "Eckpfeiler der demokratie", "25m, 10 quickdraws", "7", "Holger Keilholz", "2004-01-01"));
//		data.add(new Data(6, "Brensholmen", "Zenith", "The little John (nat)", "25m", "5+", "Holger Keilholz", "2004-01-01"));
//		data.add(new Data(7, "Brensholmen", "Zenith", "Draugen (nat)", "25m", "6-", "Holger Keilholz", "2005-01-01"));
//		data.add(new Data(1, "Gullknausen", "Karatveggen", "Platinabaren", "25m, 11 quickdraws", "5+", "Bettina Aasnes", "2003-01-01"));
//		data.add(new Data(2, "Gullknausen", "Karatveggen", "Fransk åpning", "10m, 8 quickdraws", "6", "Leif Henning Broch Johnsen", "1997-01-01"));
//		data.add(new Data(3, "Gullknausen", "Karatveggen", "Kontinentalsprekken (nat)", "30m", "5", "Svein Smelvaer", "1997-01-01"));
//		data.add(new Data(4, "Gullknausen", "Karatveggen", "18 karat", "30m, 11 quickdraws", "6", "Ben Johnsen, Svein Smelvaer", "1997-01-01"));
//		data.add(new Data(5, "Gullknausen", "Karatveggen", "Gullrushet (nat)", "30m", "5+", "Roy Inge Hansen, Svein Smelvaer", "1997-01-01"));
//		data.add(new Data(6, "Gullknausen", "Karatveggen", "Gyllene tider (nat)", "30m", "7-", "Mårten Blixt", "2003-01-01"));
//		data.add(new Data(7, "Gullknausen", "Karatveggen", "Project: Vertigo (nat)", "30m", "7+", "Øystein Andresen", "2005-01-01"));
//		data.add(new Data(8, "Gullknausen", "Karatveggen", "Project: Gull-iver (nat)", "15m", "6+", "Øystein Andresen", "2005-01-01"));
//		data.add(new Data(9, "Gullknausen", "Karatveggen", "Project: Goldfinger", "15m", "n/a", "Øystein Andresen", "2005-01-01"));
//		data.add(new Data(1, "Gullknausen", "Brattveggen", "Det gyldne snitt", "30m, 10 quickdraws", "8-", "Øystein Andresen", "2005-01-01"));
//		data.add(new Data(2, "Gullknausen", "Brattveggen", "Golden shower", "30m, 10 quickdraws", "8-/8", "Øystein Andresen", "2005-01-01"));
//		data.add(new Data(3, "Gullknausen", "Brattveggen", "Naut eller astronaut", "30m, 12 quickdraws", "8", "Øystein Andresen", "2005-01-01"));
//		data.add(new Data(4, "Gullknausen", "Brattveggen", "Man or astromen (nat)", "30m", "8", "Mårten Blixt", "2004-01-01"));
//		data.add(new Data(5, "Gullknausen", "Brattveggen", "Project: Man and astromen (nat)", "60m", "n/a", "Mårten Blixt", "2005-01-01"));
//		data.add(new Data(6, "Gullknausen", "Brattveggen", "Project: El dorado", "35m, 15 quickdraws", "n/a", "Øystein Andresen", "2005-01-01"));
//		data.add(new Data(7, "Gullknausen", "Brattveggen", "Fidel", "30m, 11 quickdraws", "9-", "Leif Henning Broch Johnsen", "2000-01-01"));
//		data.add(new Data(1, "Gullknausen", "Brosmeveggen", "Akantus", "25m, 11 quickdraws", "7+", "Øystein Andresen", "1997-01-01"));
//		data.add(new Data(2, "Gullknausen", "Brosmeveggen", "Project", "60m", "n/a", "Øystein Andresen", "2005-01-01"));
//		data.add(new Data(3, "Gullknausen", "Brosmeveggen", "Brosmelina", "30m, 10 quickdraws", "8-", "Gisle Andersen", "1997-01-01"));
//		data.add(new Data(4, "Gullknausen", "Brosmeveggen", "Flikkflak", "100m", "5", "Roy Inge Hansen, Leif Henning Broch Johnsen", "1997-01-01"));
//		data.add(new Data(5, "Gullknausen", "Brosmeveggen", "Araber flikkflak", "30m, 12 quickdraws", "6+", "Øystein Andresen", "2003-01-01"));
//		data.add(new Data(1, "Grøtfjorden", "Hovedeggen", "Energi mot apati (nat)", "50m", "6", "Arild Meyer, Sjur Nesheim", "1982-01-01"));
//		data.add(new Data(2, "Grøtfjorden", "Hovedeggen", "Fett konsept", "10m, 6 quickdraws", "7+", "Leif Henning Broch Johnsen", "1991-01-01"));
//		data.add(new Data(3, "Grøtfjorden", "Hovedeggen", "Overhengende fare", "20m, 6 quickdraws", "6-", "Ben Johnsen", "1991-01-01"));
//		data.add(new Data(4, "Grøtfjorden", "Hovedeggen", "Ballettmester andersens likkiste", "20m, 8 quickdraws", "7", "Einar and Leif Henning Broch Johnsen", "1991-01-01"));
//		data.add(new Data(5, "Grøtfjorden", "Hovedeggen", "Piruett på kistelokket", "20m, 6 quickdraws", "7-", "Einar and Leif Henning Broch Johnsen", "1992-01-01"));
//		data.add(new Data(6, "Grøtfjorden", "Hovedeggen", "God must be a boogieman (nat)", "50m", "6-", "Sjur Nesheim", "1985-01-01"));
//		data.add(new Data(7, "Grøtfjorden", "Hovedeggen", "Giv alt (nat)", "25m", "7-", "Einar and Leif Henning Broch Johnsen", "1991-01-01"));
//		data.add(new Data(8, "Grøtfjorden", "Hovedeggen", "Giv akt! (nat)", "35m", "6+", "Håvard Nesheim", "1981-01-01"));
//		data.add(new Data(9, "Grøtfjorden", "Hovedeggen", "Den Norske Amerikalinje (nat)", "25m", "7-", "Arild Meyer, Tim Hansen", "1981-01-01"));
//		data.add(new Data(10, "Grøtfjorden", "Hovedeggen", "Christians testament (nat)", "25m", "7-", "Christian Korvald", "1991-01-01"));
//		data.add(new Data(11, "Grøtfjorden", "Hovedeggen", "Gje faen (nat)", "25m", "6-", "Arild Meyer, Sjur Nesheim", "1981-01-01"));
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
//		Problem p = new Problem(idArea, 0, null, idSector, 0, null, 0, 0, null, -1, 0, d.getNr(), d.getProblem(), d.getComment(), null, d.getGrade().replaceAll(" ", ""), d.getFaDate(), null, fa, 0, 0, null, 0, 0, false, null, t, false);
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
//		Sector s = new Sector(false, idArea, 0, a.getName(), null, -1, 0, d.getSector(), null, 0, 0, null, null, null);
//		s = c.getBuldreinfoRepo().setSector(AUTH_USER_ID, false, REGION_ID, s, null);
//		return s.getId();
//	}
//}