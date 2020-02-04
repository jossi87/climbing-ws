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
//import com.google.common.collect.Lists;
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
//	private final static int REGION_ID = 11;
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
//		data.add(new Data(1, "Marvik", "Hovedveggen", "Svarta Bjørn", T.BOLT, "6bb (17m)", 1, "5+", "Anne Olufsen", "2001-03-25", 0, 0));
//		data.add(new Data(2, "Marvik", "Hovedveggen", "Hufsa", T.BOLT, "7bb (17m). Hylla og risset skal renses bedre.", 1, "6-", "Anne Olufsen", "2001-03-24", 0, 0));
//		data.add(new Data(3, "Marvik", "Hovedveggen", "Sniff", T.BOLT, "5bb (17m). Får et par ekstra bolter etterhvert?", 1, "6-", "Siv Wikberg", "2001-05-12", 0, 0));
//		data.add(new Data(4, "Marvik", "Hovedveggen", "Hattifnatten", T.BOLT, "9bb (18m). Innsteg litt fra venstre.", 1, "6", "Rudi Schrøder", "2001-04-07", 0, 0));
//		data.add(new Data(5, "Marvik", "Hovedveggen", "Hemulen", T.BOLT, "8bb (18m)", 1, "6+", "Rudi Schrøder", "2001-05-05", 0, 0));
//		data.add(new Data(6, "Marvik", "Hovedveggen", "Stinky", T.BOLT, "9bb (22m). Første (og andre?) bolt flyttes. Renses bedre.", 1, "6-", "Rudi Schrøder", "2001-07-27", 0, 0));
//		data.add(new Data(7, "Marvik", "Hovedveggen", "Prosjekt", T.BOLT, "22m. Gjennom taket (ikke boltet).", 1, "7", "Thomas Hahn", null, 0, 0));
//		data.add(new Data(8, "Marvik", "Hovedveggen", "48 kromosomer", T.BOLT, "7bb (22m). Rett opp i toppen!", 1, "7", "Anders Kindlihagen", "2001-03-25", 0, 0));
//		data.add(new Data(9, "Marvik", "Hovedveggen", "Kællt i tælt", T.BOLT, "7bb (22m)", 1, "7", "Anders Kindlihagen", "2001-03-25", 0, 0));
//		data.add(new Data(10, "Marvik", "Hovedveggen", "Nappetassen", T.BOLT, "9bb (23m)", 1, "7", "Odd Arne Hemmingstad", "2001-04-29", 0, 0));
//		data.add(new Data(11, "Marvik", "Hovedveggen", "Pyrisept & norgesplaster", T.BOLT, "8bb (24m). 7+ m juget en drøy m til v. for 4. bolt (tilh. Nappet.!).", 1, "7+/8-", "Anders Kindlihagen", "2001-04-07", 0, 0));
//		data.add(new Data(12, "Marvik", "Hovedveggen", "Svart Oktober", T.BOLT, "8bb (24m)", 1, "8-", "Anders Kindlihagen", "2001-04-21", 0, 0));
//		data.add(new Data(13, "Marvik", "Hovedveggen", "Prosjekt", T.BOLT, "6bb (19m). Prosjekt", 1, "8", "Jon Inge Haraldseid", null, 0, 0));
//		data.add(new Data(14, "Marvik", "Hovedveggen", "Lille My", T.BOLT, "8bb (20m)", 1, "7/7+", "Anne Olufsen", "2001-05-17", 0, 0));
//		data.add(new Data(15, "Marvik", "Hovedveggen", "Hugo Healer", T.BOLT, "8bb (20m). Lett 7- opp til hylla", 1, "7/7+", "Anders Kindlihagen", "2001-03-24", 0, 0));
//		data.add(new Data(16, "Marvik", "Hovedveggen", "Prosjekt?", T.BOLT, "22m. Ikke boltet", 1, "8", null, null, 0, 0));
//		data.add(new Data(17, "Marvik", "Hovedveggen", "Prosjekt (Baller i luften)", T.BOLT, "8bb (22m). Prosjekt.", 1, "9-", "Anders Kindlihagen", null, 0, 0));
//		data.add(new Data(18, "Marvik", "Hovedveggen", "Prosjekt (Ola Flytt)", T.BOLT, "9bb (22m). Prosjekt.", 1, "9-/9", "Håkon Hansen", null, 0, 0));
//		data.add(new Data(19, "Marvik", "Hovedveggen", "Straumen dans", T.BOLT, "9bb (24m)", 1, "8/8+", "Håkon Hansen", "2001-05-03", 0, 0));
//		data.add(new Data(20, "Marvik", "Hovedveggen", "Maakeberget", T.BOLT, "9bb (25m)", 1, "8-", "Håkon Hansen", "2001-04-22", 0, 0));
//		data.add(new Data(21, "Marvik", "Hovedveggen", "Prosjekt (Bala Garba)", T.BOLT, "10bb (25m). Prosjekt", 1, "9-/9", "Håkon Hansen", null, 0, 0));
//		data.add(new Data(22, "Marvik", "Hovedveggen", "Skvis", T.BOLT, "11bb (28m). Renses noe bedre i toppen...", 1, "8/8+", "Anders Kindlihagen", "2001-05-24", 0, 0));
//		data.add(new Data(23, "Marvik", "Hovedveggen", "Prosjekt", T.BOLT, "30m. Delv. boltet", 1, "8+", "Håkon Hansen", null, 0, 0));
//		data.add(new Data(24, "Marvik", "Hovedveggen", "AnderSLange", T.BOLT, "16bb (32m). Går såvidt med 60 m tau. Pass på tauenden.", 1, "8", "Anders Kindlihagen", "2001-05-17", 0, 0));
//		data.add(new Data(25, "Marvik", "Hovedveggen", "Prosjekt 1", T.BOLT, "32m. Rett til høyre for ASL (ikke boltet).", 1, "8-", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(26, "Marvik", "Hovedveggen", "Prosjekt 2", T.BOLT, "25m. Bratt kompakt vegg (ikke boltet). Grad 8/9", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(27, "Marvik", "Hovedveggen", "Prosjekt 3", T.BOLT, "25m. Bratt kompakt vegg (ikke boltet). Grad 8/9", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(28, "Marvik", "Hovedveggen", "Rullende bombe", T.BOLT, "5bb (12m). Svaet oppe til høyre", 1, "5-", "KS", "2004-09-01", 0, 0));
//		data.add(new Data(29, "Marvik", "Hovedveggen", "Høy veskeføring", T.BOLT, "5bb (12m). Høyre kant av svaet oppe til høyre", 1, "4+", "KS", "2004-09-01", 0, 0));
//		data.add(new Data(1, "Marvik", "Filmsvaet", "My precious", T.BOLT, "18m", 1, "6+", "Rudi Schrøder", "2007-06-19", 0, 0));
//		data.add(new Data(2, "Marvik", "Filmsvaet", "Laura kofta", T.BOLT, "18m", 1, "7-", "Rudi Schrøder", "2007-06-19", 0, 0));
//		data.add(new Data(1, "Bleikmyr", "Bleikmyr", "Fargeblind med demens", T.BOLT, "5bb (16m). Boltelinja til venstre for det opplagte risset", 1, "6-", "Håkon Hansen", "2002-05-22", 0, 0));
//		data.add(new Data(2, "Bleikmyr", "Bleikmyr", "Fissura serrata", T.TRAD, "16m. Det opplagte risset", 1, "6-", "Thomas Hahn", "1999-05-01", 0, 0));
//		data.add(new Data(3, "Bleikmyr", "Bleikmyr", "Fissura Veriforme", T.TRAD, "15m. Felles start m Hahnekammen, så inn i diederet til høyre", 1, "5-", "Thomas Hahn", "2002-06-02", 0, 0));
//		data.add(new Data(4, "Bleikmyr", "Bleikmyr", "Belsebub", T.BOLT, "5bb (15m). Flata til v for Ryggrad", 1, "7-", "Thomas Hahn", "2002-06-05", 0, 0));
//		data.add(new Data(5, "Bleikmyr", "Bleikmyr", "Ryggrad", T.TRAD, "16m. Den eroderte \"ryggraden\"", 1, "6+", "Anders Kindlihagen", "1998-05-20", 0, 0));
//		data.add(new Data(6, "Bleikmyr", "Bleikmyr", "Jarlsberg", T.TRAD, "16m. Følg hullene…", 1, "6-", "Anders Kindlihagen", "1998-05-19", 0, 0));
//		data.add(new Data(7, "Bleikmyr", "Bleikmyr", "Gavepakken", T.TRAD, "16m. Tynt riss til venstre for diederet", 1, "6", "Anders Kindlihagen", "1999-05-20", 0, 0));
//		data.add(new Data(8, "Bleikmyr", "Bleikmyr", "Dunkel skov", T.BOLT, "4bb (9m). Flate inne i hjørnet", 1, "4+", "MH", "2002-06-05", 0, 0));
//		data.add(new Data(9, "Bleikmyr", "Bleikmyr", "Lettpåtå", T.TRAD, "12m. Uregelmessig riss ute på hjørnet. Ikke anker.", 1, "4+", "Anders Kindlihagen", "1999-05-20", 0, 0));
//		data.add(new Data(10, "Bleikmyr", "Bleikmyr", "Sveitser", T.TRAD, "13m. Opp riss, så til venstre gjennom tak", 1, "7-", "Anders Kindlihagen", "1998-05-19", 0, 0));
//		data.add(new Data(11, "Bleikmyr", "Bleikmyr", "Eggerøre", T.BOLT, "4bb (12m). Risset til venstre ikke med. 7- med risset.", 1, "7", "Anders Kindlihagen", "1998-07-21", 0, 0));
//		data.add(new Data(12, "Bleikmyr", "Bleikmyr", "Prosjekt?", T.TRAD, "Liten vegg til h for \"Eggerørenull\"", 1, "n/a", "Thomas Hahn", null, 0, 0));
//		data.add(new Data(13, "Bleikmyr", "Bleikmyr", "Kort og godt", T.TRAD, "7m. Stort riss. Andre sida av stien. Også en fin, litt høy, bulder", 1, "5+", "Anders Kindlihagen", "1998-05-21", 0, 0));
//		data.add(new Data(1, "Espevik", "Svingveggen", "Navnløs?", T.BOLT, "6bb (16m)", 1, "7", "Jon Inge Haraldseid", "2000-05-01", 0, 0));
//		data.add(new Data(2, "Espevik", "Svingveggen", "Amanda", T.BOLT, "7bb (17m)", 1, "7+", "Håkon Hansen", "1985-08-16", 0, 0));
//		data.add(new Data(3, "Espevik", "Svingveggen", "Arabershow", T.BOLT, "7bb (18m)", 1, "7+", "Håkon Hansen", "1985-08-01", 0, 0));
//		data.add(new Data(4, "Espevik", "Svingveggen", "Sildarisp", T.BOLT, "6bb (18m). Risset midt på veggen", 1, "6+", "Håkon Hansen", "1985-01-01", 0, 0));
//		data.add(new Data(5, "Espevik", "Svingveggen", "Sjakkmatt", T.BOLT, "6bb (17m)", 1, "7-", "Håkon Hansen", "1985-01-01", 0, 0));
//		data.add(new Data(6, "Espevik", "Svingveggen", "La det svinge", T.BOLT, "6bb (16m)", 1, "6+", "Anders Kindlihagen", "2000-06-04", 0, 0));
//		data.add(new Data(7, "Espevik", "Svingveggen", "Vårsleppet", T.BOLT, "5bb (15m)", 1, "6-", "Anders Kindlihagen", "2000-05-06", 0, 0));
//		data.add(new Data(1, "Espevik", "Første etasje", "Lars Hertervig", T.TRAD, "Fint riss oppe til venstre et sted. Muligens feil inntegnet?", 1, "5+", "Håkon Hansen", "1983-01-01", 0, 0));
//		data.add(new Data(2, "Espevik", "Første etasje", "Krusifikset", T.TRAD, "15m. En bit til venstre for selve hovedveggen", 1, "6-", "Håkon Hansen", "1984-01-01", 0, 0));
//		data.add(new Data(3, "Espevik", "Første etasje", "Prosjekt", T.BOLT, "20m. Hepp, hepp Jon Inge!", 1, "7", "Jon Inge Haraldseid", null, 0, 0));
//		data.add(new Data(4, "Espevik", "Første etasje", "Akrobatisk", T.BOLT, "10bb (25m)", 1, "8/8+", "Håkon Hansen", "1987-05-20", 0, 0));
//		data.add(new Data(5, "Espevik", "Første etasje", "Evighetsmaskinen", T.BOLT, "25m, gamle bolter", 1, "8", "Håkon Hansen", "1987-05-14", 0, 0));
//		data.add(new Data(6, "Espevik", "Første etasje", "Intenst", T.BOLT, "25m, gamle bolter", 1, "8/8+", "Håkon Hansen", "1987-06-19", 0, 0));
//		data.add(new Data(7, "Espevik", "Første etasje", "Åpent prosjekt", T.BOLT, "25m, ikke boltet", 1, "9-/9", null, null, 0, 0));
//		data.add(new Data(8, "Espevik", "Første etasje", "Friksjonshysteriet", T.BOLT, "22m, gamle bolter", 1, "8", "Håkon Hansen", "1987-02-08", 0, 0));
//		data.add(new Data(9, "Espevik", "Første etasje", "General abstinens", T.BOLT, "8bb (20m). Gamle bolter skal fjernes", 1, "7+", "Håkon Hansen", "1986-06-24", 0, 0));
//		data.add(new Data(10, "Espevik", "Første etasje", "Utsøkte unnskyldninger", T.BOLT, "20m, gamle bolter", 1, "7/7+", "Håkon Hansen", "1986-06-18", 0, 0));
//		data.add(new Data(11, "Espevik", "Første etasje", "Tvilsomme unnskyldninger", T.BOLT, "11bb (23m). Felles start med Utsøkte u.", 1, "7/7+", "Anders Kindlihagen", "2000-08-13", 0, 0));
//		data.add(new Data(12, "Espevik", "Første etasje", "Dysleksi", T.BOLT, "17m. Nytt toppfeste, gamle bolter", 1, "8-", "Håkon Hansen, Alv Borge", "1986-06-26", 0, 0));
//		data.add(new Data(13, "Espevik", "Første etasje", "Predestinasjonen", T.BOLT, "16m. Nytt toppfeste, gamle bolter", 1, "8-", "Håkon Hansen", "1986-07-03", 0, 0));
//		data.add(new Data(14, "Espevik", "Første etasje", "Sagittarius", T.TRAD, "15m. Nytt toppfeste", 1, "6", "Håkon Hansen, TI", "1984-01-01", 0, 0));
//		data.add(new Data(15, "Espevik", "Andre etasje", "Åpent prosjekt", T.TRAD, "40m. Venstre dieder", 1, "7-", null, null, 0, 0));
//		data.add(new Data(16, "Espevik", "Andre etasje", "Svakrus", T.BOLT, "15bb (45m). Flott rute på svaplata mellom venstre og  midtre dieder", 1, "7", "Anders Kindlihagen", "2000-03-28", 0, 0));
//		data.add(new Data(17, "Espevik", "Andre etasje", "Sterk ryggrad", T.TRAD, "43m. Midtre dieder", 1, "6+", "PM, Håkon Hansen", "1984-01-01", 0, 0));
//		data.add(new Data(18, "Espevik", "Andre etasje", "Krympefri", T.BOLT, "13bb (43m). Svaplata mellom midtre og høyre dieder", 1, "6+", "Anders Kindlihagen, Anne Olufsen", "2000-08-13", 0, 0));
//		data.add(new Data(19, "Espevik", "Andre etasje", "Mutasjon", T.TRAD, "35m. Høyre dieder", 1, "6", "Håkon Hansen, PM", "1984-01-01", 0, 0));
//		data.add(new Data(20, "Espevik", "Tredje etasje", "Svaking", T.BOLT, "18bb (50m). Det kompakte svaet til høyre i toppen", 1, "6", "Anders Kindlihagen", "2000-03-28", 0, 0));
//		data.add(new Data(1, "Hodnafjell", "Taket", "Hop Sing", T.BOLT, "4bb (14m). På frontveggen til v for den bratte delen", 1, "6-", "Jon Inge Haraldseid", "2002-05-04", 0, 0));
//		data.add(new Data(2, "Hodnafjell", "Taket", "Rabalderbarnet", T.BOLT, "4bb (13m). Lengst til venstre i bratta", 1, "8-", "Anders Kindlihagen", "2002-05-02", 0, 0));
//		data.add(new Data(3, "Hodnafjell", "Taket", "Makko", T.BOLT, "4bb (13m). V del av overh, opp til felles anker m \"Rabalderbarnet\"", 1, "8", "Anders Kindlihagen", "2002-05-02", 0, 0));
//		data.add(new Data(4, "Hodnafjell", "Taket", "Prosjekt (Makko m/gylf)", T.BOLT, "11bb (25m). Start opp \"Makko\", ut leppe til høyre og opp langs egg", 1, "9-/9", "Anders Kindlihagen", null, 0, 0));
//		data.add(new Data(5, "Hodnafjell", "Taket", "Prosjekt (Sloggi for menn)", T.BOLT, "12bb (25m). Sentralt i overhenget og opp fin toppvegg.", 1, "9-", "Anders Kindlihagen", null, 0, 0));
//		data.add(new Data(6, "Hodnafjell", "Taket", "Krabaten kommer", T.BOLT, "4bb (13m). Starter i v del av \"starttaket\", skrår mot høyre til hylla.", 1, "7+", "Anders Kindlihagen", "2002-04-03", 0, 0));
//		data.add(new Data(7, "Hodnafjell", "Taket", "Lys i krana", T.BOLT, "5bb (13m). Start på v. side av blokk. Felles anker m \"Krabaten\"", 1, "7", "Anders Kindlihagen", "2002-03-22", 0, 0));
//		data.add(new Data(8, "Hodnafjell", "Taket", "Changalabanga", T.BOLT, "4bb (13m). Helt til høyre i bratta og opp til hylla", 1, "7-", "Anders Kindlihagen", "2002-05-02", 0, 0));
//		data.add(new Data(9, "Hodnafjell", "Taket", "Prosjekt 1", T.BOLT, "25m. Hovedeggen til venstre for stort takoverheng (ikke boltet).", 1, "n/a", "Håkon Hansen", null, 0, 0));
//		data.add(new Data(10, "Hodnafjell", "Taket", "Prosjekt 2", T.BOLT, "20m. Felles start m 9, så v vegg til h for egg (ikke boltet)", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(11, "Hodnafjell", "Taket", "Prosjekt 3", T.BOLT, "20m. Felles start m 8/9 el 12, så diederet opp til taket (ikke boltet)", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(12, "Hodnafjell", "Taket", "Prosjekt 4", T.BOLT, "20m. Sentralt opp veggen under taket (ikke boltet)", 1, "7-", "Anne Olufsen", null, 0, 0));
//		data.add(new Data(13, "Hodnafjell", "Taket", "Hodnahakket", T.BOLT, "10bb (21m). Høyre kant av veggen under taket", 1, "6", "Rudi Schrøder", "2002-05-07", 0, 0));
//		data.add(new Data(14, "Hodnafjell", "Taket", "Prosjekt (Jazztobakk)", T.BOLT, "9bb (23m). Start på blokk. Opp på h side av stort takoverh", 1, "7", "Rudi Schrøder", null, 0, 0));
//		data.add(new Data(1, "Hodnafjell", "Klov", "Prosjekt 1", T.BOLT, "18m. No ruklete start til v så fint flak på bratt toppvegg", 1, "7", null, null, 0, 0));
//		data.add(new Data(2, "Hodnafjell", "Klov", "Hengepung", T.BOLT, "5bb (16m). Venstre eggen i den bratteste delen", 1, "7+/8-", "Anders Kindlihagen", "2002-06-03", 0, 0));
//		data.add(new Data(3, "Hodnafjell", "Klov", "Prosjekt 2", T.BOLT, "17m. Rett til h for eggen", 1, "8+", null, null, 0, 0));
//		data.add(new Data(4, "Hodnafjell", "Klov", "Flatulens", T.BOLT, "6bb (16m). Taket og flaket til v for det store diederet", 1, "8", "Anders Kindlihagen", "2002-07-16", 0, 0));
//		data.add(new Data(5, "Hodnafjell", "Klov", "Prosjekt 3", T.BOLT, "18m. Gjennom taket og videre i det store diederet", 1, "7+", "Rudi Schrøder", null, 0, 0));
//		data.add(new Data(6, "Hodnafjell", "Klov", "Prosjekt 4", T.BOLT, "18m. Gjennom taket og videre på veggen til h for diederet", 1, "8-", null, null, 0, 0));
//		data.add(new Data(7, "Hodnafjell", "Klov", "Prosjekt 5", T.BOLT, "18m. Lengst til h i bratteste høyeste delen", 1, "7", null, null, 0, 0));
//		data.add(new Data(1, "Karmøy", "Haga", "Veiarbeid", T.TRAD, "10m. Start i dieder i v hjørne av veggen", 1, "7-", "Lars Audun Nornes", "2002-04-01", 0, 0));
//		data.add(new Data(2, "Karmøy", "Haga", "Prosjekt 1", T.BOLT, "10m", 1, "8+", null, null, 0, 0));
//		data.add(new Data(3, "Karmøy", "Haga", "Blindpassasjer", T.TRAD, "10m. Start litt til v for det tydelige risset", 1, "8-", "Lars Audun Nornes", "2002-04-01", 0, 0));
//		data.add(new Data(4, "Karmøy", "Haga", "Pilotrisset", T.TRAD, "10m. Det opplagte risset. Godt sikret bortsett fra starten.", 1, "7", "Lars Audun Nornes", "2002-04-01", 0, 0));
//		data.add(new Data(5, "Karmøy", "Haga", "Prosjekt 2", T.BOLT, "10m", 1, "8+", null, null, 0, 0));
//		data.add(new Data(6, "Karmøy", "Haga", "Bakkemannskap", T.TRAD, "10m. Rissystem lengst til h i veggen", 1, "6+", "Lars Audun Nornes", "2002-04-01", 0, 0));
//		data.add(new Data(1, "Karmøy", "Syre", "Siratrodlet (prosjekt)", T.BOLT, "4bb (10m). Hepp, hepp Odd Arne", 1, "7", "Odd Arne Hemmingstad", null, 0, 0));
//		data.add(new Data(2, "Karmøy", "Syre", "Syretrip", T.BOLT, "4bb (12m)", 1, "8-", "Anders Kindlihagen", "1999-08-06", 0, 0));
//		data.add(new Data(3, "Karmøy", "Syre", "Mulig prosjekt 1", T.BOLT, null, 1, "7+", null, null, 0, 0));
//		data.add(new Data(4, "Karmøy", "Syre", "Mulig prosjekt 2", T.BOLT, null, 1, "7", null, null, 0, 0));
//		data.add(new Data(5, "Karmøy", "Syre", "Mulig prosjekt 3", T.BOLT, "Fantastisk linje", 1, "9", null, null, 0, 0));
//		data.add(new Data(6, "Karmøy", "Syre", "Syrlig kuling (prosjekt)", T.BOLT, "6bb (14m). Hepp, hepp Anders", 1, "8", "Anders Kindlihagen", null, 0, 0));
//		data.add(new Data(7, "Karmøy", "Syre", "Mulig prosjekt 4", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(8, "Karmøy", "Syre", "Mulig prosjekt 5", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(1, "Karmøy", "Visnes", "Navnløs 1", T.TRAD, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Karmøy", "Visnes", "Bubo", T.BOLT, null, 1, "6", "Magne-Henrik Velde", "2005-01-01", 0, 0));
//		data.add(new Data(3, "Karmøy", "Visnes", "Navnløs 2", T.TRAD, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(4, "Karmøy", "Visnes", "Arasete", T.BOLT, null, 1, "6-", "Magne-Henrik Velde", "2005-01-01", 0, 0));
//		data.add(new Data(5, "Karmøy", "Visnes", "Prosjekt BjR", T.BOLT, null, 1, "6", null, null, 0, 0));
//		data.add(new Data(6, "Karmøy", "Visnes", "Drømmen om Elin", T.BOLT, null, 1, "5+", "Bjørn Reppe", "2005-01-01", 0, 0));
//		data.add(new Data(7, "Karmøy", "Visnes", "Kalveland Boogie", T.BOLT, null, 1, "5-", "Bjørn Reppe", "2005-01-01", 0, 0));
//		data.add(new Data(8, "Karmøy", "Visnes", "Jomfruturen", T.BOLT, null, 1, "5-", "Elin Olsgård", "2006-01-01", 0, 0));
//		data.add(new Data(9, "Karmøy", "Visnes", "Navnløs 3", T.TRAD, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(10, "Karmøy", "Visnes", "Prosjekt EIO", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(11, "Karmøy", "Visnes", "Navnløs 4", T.TRAD, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(1, "Kreppene", "Kviteveggen", "Noe spenning", T.BOLT, "4bb (10m). I venstre hjørne av Kviteveggen", 1, "4", "TR", "2005-06-24", 0, 0));
//		data.add(new Data(2, "Kreppene", "Kviteveggen", "Lavspenning", T.BOLT, "4bb (10m)", 1, "6", "Håkon Hansen", "2005-06-24", 0, 0));
//		data.add(new Data(3, "Kreppene", "Kviteveggen", "Høyspenning", T.BOLT, "4bb (10m). Rett opp på huller og riss", 1, "7-", "Håkon Hansen", "2005-06-06", 0, 0));
//		data.add(new Data(4, "Kreppene", "Kviteveggen", "Pump og prakt", T.BOLT, "4bb (10m). Diagonalt mot høyre etter første bolt på Høyspenning", 1, "7", "Håkon Hansen", "2005-06-06", 0, 0));
//		data.add(new Data(5, "Kreppene", "Kviteveggen", "Kraftverk direkte", T.BOLT, "4bb (10m). Gamle, men OK bolter", 1, "5", "Odd Arne Hemmingstad", "2005-06-12", 0, 0));
//		data.add(new Data(6, "Kreppene", "Kviteveggen", "Kraftverk", T.MIXED, "10m. Til h på kiler ved tredje bolt på K-direkte", 1, "5", "Håkon Hansen", "2005-06-06", 0, 0));
//		data.add(new Data(7, "Kreppene", "Kviteveggen", "Prosjekt", T.BOLT, "10m. Ikke på kvitveggen, men helt oppe ved parkeringa", 1, "n/a", "TR", null, 0, 0));
//		data.add(new Data(8, "Kreppene", "Kviteveggen", "Haugaland kraft", T.BOLT, "5bb (12m). Parkeringsveggen", 1, "7/7+", "Håkon Hansen", "2005-06-23", 0, 0));
//		data.add(new Data(9, "Kreppene", "Kviteveggen", "Knott", T.TRAD, "10m. Bra bit til h for Haugaland kraft, \"mini-kamin\"", 1, "5+", "Håkon Hansen, TI", "2005-06-04", 0, 0));
//		data.add(new Data(1, "Kreppene", "Hovedveggen", "Angstdemper", T.BOLT, "6bb (15m). Første ruta på 9-tallet på Haugalandet", 1, "9-", "Håkon Hansen", "2002-05-29", 0, 0));
//		data.add(new Data(2, "Kreppene", "Hovedveggen", "Vival", T.BOLT, "5bb (15m). Bratt start, så dieder", 1, "7+", "ES", "2002-07-18", 0, 0));
//		data.add(new Data(3, "Kreppene", "Hovedveggen", "Andy Capp", T.TRAD, "10m, diederet", 1, "5", "Håkon Hansen", "2005-06-05", 0, 0));
//		data.add(new Data(4, "Kreppene", "Hovedveggen", "Fingrenes sjelsliv", T.MIXED, "Hele borres etter hvert", 1, "7+/8-", "Håkon Hansen", "1986-05-26", 0, 0));
//		data.add(new Data(1, "Valhest", "1. Obelix", "Sputnik", T.BOLT, "4bb (10m). Hoppstart fra stein/steiner..", 1, "8-/8", "Jon Inge Haraldseid", "2009-05-31", 0, 0));
//		data.add(new Data(2, "Valhest", "1. Obelix", "Ferdigmelding", T.BOLT, "6bb (18m). Dieder og egg til venstre for Obelix", 1, "7-", "Jon Inge Haraldseid", "2009-05-31", 0, 0));
//		data.add(new Data(3, "Valhest", "1. Obelix", "Obelix", T.TRAD, "18m. Solid klassiker sentralt opp veggen.", 1, "7+", "Anders Kindlihagen", "1998-05-16", 0, 0));
//		data.add(new Data(4, "Valhest", "1. Obelix", "Kometkameratene", T.BOLT, "6bb (18m). Eggen til høyre for Obelix", 1, "7+", "Anders Kindlihagen", "2009-05-30", 0, 0));
//		data.add(new Data(5, "Valhest", "1. Obelix", "Uten Oksygen", T.BOLT, "7bb (18m). Starter i diederet, går så raskt som mulig ut på veggen", 1, "6", "Rudi Schrøder", "2009-05-31", 0, 0));
//		data.add(new Data(6, "Valhest", "1. Obelix", "Idefix", T.TRAD, "15m", 1, "4+", null, null, 0, 0));
//		data.add(new Data(7, "Valhest", "1. Obelix", "Bennaren", T.BOLT, "4bb (12m)", 1, "5", "Rudi Schrøder", "2009-05-31", 0, 0));
//		data.add(new Data(8, "Valhest", "1. Obelix", "Svineflonså", T.BOLT, "5bb (12m). Boltet av RS. Litt rekkevidde på krukset...", 1, "7+", "Anders Kindlihagen", "2009-08-19", 0, 0));
//		data.add(new Data(9, "Valhest", "1. Obelix", "Doktor Proktor", T.TRAD, "11m, skråriss.", 1, "4", "IK", "2009-05-31", 0, 0));
//		data.add(new Data(10, "Valhest", "1. Obelix", "Trollfar", T.BOLT, "4bb (10m). boltet som barnerute", 1, "4", "IK", "2010-04-12", 0, 0));
//		data.add(new Data(11, "Valhest", "1. Obelix", "Trollungen", T.BOLT, "5bb (10m). boltet som barnerute", 1, "3+", "IK", "2010-04-11", 0, 0));
//		data.add(new Data(12, "Valhest", "1. Obelix", "Kontrollkommiteen", T.BOLT, "5bb (15m). Diederhjørne, hylle, egg.", 1, "6-", "Anders Kindlihagen", "2009-05-31", 0, 0));
//		data.add(new Data(13, "Valhest", "1. Obelix", "Prosjekt", T.BOLT, "6bb (12m). Lukket prosjekt 50 m sør for Konrollkommiteen", 1, "5", "Ingrid", null, 0, 0));
//		data.add(new Data(1, "Valhest", "2. Trubadurix", "Trubaturix", T.TRAD, "30m. Litt utfordrende i toppen? Toppfeste", 1, "5+", "Anders Kindlihagen", "1998-05-16", 0, 0));
//		data.add(new Data(2, "Valhest", "2. Trubadurix", "Flåttmyr", T.BOLT, "7bb (25m). Gåes minst mulig uten diederet...", 1, "7-", "Anders Kindlihagen", "2010-04-11", 0, 0));
//		data.add(new Data(3, "Valhest", "2. Trubadurix", "Prosjekt", T.TRAD, "22m, åpent prosjekt", 1, "5", null, null, 0, 0));
//		data.add(new Data(4, "Valhest", "2. Trubadurix", "Is i rubben", T.BOLT, "5bb (15m)", 1, "5+", "Anders Kindlihagen", "2010-04-11", 0, 0));
//		data.add(new Data(1, "Valhest", "3. Asterix", "Buksvoger", T.BOLT, "10bb (25m). Eineren i toppen taes vekk etterhvert", 1, "5", "Rudi Schrøder", "2009-08-19", 0, 0));
//		data.add(new Data(2, "Valhest", "3. Asterix", "Tafsetøsen", T.BOLT, "10bb (25m)", 1, "5", "Rudi Schrøder", "2009-05-31", 0, 0));
//		data.add(new Data(3, "Valhest", "3. Asterix", "Kjempeflaket", T.TRAD, "30m. Toppfeste sammen med \"Tafsetøsen\", ekstra toppfeste litt til høyre.", 1, "5", null, null, 0, 0));
//		data.add(new Data(4, "Valhest", "3. Asterix", "Antitrist", T.BOLT, "25m", 1, "6-", "Rudi Schrøder", "2010-04-11", 0, 0));
//		data.add(new Data(5, "Valhest", "3. Asterix", "Asterix", T.TRAD, "25m. Litt utsatt direktestart, så tyynnnt klokkerent riss. Toppfeste", 1, "7", "Anders Kindlihagen", "1999-04-26", 0, 0));
//		data.add(new Data(6, "Valhest", "3. Asterix", "Rotfast & lealaust", T.BOLT, "8bb (22m). Vegg og riss, samme toppfeste som neste rute", 1, "8-", "Anders Kindlihagen", "2009-10-02", 0, 0));
//		data.add(new Data(7, "Valhest", "3. Asterix", "Prosjekt", T.BOLT, "10bb (25m). Åpent prosjekt. EGGEN!!!", 1, "9-", null, null, 0, 0));
//		data.add(new Data(1, "Valhest", "4. Bare Blåbær", "Lukket prosjekt", T.BOLT, "Lukket prosjekt.", 1, "6", "Jan Seeman", null, 0, 0));
//		data.add(new Data(2, "Valhest", "4. Bare Blåbær", "Singel og sugen", T.BOLT, "5bb (12m)", 1, "6-", "Rudi Schrøder", "2009-10-17", 0, 0));
//		data.add(new Data(3, "Valhest", "4. Bare Blåbær", "Bare blåbær", T.BOLT, "5bb (10m). Liten pillar rett til venstre for \"Buksvoger\"", 1, "5", "Jan Seeman", "2009-08-23", 0, 0));
//		data.add(new Data(1, "Valhest", "5. Løpetid", "Løpetid", T.TRAD, "20m. Usikret start opp til risset. felles anker m \"På kryss..\"", 1, "7-", "EM", "1998-05-16", 0, 0));
//		data.add(new Data(2, "Valhest", "5. Løpetid", "Syke sjeler", T.BOLT, "7bb (18m)", 1, "8/8+", "Anders Kindlihagen", "2009-10-18", 0, 0));
//		data.add(new Data(3, "Valhest", "5. Løpetid", "På kryss og tur", T.TRAD, "25m. Start fra toppen av steinen. Toppanker.", 1, "6", "KS", "1998-05-16", 0, 0));
//		data.add(new Data(4, "Valhest", "5. Løpetid", "Prosjekt", T.BOLT, "6bb (17m). Åpent prosjekt. Venstre variant i toppen.", 1, "9-", null, null, 0, 0));
//		data.add(new Data(5, "Valhest", "5. Løpetid", "Rett opp og ned", T.BOLT, "6bb (17m). Høyre variant i toppen", 1, "8", "Anders Kindlihagen", "2010-05-08", 0, 0));
//		data.add(new Data(1, "Valhest", "6. Bergmenn", "Fittis & fettis", T.BOLT, "23m", 1, "6+", "Joel Nielsen", "2010-05-09", 0, 0));
//		data.add(new Data(2, "Valhest", "6. Bergmenn", "Full behandling", T.BOLT, "8bb (23m)", 1, "7", "Anders Kindlihagen", "2010-05-08", 0, 0));
//		data.add(new Data(3, "Valhest", "6. Bergmenn", "Tarotdronningen", T.TRAD, "19m. Tynnt sikret start", 1, "6", "Anders Kindlihagen", "2010-05-09", 0, 0));
//		data.add(new Data(4, "Valhest", "6. Bergmenn", "Global astrologi", T.TRAD, "19m. Tynnt sikret på svaet i overgang mellom riss.", 1, "6+", "Anders Kindlihagen", "2010-05-09", 0, 0));
//		data.add(new Data(5, "Valhest", "6. Bergmenn", "Bergmenn", T.BOLT, "12bb (27m). Til siste anker åpent prosjekt", 1, "7+", "Anders Kindlihagen", "2010-04-11", 0, 0));
//		data.add(new Data(6, "Valhest", "6. Bergmenn", "Prosjekt", T.TRAD, "20m. Lukket prosjekt", 1, "n/a", "Rudi Schrøder", null, 0, 0));
//		data.add(new Data(7, "Valhest", "6. Bergmenn", "Kjenslevar", T.BOLT, "6bb (18m). Svært fin rute. Solid.", 1, "7-", "Rudi Schrøder", "2010-05-16", 0, 0));
//		data.add(new Data(8, "Valhest", "6. Bergmenn", "Panserhjerte", T.BOLT, "6bb (18m). Skal rense unna litt bedre rundt einerbusken til høyre...", 1, "7-", "Anders Kindlihagen", "2010-05-08", 0, 0));
//		data.add(new Data(1, "Valhest", "7. Nøgne Ø", "IPA", T.BOLT, "4bb (12m)", 1, "6-", "Anders Kindlihagen", "2009-07-23", 0, 0));
//		data.add(new Data(2, "Valhest", "7. Nøgne Ø", "Nøgne Ø", T.BOLT, "4bb (12m)", 1, "5+/6-", "Anders Kindlihagen", "2009-07-23", 0, 0));
//		data.add(new Data(3, "Valhest", "7. Nøgne Ø", "Prosjekt", T.TRAD, "Åpent for den som vil. ", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(4, "Valhest", "7. Nøgne Ø", "??", T.TRAD, "10m. Gammelt toppfeste, lett", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(1, "Valhest", "8. Sektor X", "Mannevondt", T.BOLT, "16m. Diederet", 1, "7-", "Rudi Schrøder", "2010-05-16", 0, 0));
//		data.add(new Data(2, "Valhest", "8. Sektor X", "Prosjekt", T.TRAD, "17m. Lukket prosjekt", 1, "n/a", "Rudi Schrøder", null, 0, 0));
//		data.add(new Data(1, "Valhest", "9. Storveggen", "Aquila", T.TRAD, "70m. 2 el. 3 taulengder, mye fin klatring på 5-tallet, 2-3 punktkruks på 6-tallet, ingen bolter eller snufester.", 2, "6-", "Bjørn Reppe, Truls", "2010-05-01", 0, 0));
//		data.add(new Data(1, "Yrkje", "1. Første nedre", "Yrkjesriss", T.BOLT, "5bb (10m)", 1, "6", "MH", "2014-04-01", 0, 0));
//		data.add(new Data(2, "Yrkje", "1. Første nedre", "Yrkjesintro", T.BOLT, "5bb (10m)", 1, "6", "MH", "2014-04-01", 0, 0));
//		data.add(new Data(1, "Yrkje", "2. Første øvre", "Yrkjesangst", T.BOLT, "3bb (8m)", 1, "6", "MH", "2014-04-01", 0, 0));
//		data.add(new Data(2, "Yrkje", "2. Første øvre", "Lukket prosjekt", T.BOLT, "10m. Lukket prosjekt", 1, "7", "MH", null, 0, 0));
//		data.add(new Data(3, "Yrkje", "2. Første øvre", "Yrkjesbalanse", T.BOLT, "3bb (10m)", 1, "7-", "MH", "2014-04-01", 0, 0));
//		data.add(new Data(4, "Yrkje", "2. Første øvre", "Lov & uredd", T.BOLT, "10m", 1, "6+", "Rudi Schrøder", "2014-04-20", 0, 0));
//		data.add(new Data(5, "Yrkje", "2. Første øvre", "Krig & Fredag", T.BOLT, "10m", 1, "7-", "Rudi Schrøder", "2014-04-15", 0, 0));
//		data.add(new Data(6, "Yrkje", "2. Første øvre", "Rissprosjekt", T.TRAD, "10m. Åpent prosjekt", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(7, "Yrkje", "2. Første øvre", "Rissprosjekt", T.TRAD, "12m. Åpent prosjekt", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(8, "Yrkje", "2. Første øvre", "Forbrytelse & straffespark", T.BOLT, "12m", 1, "6-", "Rudi Schrøder", "2014-03-12", 0, 0));
//		data.add(new Data(1, "Yrkje", "3. Mellomveggene", "Verdens eldste…", T.BOLT, "10m. Den du (RS) har laget litt høyt oppe et sted….", 1, "6", "Rudi Schrøder", "2014-04-18", 0, 0));
//		data.add(new Data(1, "Yrkje", "4. Venstre for taket", "Vriompeis", T.BOLT, "4bb (10m). Lettere om man klatrer til venstre", 1, "6", "MH", "2014-04-01", 0, 0));
//		data.add(new Data(2, "Yrkje", "4. Venstre for taket", "Kåre fankernta", T.BOLT, "4bb (10m). Felles anker med 1", 1, "6+", "MH", "2014-04-01", 0, 0));
//		data.add(new Data(3, "Yrkje", "4. Venstre for taket", "Yrkjeskriminell", T.BOLT, "5bb (12m)", 1, "6", "MH", "2014-04-01", 0, 0));
//		data.add(new Data(4, "Yrkje", "4. Venstre for taket", "Yrkjeskule", T.BOLT, "4bb (10m)", 1, "6-", "MH", "2014-04-01", 0, 0));
//		data.add(new Data(5, "Yrkje", "4. Venstre for taket", "Prolapsgenerasjonen", T.BOLT, "4bb (9m). Kort svarute", 1, "7", "MH", "2014-04-18", 0, 0));
//		data.add(new Data(1, "Yrkje", "5. Taket", "Skam  topper alt", T.BOLT, "I brattveggen rett til venstre for taket", 1, "7-/7", "Rudi Schrøder", "2014-04-21", 0, 0));
//		data.add(new Data(2, "Yrkje", "5. Taket", "Venstre eggen", T.BOLT, "Lukket prosjekt", 1, "n/a", "MH", null, 0, 0));
//		data.add(new Data(3, "Yrkje", "5. Taket", "Kongelinja", T.BOLT, "25m. Lukket prosjekt", 1, "7+/8-", "MH", null, 0, 0));
//		data.add(new Data(4, "Yrkje", "5. Taket", "Sentrallinja", T.BOLT, "7-8bb (25m). Lukket prosjekt", 1, "8", "Anders Kindlihagen", null, 0, 0));
//		data.add(new Data(5, "Yrkje", "5. Taket", "Høyrekanten", T.BOLT, "7-8bb (25m). Lukket prosjekt", 1, "8", "Anders Kindlihagen", null, 0, 0));
//		data.add(new Data(1, "Yrkje", "6. Høyre for taket", "Yrkjeskadd", T.BOLT, null, 1, "6-", "Rudi Schrøder", "2014-03-13", 0, 0));
//		data.add(new Data(1, "Fiskeveggen", "Fiskeveggen", "Hå Hå", T.BOLT, "11bb (25m). Kommende klassiker.", 1, "8+", "Håkon Hansen", "2003-04-15", 0, 0));
//		data.add(new Data(2, "Fiskeveggen", "Fiskeveggen", "Gjeddeheng", T.BOLT, "11bb (26m). Fantastisk linje. Alternativ start er boltet av noen….", 1, "9-/9", "Håkon Hansen", "2003-08-17", 0, 0));
//		data.add(new Data(3, "Fiskeveggen", "Fiskeveggen", "Sei you sei me", T.BOLT, "11bb (25m). Jevn & flott klassiker.", 1, "8/8+", "Håkon Hansen", "2002-04-05", 0, 0));
//		data.add(new Data(4, "Fiskeveggen", "Fiskeveggen", "Kulturulke", T.BOLT, "10bb (25m). Klassiker med overraskelser i kø.", 1, "8-", "Anders Kindlihagen", "2002-04-06", 0, 0));
//		data.add(new Data(5, "Fiskeveggen", "Fiskeveggen", "Flakfisk", T.BOLT, "10bb (25m). Start langs fint flak", 1, "8/8+", "Anders Kindlihagen", "2002-09-15", 0, 0));
//		data.add(new Data(6, "Fiskeveggen", "Fiskeveggen", "Snøskreifare", T.BOLT, "5bb (17m). Fin rute (som kan forlenges).", 1, "6", "Siv Wikberg", "2002-09-22", 0, 0));
//		data.add(new Data(7, "Fiskeveggen", "Fiskeveggen", "Prosjekt 1", T.BOLT, "25m, bolting påbegynt…", 1, "8+", null, null, 0, 0));
//		data.add(new Data(8, "Fiskeveggen", "Fiskeveggen", "Prosjekt 2", T.BOLT, "25m, bolting påbegynt?", 1, "9-", null, null, 0, 0));
//		data.add(new Data(9, "Fiskeveggen", "Fiskeveggen", "Prosjekt 3", T.BOLT, "25m, bolting påbegynt?", 1, "8+", null, null, 0, 0));
//		data.add(new Data(10, "Fiskeveggen", "Fiskeveggen", "FLaks", T.BOLT, "8bb (18m). Nydelig gjennom ”buen”", 1, "8", "Håkon Hansen", "2002-05-12", 0, 0));
//		data.add(new Data(11, "Fiskeveggen", "Fiskeveggen", "Du store sardin", T.BOLT, "7bb (17m). Det venstre tydelige risset - må renses igjen….", 1, "8-", "Håkon Hansen", "2002-04-13", 0, 0));
//		data.add(new Data(12, "Fiskeveggen", "Fiskeveggen", "Sildajazz", T.BOLT, "6bb (18m). Superrute med litt magasug.", 1, "8", "Håkon Hansen", "2002-07-29", 0, 0));
//		data.add(new Data(13, "Fiskeveggen", "Fiskeveggen", "Haisommer", T.BOLT, "12bb (27m). Felles første bolt m. Sildajazz", 1, "8+/9-", "Håkon Hansen", "2002-09-15", 0, 0));
//		data.add(new Data(14, "Fiskeveggen", "Fiskeveggen", "Prosjekt 4", T.BOLT, "25m, risslinja", 1, "8", null, null, 0, 0));
//		data.add(new Data(15, "Fiskeveggen", "Fiskeveggen", "General krabbe", T.BOLT, "8bb (17m)", 1, "8/8+", "Håkon Hansen", "2002-07-17", 0, 0));
//		data.add(new Data(16, "Fiskeveggen", "Fiskeveggen", "Prosjekt", T.BOLT, "25m. Åpent prosjekt (boltet av HH)", 1, "9", null, null, 0, 0));
//		data.add(new Data(17, "Fiskeveggen", "Fiskeveggen", "Prosjekt - Flyndretokt", T.BOLT, "Åpent prosjekt (boltet av HH)", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(18, "Fiskeveggen", "Fiskeveggen", "Plukkfisk", T.BOLT, "11bb (23m). Felles første 5 bb med Carpe, så til ventre.", 1, "7+", "Håkon Hansen", "2002-10-08", 0, 0));
//		data.add(new Data(19, "Fiskeveggen", "Fiskeveggen", "Carpe dieder", T.BOLT, "8bb (18m). Start ved venstrevendt dieder, til ringer halvveis i veggen", 1, "7-", "Rudi Schrøder", "2002-09-15", 0, 0));
//		data.add(new Data(20, "Fiskeveggen", "Fiskeveggen", "Prosjekt Feberfiske", T.BOLT, "25m. Fortsettelsen fra Carpe", 1, "7+/8-", "Rudi Schrøder", null, 0, 0));
//		data.add(new Data(21, "Fiskeveggen", "Fiskeveggen", "Slukørret", T.BOLT, "5bb (14m). Balanse på den korte eggen", 1, "8-", "Håkon Hansen", "2002-06-02", 0, 0));
//		data.add(new Data(22, "Fiskeveggen", "Fiskeveggen", "Fiskis & Svettis", T.BOLT, null, 1, "6", "Siv Wikberg", null, 0, 0));
//		data.add(new Data(23, "Fiskeveggen", "Fiskeveggen", "Langefoss", T.BOLT, "5bb (14m). Opp til hylle, så venstrevendt dieder", 1, "6", "Håkon Hansen", "2002-07-21", 0, 0));
//		data.add(new Data(24, "Fiskeveggen", "Fiskeveggen", "Svidd fiskegrateng", T.BOLT, "7bb (17m)", 1, "6+", "Rudi Schrøder", "2002-09-14", 0, 0));
//		data.add(new Data(25, "Fiskeveggen", "Fiskeveggen", "Tørrfisk", T.BOLT, "14m", 1, "5", "Siv Wikberg", "2006-08-15", 0, 0));
//		data.add(new Data(26, "Fiskeveggen", "Fiskeveggen", "FiskeBen", T.BOLT, "8bb (18m). Boltet av Ben Velde. Lett start så videre på høyre kant av markant pillar", 1, "7-", null, null, 0, 0));
//		data.add(new Data(27, "Fiskeveggen", "Fiskeveggen", "Yngel", T.BOLT, "5bb (14m). Mye moro på få meter!", 1, "6+", "Anders Kindlihagen", "2002-09-15", 0, 0));
//		data.add(new Data(1, "Fjæra", "1. Ridderveggen", "SjimpHansen", T.BOLT, "18m", 1, "9/9+", "Håkon Hansen", "2004-07-17", 0, 0));
//		data.add(new Data(2, "Fjæra", "1. Ridderveggen", "Mols", T.BOLT, "15m", 1, "9-", "Håkon Hansen", "2006-08-17", 0, 0));
//		data.add(new Data(3, "Fjæra", "1. Ridderveggen", "Prosjekt", T.BOLT, "20m. Start i 2 over i 4 etter 4bb", 1, "9", "Håkon Hansen", null, 0, 0));
//		data.add(new Data(4, "Fjæra", "1. Ridderveggen", "Den ensomme ridder", T.BOLT, "18m. Evt 9-/9. Bratt egg", 1, "9", "Håkon Hansen", "2004-09-04", 0, 0));
//		data.add(new Data(1, "Fjæra", "2. Fugleveggen", "Bæremeis", T.BOLT, "5bb (15m)", 1, "4", "Håkon Hansen", "2002-09-14", 0, 0));
//		data.add(new Data(2, "Fjæra", "2. Fugleveggen", "Grå stær", T.BOLT, "6bb (15m)", 1, "5-", "Siv Wikberg", "2002-09-14", 0, 0));
//		data.add(new Data(3, "Fjæra", "2. Fugleveggen", "Tranedans", T.BOLT, "7bb (18m)", 1, "7-", "Håkon Hansen", "2002-05-19", 0, 0));
//		data.add(new Data(4, "Fjæra", "2. Fugleveggen", "Rugdeberget", T.BOLT, "5bb (15m)", 1, "4+", "Siv Wikberg", "2002-05-24", 0, 0));
//		data.add(new Data(5, "Fjæra", "2. Fugleveggen", "Over stork og stein", T.BOLT, "9bb (23m). Felles anker med \"Tranedans\"", 1, "6", "Håkon Hansen", "2002-05-19", 0, 0));
//		data.add(new Data(6, "Fjæra", "2. Fugleveggen", "Fuglemannen", T.BOLT, "5bb (13m). Tverrvegg", 1, "8-", "ES", "2002-09-14", 0, 0));
//		data.add(new Data(7, "Fjæra", "2. Fugleveggen", "Gribben", T.TRAD, "27m. Snufeste til høyre halvveis i diederet.", 1, "5", "TR", "2002-05-19", 0, 0));
//		data.add(new Data(1, "Fjæra", "3. Saueveggen", "Prosjekt \"Lam i hakket\"", T.MIXED, "6 taulengder. To første lengdene ferdige... (7-/8)", 6, "n/a", "Håkon Hansen, Rudi Schrøder", null, 0, 0));
//		data.add(new Data(1, "Fjæra", "4. Kuveggen", "Bjellekua", T.BOLT, "8bb (18m). Tydelig egg.", 1, "5", "TR", "2002-05-11", 0, 0));
//		data.add(new Data(2, "Fjæra", "4. Kuveggen", "Kuromperisset", T.TRAD, "20m. Jomfruturen i Kuveggen. Toppanker", 1, "5", "Rudi Schrøder", "2002-04-13", 0, 0));
//		data.add(new Data(3, "Fjæra", "4. Kuveggen", "Navnløs 1", T.BOLT, "20m", 1, "7", "Håkon Hansen", null, 0, 0));
//		data.add(new Data(4, "Fjæra", "4. Kuveggen", "Navnløs 2", T.BOLT, "22m", 1, "6", "Rudi Schrøder", null, 0, 0));
//		data.add(new Data(5, "Fjæra", "4. Kuveggen", "Insemenørene", T.BOLT, "14bb (45m)", 1, "6+", "Thomas Hahn, MH", null, 0, 0));
//		data.add(new Data(1, "Rullestad", "1. Pondus", "Cave Canem", T.BOLT, "18m. Boltet av HH", 1, "8-", "Anders Kindlihagen", "2014-05-28", 0, 0));
//		data.add(new Data(2, "Rullestad", "1. Pondus", "Pels & Poter", T.BOLT, "18m. Boltet av HH", 1, "7+", "Anders Kindlihagen", "2014-05-30", 0, 0));
//		data.add(new Data(3, "Rullestad", "1. Pondus", "Luddes beste venn", T.BOLT, "6bb (15m). Boltet av HH.", 1, "8", "Anders Kindlihagen", "2013-09-08", 0, 0));
//		data.add(new Data(4, "Rullestad", "1. Pondus", "Passopp", T.BOLT, "12m. Boltet av HH.", 1, "6-", "Håkon Hansen", "2010-07-31", 0, 0));
//		data.add(new Data(5, "Rullestad", "1. Pondus", "Aitana", T.BOLT, "18m. Boltet av HH.", 1, "8-/8", "Anders Kindlihagen, Håkon Hansen", "2011-07-10", 0, 0));
//		data.add(new Data(6, "Rullestad", "1. Pondus", "Doggystyle", T.BOLT, "7bb (18m)", 1, "7+", "Håkon Hansen", "2010-07-08", 0, 0));
//		data.add(new Data(7, "Rullestad", "1. Pondus", "Bikkja i baken", T.BOLT, "7bb (18m)", 1, "6+", "Anders Kindlihagen", "2010-07-10", 0, 0));
//		data.add(new Data(8, "Rullestad", "1. Pondus", "Hangdog", T.BOLT, "8bb (18m)", 1, "6", "Rudi Schrøder", "2010-07-08", 0, 0));
//		data.add(new Data(9, "Rullestad", "1. Pondus", "Oro jaska beana", T.BOLT, "7bb (16m)", 1, "7-", "Rudi Schrøder", "2010-07-08", 0, 0));
//		data.add(new Data(1, "Rullestad", "2. Flåttveggen", "Åpent prosjekt - Salamanderkrigen", T.BOLT, "20m. Boltet av HH. Riss i toppen.", 1, "8+", null, null, 0, 0));
//		data.add(new Data(2, "Rullestad", "2. Flåttveggen", "Markant", T.BOLT, "8bb (18m). Boltet av HH. Den markerte kanten. Svært fin!!", 1, "7+/8-", "Joel Nielsen", "2013-09-08", 0, 0));
//		data.add(new Data(3, "Rullestad", "2. Flåttveggen", "Prosjekt???", T.BOLT, "Eggen - boltes om litt…", 1, "9", null, null, 0, 0));
//		data.add(new Data(4, "Rullestad", "2. Flåttveggen", "Kjempeflått", T.BOLT, "20m. Ringanker midt i veggen, forlengelse boltet, men ikke gått", 1, "8+", "Alv Borge", "2010-07-01", 0, 0));
//		data.add(new Data(5, "Rullestad", "2. Flåttveggen", "Råflott", T.BOLT, "12bb (27m). Klassiker opp den diagonale sentrallinja", 1, "7/7+", "Håkon Hansen, Gareth Harding", "2010-05-01", 0, 0));
//		data.add(new Data(6, "Rullestad", "2. Flåttveggen", "Flåtte dama", T.BOLT, "12bb (27m)", 1, "5+", "Håkon Hansen, Gareth Harding", "2010-05-01", 0, 0));
//		data.add(new Data(7, "Rullestad", "2. Flåttveggen", "Aeroflått", T.BOLT, "12bb (27m). Den slake fine risslinja", 1, "5+", "Håkon Hansen, Gareth Harding", "2010-05-01", 0, 0));
//		data.add(new Data(8, "Rullestad", "2. Flåttveggen", "Nattergalen", T.BOLT, "18m. Felles start m Aeroflått, så til høyre", 1, "5+", "Håkon Hansen", "2005-07-03", 0, 0));
//		data.add(new Data(9, "Rullestad", "2. Flåttveggen", "Othello", T.BOLT, "15m. felles anker med foregående", 1, "6", "Annmari Vitikainen", "2005-07-03", 0, 0));
//		data.add(new Data(10, "Rullestad", "2. Flåttveggen", "Skorsteinsfeieren", T.BOLT, "20m. Opplagt linje langs minidieder - solid!", 1, "7-", "Håkon Hansen", "2005-07-03", 0, 0));
//		data.add(new Data(11, "Rullestad", "2. Flåttveggen", "Røverbrødrene", T.BOLT, "20m", 1, "6", "Håkon Hansen", "2005-07-04", 0, 0));
//		data.add(new Data(12, "Rullestad", "2. Flåttveggen", "Isbjørnen", T.BOLT, "18m", 1, "6-", "Håkon Hansen", "2012-08-15", 0, 0));
//		data.add(new Data(13, "Rullestad", "2. Flåttveggen", "Paradisfuglen", T.BOLT, "18m. Rett opp til ankeret fra siste bolt gir ca 7-/7", 1, "6+", "Håkon Hansen", "2012-08-15", 0, 0));
//		data.add(new Data(14, "Rullestad", "2. Flåttveggen", "Pyramidebyggerne", T.BOLT, "18m", 1, "6+", "Håkon Hansen", "2012-08-15", 0, 0));
//		data.add(new Data(15, "Rullestad", "2. Flåttveggen", "Åpent prosjekt", T.BOLT, "18m. Boltet av HH.", 1, "8", null, null, 0, 0));
//		data.add(new Data(16, "Rullestad", "2. Flåttveggen", "Sinbad", T.BOLT, "6bb (18m). Boltet av HH. Ta med stålbørste og gjør ruta til en tre-stjerners...", 1, "7", "Joel Nielsen", "2013-09-08", 0, 0));
//		data.add(new Data(17, "Rullestad", "2. Flåttveggen", "Flått1", T.BOLT, "20m. Denne og de to neste går på det slake svaet øverst - som lettest nåes fra den gamle skolen", 1, "4", "Håkon Hansen, Gareth Harding", "2010-05-01", 0, 0));
//		data.add(new Data(18, "Rullestad", "2. Flåttveggen", "Flått2", T.BOLT, "20m", 1, "4", "Håkon Hansen, Gareth Harding", "2010-05-01", 0, 0));
//		data.add(new Data(19, "Rullestad", "2. Flåttveggen", "Flått3", T.BOLT, "20m", 1, "4", "Håkon Hansen, Gareth Harding", "2010-05-01", 0, 0));
//		data.add(new Data(1, "Rullestad", "3. Fosseveggen", "Vaktmesteren", T.BOLT, "9bb (20m). Super rute", 1, "7-", "Håkon Hansen", "2013-05-01", 0, 0));
//		data.add(new Data(2, "Rullestad", "3. Fosseveggen", "Flintstone", T.BOLT, "9bb (20m). Felles start + felles anker med Alkejægeren", 1, "7-", "Håkon Hansen", "2013-05-01", 0, 0));
//		data.add(new Data(3, "Rullestad", "3. Fosseveggen", "Alkejægeren", T.BOLT, "10bb (20m). Fin rute!", 1, "7+", "Håkon Hansen", "2013-05-01", 0, 0));
//		data.add(new Data(4, "Rullestad", "3. Fosseveggen", "Gullalder", T.BOLT, "6bb (13m)", 1, "5", "Håkon Hansen", "2013-05-01", 0, 0));
//		data.add(new Data(5, "Rullestad", "3. Fosseveggen", "Fossekallen", T.BOLT, "6bb (15m). Vertikal fin vegg", 1, "6", "Håkon Hansen", "2012-07-01", 0, 0));
//		data.add(new Data(6, "Rullestad", "3. Fosseveggen", "Fotsoppen", T.BOLT, null, 1, "6-", "Håkon Hansen", "2013-05-01", 0, 0));
//		data.add(new Data(7, "Rullestad", "3. Fosseveggen", "Fleinsoppen", T.BOLT, "6bb (15m). Slopey på store formasjoner...", 1, "7-", "Håkon Hansen", "2012-07-01", 0, 0));
//		data.add(new Data(8, "Rullestad", "3. Fosseveggen", "Kekkonen", T.BOLT, "6bb (15m). Fin egg og vegg til høyre", 1, "7", "Håkon Hansen", "2012-07-01", 0, 0));
//		data.add(new Data(9, "Rullestad", "3. Fosseveggen", "Mayday", T.BOLT, "12m", 1, "6", "Håkon Hansen", "2013-06-01", 0, 0));
//		data.add(new Data(10, "Rullestad", "3. Fosseveggen", "Fortuna", T.BOLT, "12m", 1, "6", "Håkon Hansen", "2013-06-01", 0, 0));
//		data.add(new Data(1, "Rullestad", "4-1. Katteveggen", "Prosjekt 1", T.BOLT, null, 1, "8", "Håkon Hansen", null, 0, 0));
//		data.add(new Data(2, "Rullestad", "4-1. Katteveggen", "Prosjekt 2", T.BOLT, null, 1, "7+", "Håkon Hansen", null, 0, 0));
//		data.add(new Data(3, "Rullestad", "4-1. Katteveggen", "Prosjekt 3", T.BOLT, null, 1, "n/a", "Håkon Hansen", null, 0, 0));
//		data.add(new Data(4, "Rullestad", "4-1. Katteveggen", "Prosjekt 4", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(5, "Rullestad", "4-1. Katteveggen", "Kattepiss", T.BOLT, null, 1, "7-", "Håkon Hansen", "2005-07-03", 0, 0));
//		data.add(new Data(6, "Rullestad", "4-1. Katteveggen", "Kattastrofe", T.BOLT, null, 1, "6-", "Håkon Hansen", "2005-07-03", 0, 0));
//		data.add(new Data(7, "Rullestad", "4-1. Katteveggen", "Prosjekt 5", T.BOLT, null, 1, "n/a", "Håkon Hansen", null, 0, 0));
//		data.add(new Data(8, "Rullestad", "4-1. Katteveggen", "Katzenjammer", T.BOLT, "6bb. Feltets fineste. markert riss", 1, "7/7+", "Håkon Hansen", "2005-07-03", 0, 0));
//		data.add(new Data(9, "Rullestad", "4-1. Katteveggen", "Kattmandu", T.BOLT, "6bb. Felles start m Katzenjammer", 1, "7+/8-", "Håkon Hansen", "2013-05-25", 0, 0));
//		data.add(new Data(10, "Rullestad", "4-1. Katteveggen", "Muskatt", T.BOLT, "6bb", 1, "7-", "Håkon Hansen", "2013-05-25", 0, 0));
//		data.add(new Data(11, "Rullestad", "4-1. Katteveggen", "Festus Gabriel", T.BOLT, "6bb", 1, "6-", "Håkon Hansen", "2005-07-03", 0, 0));
//		data.add(new Data(12, "Rullestad", "4-1. Katteveggen", "Hoffe", T.BOLT, "6bb. Felles anker med Festus", 1, "6-", "Håkon Hansen", "2005-07-03", 0, 0));
//		data.add(new Data(13, "Rullestad", "4-1. Katteveggen", "Kattegatt", T.BOLT, "Gjennom hullet og til høyre", 1, "6+", "Håkon Hansen", "2005-07-03", 0, 0));
//		data.add(new Data(14, "Rullestad", "4-1. Katteveggen", "Katt og hund", T.BOLT, "Gjennom hullet og til venstre", 1, "6", "Håkon Hansen", "2005-07-03", 0, 0));
//		data.add(new Data(1, "Rullestad", "4-2. Tre", "Lumberjack", T.TRAD, "20m. Toppfeste kommer...", 1, "7", "Joel Nielsen", "2012-05-12", 0, 0));
//		data.add(new Data(1, "Rullestad", "5. Smielva", "God elg", T.BOLT, "7bb (15m)", 1, "7/7+", "Anders Kindlihagen, Håkon Hansen", "2005-07-04", 0, 0));
//		data.add(new Data(2, "Rullestad", "5. Smielva", "Elgbrødrene", T.BOLT, "7bb (15m)", 1, "7+", "Anders Kindlihagen", "2005-07-04", 0, 0));
//		data.add(new Data(1, "Rullestad", "6. Grevling", "Grevling i taket", T.BOLT, "10bb (23m). Slak fin linje", 1, "3+", "Håkon Hansen", "2010-07-25", 0, 0));
//		data.add(new Data(1, "Rullestad", "7. Hjort", "Hjort i buksa", T.BOLT, "9bb (17m)", 1, "4", "Håkon Hansen, Egil Bergersen, Kine Valen", "2010-07-13", 0, 0));
//		data.add(new Data(2, "Rullestad", "7. Hjort", "Uthjort", T.BOLT, "9bb (17m)", 1, "4", "Håkon Hansen, Egil Bergersen, Kine Valen", "2010-07-13", 0, 0));
//		data.add(new Data(3, "Rullestad", "7. Hjort", "Hjortejegeren", T.BOLT, "9bb (17m)", 1, "4", "Håkon Hansen, Egil Bergersen, Kine Valen", "2010-07-13", 0, 0));
//		data.add(new Data(1, "Rullestadjuvet", "Hest", "Hestekraft", T.BOLT, "15m", 1, "8+", "Håkon Hansen", "2005-07-03", 0, 0));
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