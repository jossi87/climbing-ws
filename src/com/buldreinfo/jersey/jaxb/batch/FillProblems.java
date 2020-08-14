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
//	private final static int REGION_ID = 14;
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
//		data.add(new Data(1, "Strøm", "Sandmælen Come back", "Tåstu", T.BOLT, null, 1, "6c", "Petter S. Kristensen", "01-01-2012", 0, 0));
//		data.add(new Data(2, "Strøm", "Sandmælen Come back", "Posible nick", T.BOLT, null, 1, "6a", "Anna Gärdsmo", "01-01-2012", 0, 0));
//		data.add(new Data(3, "Strøm", "Sandmælen Come back", "Katrins first", T.BOLT, null, 1, "6c+", "Katrin Amann", "01-01-2012", 0, 0));
//		data.add(new Data(4, "Strøm", "Sandmælen Come back", "Saft Kraft", T.BOLT, null, 1, "6b+", "Torkil Dyb Remøy", "01-01-2012", 0, 0));
//		data.add(new Data(5, "Strøm", "Sandmælen Come back", "Come back", T.BOLT, null, 1, "6b+", "Magnhild Østi", "01-01-2012", 0, 0));
//		data.add(new Data(6, "Strøm", "Sandmælen Come back", "Uman River", T.BOLT, null, 1, "6a+", "Petra Rantatab", "01-01-2012", 0, 0));
//		data.add(new Data(7, "Strøm", "Sandmælen Come back", "Big ass", T.BOLT, null, 1, "6a", "Synneva Storaas", "01-01-2012", 0, 0));
//		data.add(new Data(8, "Strøm", "Sandmælen Come back", "Olve ruta", T.BOLT, null, 1, "5a", "Thomas Flagelien", "01-01-2012", 0, 0));
//		data.add(new Data(9, "Strøm", "Sandmælen Come back", "Gaffre", T.BOLT, null, 1, "6a", "Sindre Vastenhouw Strøm", "01-01-2014", 0, 0));
//		data.add(new Data(10, "Strøm", "Sandmælen Come back", "Surstraumeningen", T.BOLT, null, 1, "6a", "Henrik Strøm", "01-01-2014", 0, 0));
//		data.add(new Data(11, "Strøm", "Sandmælen Come back", "selfie på toppen", T.BOLT, null, 1, "5b", "Erlend Waldersløw Strøm", "01-01-2014", 0, 0));
//		data.add(new Data(12, "Strøm", "Sandmælen Come back", "Comme unain France", T.BOLT, null, 1, "5a", "Sandine Pic", "01-01-2012", 0, 0));
//		data.add(new Data(13, "Strøm", "Sandmælen Come back", "FinForEnFærsking", T.BOLT, null, 1, "4c", "Kristina Torsteinsen", "01-01-2014", 0, 0));
//		data.add(new Data(14, "Strøm", "Sandmælen Come back", "Glættis", T.BOLT, null, 1, "5a", "Kristina Torsteinsen", "01-01-2014", 0, 0));
//		data.add(new Data(15, "Strøm", "Sandmælen Come back", "I Love smegma", T.BOLT, null, 1, "4c", "Baston D", "01-01-2012", 0, 0));
//		data.add(new Data(16, "Strøm", "Sandmælen Come back", "Slyngelen", T.BOLT, null, 1, "4c", "Caroline Zakariassen Reinvik", "01-01-2015", 0, 0));
//		data.add(new Data(17, "Strøm", "Sandmælen Come back", "Cafemn", T.BOLT, null, 1, "4b", null, "01-01-2012", 0, 0));
//		data.add(new Data(18, "Strøm", "Sandmælen Come back", "Cafeemn", T.BOLT, null, 1, "4c", null, "01-01-2012", 0, 0));
//		data.add(new Data(19, "Strøm", "Sandmælen Come back", "Navnløs", T.BOLT, null, 1, "6a", "Erlend Waldersløw Strøm", "01-01-2016", 0, 0));
//		data.add(new Data(1, "Strøm", "Sandmælen Flach on", "Mine hender på ditt fjell", T.BOLT, null, 1, "7b", "Runar Carlsen", "01-01-2012", 0, 0));
//		data.add(new Data(2, "Strøm", "Sandmælen Flach on", "Svenneprøven", T.BOLT, null, 1, "7b", "Runar Carlsen", "01-01-2012", 0, 0));
//		data.add(new Data(3, "Strøm", "Sandmælen Flach on", "I Siget", T.BOLT, null, 1, "6a+", "Eline Storm", "01-01-2012", 0, 0));
//		data.add(new Data(4, "Strøm", "Sandmælen Flach on", "Trolllollol", T.BOLT, null, 1, "6b+", "Alexander Rydén", "01-01-2012", 0, 0));
//		data.add(new Data(5, "Strøm", "Sandmælen Flach on", "Au Le Coq", T.BOLT, null, 1, "6b+", "Einar Sulheim", "01-01-2012", 0, 0));
//		data.add(new Data(6, "Strøm", "Sandmælen Flach on", "På Jakt i kvadraten", T.BOLT, null, 1, "7b", "Runar Carlsen", "01-01-2012", 0, 0));
//		data.add(new Data(7, "Strøm", "Sandmælen Flach on", "Heman hunters", T.BOLT, null, 1, "7b", "Nanna Sødrin", "01-01-2012", 0, 0));
//		data.add(new Data(8, "Strøm", "Sandmælen Flach on", "Crawling Puman ", T.BOLT, null, 1, "6c+", "Hannes Puman", "01-01-2012", 0, 0));
//		data.add(new Data(9, "Strøm", "Sandmælen Flach on", "Tuvaplatået", T.BOLT, null, 1, "6b+", "Arve Stavø", "01-01-2012", 0, 0));
//		data.add(new Data(10, "Strøm", "Sandmælen Flach on", "Prosjekt", T.BOLT, null, 1, "7c", null, null, 0, 0));
//		data.add(new Data(11, "Strøm", "Sandmælen Flach on", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(12, "Strøm", "Sandmælen Flach on", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(13, "Strøm", "Sandmælen Flach on", "Flashon", T.BOLT, null, 1, "6b", "Anna Gärdsmo", "01-01-2012", 0, 0));
//		data.add(new Data(14, "Strøm", "Sandmælen Flach on", "Fredagsmys", T.BOLT, null, 1, "7a", "Alexander Rydén", "01-01-2012", 0, 0));
//		data.add(new Data(15, "Strøm", "Sandmælen Flach on", "Njutåger", T.BOLT, null, 1, "7b", "Geir Søderin", "01-01-2012", 0, 0));
//		data.add(new Data(1, "Strøm", "Sandmælen Wonderboy", "Wonderboy", T.BOLT, null, 1, "7a+", "Runar Carlsen", "01-01-2012", 0, 0));
//		data.add(new Data(2, "Strøm", "Sandmælen Wonderboy", "Open your eyes", T.BOLT, null, 1, "8c", "Adam Ondra", "01-01-2016", 0, 0));
//		data.add(new Data(3, "Strøm", "Sandmælen Wonderboy", "Mooseland", T.BOLT, null, 1, "8a+", "Michael Gunsilius", "01-01-2017", 0, 0));
//		data.add(new Data(4, "Strøm", "Sandmælen Wonderboy", "Tallefjant", T.BOLT, null, 1, "7b+", "Geir Søderin", "01-01-2012", 0, 0));
//		data.add(new Data(5, "Strøm", "Sandmælen Wonderboy", "NKF´s 20 års jubileum", T.BOLT, null, 1, "6c+", "Ole Karsten Birkeland", "01-01-2012", 0, 0));
//		data.add(new Data(6, "Strøm", "Sandmælen Wonderboy", "Verdens Største undercling", T.BOLT, null, 1, "6b+", "Runar Carlsen", "01-01-2012", 0, 0));
//		data.add(new Data(7, "Strøm", "Sandmælen Wonderboy", "The tiki taka slab", T.BOLT, null, 1, "5b", "Thomas Rieger", "01-01-2012", 0, 0));
//		data.add(new Data(8, "Strøm", "Sandmælen Wonderboy", "Emmafina är finast ", T.BOLT, null, 1, "4c", "Franz Schiassi", "01-01-2012", 0, 0));
//		data.add(new Data(9, "Strøm", "Sandmælen Wonderboy", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(10, "Strøm", "Sandmælen Wonderboy", "Gradskjærringa på Strøm", T.BOLT, null, 1, "6a", "Line Hestnes", "01-01-2012", 0, 0));
//		data.add(new Data(11, "Strøm", "Sandmælen Wonderboy", "Ende på Knuten", T.BOLT, null, 1, "5c", "Geir Hugo Hansen", "01-01-2012", 0, 0));
//		data.add(new Data(12, "Strøm", "Sandmælen Wonderboy", "Oda forever", T.BOLT, null, 1, "6a", "Tor Røseth", "01-01-2012", 0, 0));
//		data.add(new Data(13, "Strøm", "Sandmælen Wonderboy", "Safari", T.BOLT, null, 1, "5b", "Geir Hugo Hansen", "01-01-2012", 0, 0));
//		data.add(new Data(1, "Strøm", "Sandmælen Supernova", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Strøm", "Sandmælen Supernova", "Erlend-diederet", T.BOLT, null, 1, "6b+", "Arve Stavø", "01-01-2012", 0, 0));
//		data.add(new Data(3, "Strøm", "Sandmælen Supernova", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(4, "Strøm", "Sandmælen Supernova", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(5, "Strøm", "Sandmælen Supernova", "Niens nyp", T.BOLT, null, 1, "6c", "Gustav Boman", "01-01-2012", 0, 0));
//		data.add(new Data(6, "Strøm", "Sandmælen Supernova", "No hands", T.BOLT, null, 1, "6c", "Hanna Larsson", "01-01-2012", 0, 0));
//		data.add(new Data(7, "Strøm", "Sandmælen Supernova", "Min første gang", T.BOLT, null, 1, "6a", "D.weiss/Hanne Solskinnsbakk", "01-01-2012", 0, 0));
//		data.add(new Data(8, "Strøm", "Sandmælen Supernova", "Jeg går ned på deg", T.BOLT, null, 1, "5b", "D.weiss/Hanne Solskinnsbakk", "01-01-2012", 0, 0));
//		data.add(new Data(9, "Strøm", "Sandmælen Supernova", "Navnløs", T.BOLT, null, 1, "5a", "Dag Stvan", "01-01-2012", 0, 0));
//		data.add(new Data(10, "Strøm", "Sandmælen Supernova", "Gi slakk", T.BOLT, null, 1, "4b", "Kristian Sagmo", "01-01-2012", 0, 0));
//		data.add(new Data(11, "Strøm", "Sandmælen Supernova", "I Rampas makt", T.BOLT, null, 1, "7b+", "Runar Carlsen", "01-01-2012", 0, 0));
//		data.add(new Data(12, "Strøm", "Sandmælen Supernova", "Prosjekt", T.TRAD, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(13, "Strøm", "Sandmælen Supernova", "Supermann", T.BOLT, null, 1, "7b", "Nils Rune Birkeland", "01-01-2012", 0, 0));
//		data.add(new Data(14, "Strøm", "Sandmælen Supernova", "Superrnova", T.BOLT, null, 1, "6c", "Runar Carlsen", "01-01-2012", 0, 0));
//		data.add(new Data(15, "Strøm", "Sandmælen Supernova", "Arvesvaet", T.TRAD, null, 1, "5b", "Arve Stavø", "01-01-2012", 0, 0));
//		data.add(new Data(16, "Strøm", "Sandmælen Supernova", "Jord og mold", T.TRAD, null, 1, "4b", "Erlend Ilstad/Arve Stavø", "01-01-2012", 0, 0));
//		data.add(new Data(17, "Strøm", "Sandmælen Supernova", "Prosjekt", T.TRAD, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(18, "Strøm", "Sandmælen Supernova", "Prosjekt", T.TRAD, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(19, "Strøm", "Sandmælen Supernova", "Gladgutten", T.BOLT, null, 1, "6c", "Øystein M.K. Johnsen", "01-01-2012", 0, 0));
//		data.add(new Data(20, "Strøm", "Sandmælen Supernova", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(21, "Strøm", "Sandmælen Supernova", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(22, "Strøm", "Sandmælen Supernova", "Perfect moment", T.BOLT, null, 1, "6c", "Runar Carlsen", "01-01-2012", 0, 0));
//		data.add(new Data(23, "Strøm", "Sandmælen Supernova", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(24, "Strøm", "Sandmælen Supernova", "Pure Love", T.BOLT, null, 1, "6c", "Runar Carlsen", "01-01-2012", 0, 0));
//		data.add(new Data(25, "Strøm", "Sandmælen Supernova", "Averida Brasil", T.BOLT, null, 1, "6a+", "Geir Inge Sandnes", "01-01-2012", 0, 0));
//		data.add(new Data(1, "Strøm", "Einvikfjellet Venstre", "Fluenes herre", T.BOLT, null, 1, "6a", "Magnus Jahre", "01-01-2012", 0, 0));
//		data.add(new Data(2, "Strøm", "Einvikfjellet Venstre", "I des", T.BOLT, null, 1, "5b", "Jenny Johnsson", "01-01-2012", 0, 0));
//		data.add(new Data(3, "Strøm", "Einvikfjellet Venstre", "Svake mennesker", T.BOLT, null, 1, "5b", "Anne Jahre", "01-01-2012", 0, 0));
//		data.add(new Data(4, "Strøm", "Einvikfjellet Venstre", "A muerte", T.BOLT, null, 1, "6b", "Anne Jahre", "01-01-2012", 0, 0));
//		data.add(new Data(5, "Strøm", "Einvikfjellet Venstre", "Schwalbennest", T.BOLT, null, 1, "6a+", "Thomas Rieger", "01-01-2012", 0, 0));
//		data.add(new Data(6, "Strøm", "Einvikfjellet Venstre", "Trivelige trøndelag", T.BOLT, null, 1, "6b+", "Magnus Jahre", "01-01-2012", 0, 0));
//		data.add(new Data(7, "Strøm", "Einvikfjellet Venstre", "Juster paradis", T.BOLT, null, 1, "6b", "Børge Nordland", "01-01-2012", 0, 0));
//		data.add(new Data(8, "Strøm", "Einvikfjellet Venstre", "Navnløs", T.BOLT, null, 1, "6a+", "Dag Stwan", "01-01-2012", 0, 0));
//		data.add(new Data(9, "Strøm", "Einvikfjellet Venstre", "Mindfulnes", T.BOLT, null, 1, "6b+", "Daniel Weiss", "01-01-2012", 0, 0));
//		data.add(new Data(10, "Strøm", "Einvikfjellet Venstre", "Det...på laksen", T.BOLT, null, 1, "6a", "Erik Smedsrød", "01-01-2012", 0, 0));
//		data.add(new Data(11, "Strøm", "Einvikfjellet Venstre", "Gifrast", T.BOLT, null, 1, "6b", "Thomas Rieger", "01-01-2012", 0, 0));
//		data.add(new Data(12, "Strøm", "Einvikfjellet Venstre", "Assbear", T.BOLT, null, 1, "6b", "Asbjørn Rekkebo", "01-01-2012", 0, 0));
//		data.add(new Data(13, "Strøm", "Einvikfjellet Venstre", "Stonehogger", T.BOLT, null, 1, "5b", "Petter S. Kristensen", "01-01-2012", 0, 0));
//		data.add(new Data(14, "Strøm", "Einvikfjellet Venstre", "Lonely fly", T.BOLT, null, 1, "5a", "Petter S. Kristensen", "01-01-2012", 0, 0));
//		data.add(new Data(1, "Strøm", "Einvikfjellet Midtre", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Strøm", "Einvikfjellet Midtre", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(3, "Strøm", "Einvikfjellet Midtre", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(4, "Strøm", "Einvikfjellet Midtre", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(5, "Strøm", "Einvikfjellet Midtre", "Kjepp i Buksa", T.BOLT, null, 1, "6a+", "Stig A.Finvold", "01-01-2012", 0, 0));
//		data.add(new Data(1, "Strøm", "Einvikfjellet Høyre", "Hybris", T.BOLT, null, 1, "6a", "Torkil Dyb Remøy", "01-01-2012", 0, 0));
//		data.add(new Data(2, "Strøm", "Einvikfjellet Høyre", "Prosjekt", T.BOLT, null, 1, "n/a", "Anne Jahre", null, 0, 0));
//		data.add(new Data(3, "Strøm", "Einvikfjellet Høyre", "Oppvarmingssvaet", T.BOLT, null, 1, "6c", "Magnus Jahre", "01-01-2012", 0, 0));
//		data.add(new Data(4, "Strøm", "Einvikfjellet Høyre", "Time Travler", T.BOLT, null, 1, "7b", "Runar Carlsen", "01-01-2015", 0, 0));
//		data.add(new Data(5, "Strøm", "Einvikfjellet Høyre", "Lunsj", T.BOLT, null, 1, "6b", "Torkil Dyb Remøy", "01-01-2012", 0, 0));
//		data.add(new Data(6, "Strøm", "Einvikfjellet Høyre", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(7, "Strøm", "Einvikfjellet Høyre", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(8, "Strøm", "Einvikfjellet Høyre", "Veldig lett", T.BOLT, null, 1, "6b+", "Arve Stavø", "01-01-2012", 0, 0));
//		data.add(new Data(9, "Strøm", "Einvikfjellet Høyre", "Kvæss", T.BOLT, null, 1, "6b+", "Kristian Sagmo", "01-01-2012", 0, 0));
//		data.add(new Data(10, "Strøm", "Einvikfjellet Høyre", "School is out for the wizard", T.BOLT, null, 1, "6a+", "Franz Schiassi", "01-01-2012", 0, 0));
//		data.add(new Data(1, "Strøm", "Hanshelleren Hans", "Joris", T.BOLT, null, 1, "6a", "J. Flueck", "01-01-2016", 0, 0));
//		data.add(new Data(2, "Strøm", "Hanshelleren Hans", "Pelle", T.BOLT, null, 1, "6b", "Chris Frick", "01-01-2016", 0, 0));
//		data.add(new Data(3, "Strøm", "Hanshelleren Hans", "Limpa", T.BOLT, null, 1, "5c", "Alexander Rydén", "01-01-2013", 0, 0));
//		data.add(new Data(4, "Strøm", "Hanshelleren Hans", "Holy Friction", T.BOLT, null, 1, "6c", "Igor Bogonandor", "01-01-2012", 0, 0));
//		data.add(new Data(5, "Strøm", "Hanshelleren Hans", "Alfa Krull", T.BOLT, "Noen kvasse kanter/Watch the rope on sharp edges.", 1, "6b", "Alexander Rydén", "01-01-2012", 0, 0));
//		data.add(new Data(6, "Strøm", "Hanshelleren Hans", "Hans", T.BOLT, null, 1, "7b", "Øystein M.K. Johnsen", "01-01-2011", 0, 0));
//		data.add(new Data(7, "Strøm", "Hanshelleren Hans", "Aftenstjernen", T.BOLT, null, 1, "7b", "Chris Frick", "01-01-2013", 0, 0));
//		data.add(new Data(8, "Strøm", "Hanshelleren Hans", "Morgengaven", T.BOLT, null, 1, "7a+", "Chris Frick", "01-01-2013", 0, 0));
//		data.add(new Data(9, "Strøm", "Hanshelleren Hans", "Chossploring", T.BOLT, null, 1, "8b", "Adam Ondra", "01-01-2014", 0, 0));
//		data.add(new Data(10, "Strøm", "Hanshelleren Hans", "Navnløs", T.BOLT, null, 1, "7b+", "n/a", null, 0, 0));
//		data.add(new Data(11, "Strøm", "Hanshelleren Hans", "Kyrgizstan", T.BOLT, null, 1, "8a+", "n/a", null, 0, 0));
//		data.add(new Data(12, "Strøm", "Hanshelleren Hans", "Steinapen", T.BOLT, null, 1, "7a", "Chris Frick", "01-01-2014", 0, 0));
//		data.add(new Data(13, "Strøm", "Hanshelleren Hans", "Steinapen L2", T.BOLT, null, 1, "8b", "Adam Ondra", "01-01-2013", 0, 0));
//		data.add(new Data(14, "Strøm", "Hanshelleren Hans", "De profundis", T.BOLT, null, 1, "7a", "Andreas Øygard", "01-01-2016", 0, 0));
//		data.add(new Data(15, "Strøm", "Hanshelleren Hans", "Akvarell", T.BOLT, null, 1, "6b+", "Chris Frick", "01-01-2012", 0, 0));
//		data.add(new Data(16, "Strøm", "Hanshelleren Hans", "Trykkokeren", T.BOLT, null, 1, "6a", "Øystein M.K. Johnsen", "01-01-2014", 0, 0));
//		data.add(new Data(17, "Strøm", "Hanshelleren Hans", "Norsk Strikk", T.BOLT, null, 1, "7c+", "Chris Frick", "01-01-2012", 0, 0));
//		data.add(new Data(18, "Strøm", "Hanshelleren Hans", "Lykkelige Sønner", T.BOLT, null, 1, "6a+", "Chris Frick/J.Flueck", "01-01-2014", 0, 0));
//		data.add(new Data(19, "Strøm", "Hanshelleren Hans", "Ingwe", T.BOLT, null, 1, "7a", "Inka Matouskova", "01-01-2016", 0, 0));
//		data.add(new Data(20, "Strøm", "Hanshelleren Hans", "Rosens innerste sjel", T.BOLT, null, 1, "6b+", "Runar Carlsen", "01-01-2012", 0, 0));
//		data.add(new Data(21, "Strøm", "Hanshelleren Hans", "Mindstalker", T.BOLT, null, 1, "7b", "Runar Carlsen", "01-01-2012", 0, 0));
//		data.add(new Data(22, "Strøm", "Hanshelleren Hans", "Espen Askeladd søker lykken", T.BOLT, null, 1, "7a+", "Chris Frick", "01-01-2012", 0, 0));
//		data.add(new Data(23, "Strøm", "Hanshelleren Hans", "Trang Fødsel", T.TRAD, null, 1, "6a", "Vegard Fluge Samuelsen/Øyvind Amundsgård", "01-01-1996", 0, 0));
//		data.add(new Data(24, "Strøm", "Hanshelleren Hans", "Tusen Takk", T.TRAD, "Samme start som Trang Fødsel,men så til høyre. ", 1, "6b", "Thomas Vekne/Erik Aalde", "01-01-1996", 0, 0));
//		data.add(new Data(25, "Strøm", "Hanshelleren Hans", "Gulrot tyven", T.BOLT, null, 1, "6b+", "Andreas Klarstrøm", "01-01-1996", 0, 0));
//		data.add(new Data(26, "Strøm", "Hanshelleren Hans", "Mega gulrot tyven", T.BOLT, null, 1, "7a", "Elliot Ashe", "01-01-2014", 0, 0));
//		data.add(new Data(1, "Strøm", "Hanshelleren Kneopatra", "Tungt møblement", T.BOLT, "Klassikeren! Like til venstre for det store henget.", 1, "7a+", "Eivind W. Nagell", "01-01-2000", 0, 0));
//		data.add(new Data(2, "Strøm", "Hanshelleren Kneopatra", "Syvsover", T.BOLT, null, 1, "7b", "Chris Frick", "01-01-2014", 0, 0));
//		data.add(new Data(3, "Strøm", "Hanshelleren Kneopatra", "Steini L1", T.BOLT, null, 1, "8a", "Gudmund Grønhaug", "01-01-2012", 0, 0));
//		data.add(new Data(4, "Strøm", "Hanshelleren Kneopatra", "Steini L2", T.BOLT, null, 1, "8a+", "Adam Ondra", "01-01-2012", 0, 0));
//		data.add(new Data(5, "Strøm", "Hanshelleren Kneopatra", "Arven etter hulemannen", T.BOLT, null, 1, "8a", "Chris Frick", "01-01-2013", 0, 0));
//		data.add(new Data(6, "Strøm", "Hanshelleren Kneopatra", "Frigg L1", T.BOLT, null, 1, "7b+", "Chris Frick", "01-01-2013", 0, 0));
//		data.add(new Data(7, "Strøm", "Hanshelleren Kneopatra", "Frigg L2", T.BOLT, null, 1, "8a", "Chris Frick", "01-01-2013", 0, 0));
//		data.add(new Data(8, "Strøm", "Hanshelleren Kneopatra", "Hand-made Jam", T.BOLT, null, 1, "8b", "Adam Ondra", "01-01-2016", 0, 0));
//		data.add(new Data(9, "Strøm", "Hanshelleren Kneopatra", "Romsdøling på tur", T.TRAD, null, 1, "7c+", "Sindre Sæter", "01-01-2012", 0, 0));
//		data.add(new Data(10, "Strøm", "Hanshelleren Kneopatra", "Kneopatra", T.BOLT, "Markert dieder og opp til anker midt i Bondeanger", 1, "7c+", "Thomas Vekne", "01-01-1999", 0, 0));
//		data.add(new Data(11, "Strøm", "Hanshelleren Kneopatra", "Bondeanger (Start 12)", T.BOLT, "Felles start med kykkelig,traverser så inn i toppen av Kneopatra og fortsetter forbi ankere og helt opp til Stor hylle. Langslynger er fordel på traversen.", 1, "7c", "Thomas Vekne", "01-01-1999", 0, 0));
//		data.add(new Data(12, "Strøm", "Hanshelleren Kneopatra", "Kykkeli", T.BOLT, "Markert riss opp til takoverheng", 1, "7b", "Pål B.Reiten/Thomas Vekne", "01-01-1996", 0, 0));
//		data.add(new Data(13, "Strøm", "Hanshelleren Kneopatra", "Kykkelikokos", T.BOLT, "Forlengelsen av kykkelig", 1, "7b+", "Pål B.Reiten/Thomas Vekne", "01-01-1996", 0, 0));
//		data.add(new Data(14, "Strøm", "Hanshelleren Kneopatra", "Heart Beat", T.BOLT, null, 1, "8c", "Adam Ondra", "01-01-2016", 0, 0));
//		data.add(new Data(15, "Strøm", "Hanshelleren Kneopatra", "Påltergeist", T.BOLT, "Rett opp den relativt blanke flaten.", 1, "7c+", "Pål Benum Reiten", "01-01-1997", 0, 0));
//		data.add(new Data(16, "Strøm", "Hanshelleren Kneopatra", "Dumster Diver", T.BOLT, null, 1, "8a+", "Vojtech Vrzba", "01-01-2016", 0, 0));
//		data.add(new Data(17, "Strøm", "Hanshelleren Kneopatra", "Dumster Master", T.BOLT, null, 1, "8b", "Adam Ondra", "01-01-2016", 0, 0));
//		data.add(new Data(18, "Strøm", "Hanshelleren Kneopatra", "Påletikk", T.TRAD, "Markert riss og diederformasjon", 1, "7b", "Pål Benum Reiten", "01-01-1997", 0, 0));
//		data.add(new Data(1, "Strøm", "Hanshelleren Flaggermusmannen", "Eventyrblanding", T.BOLT, "Starter til høyre for risset påletikk.", 1, "7c", "Thomas Vekne", "01-01-2001", 0, 0));
//		data.add(new Data(2, "Strøm", "Hanshelleren Flaggermusmannen", "Eventyrblanding extension", T.BOLT, null, 1, "8a", "Chris Frick", "01-01-2014", 0, 0));
//		data.add(new Data(3, "Strøm", "Hanshelleren Flaggermusmannen", "Nagells drømmedieder", T.BOLT, "Drummediederet-prosjektet. Felles start med eventyrblanding", 1, "8a+", "Erik Massih", "01-01-2006", 0, 0));
//		data.add(new Data(4, "Strøm", "Hanshelleren Flaggermusmannen", "Nagells drømmedieder extension", T.BOLT, null, 1, "8b", "Adam Ondra", "01-01-2013", 0, 0));
//		data.add(new Data(5, "Strøm", "Hanshelleren Flaggermusmannen", "Massih attack ", T.BOLT, "Egberts krakk-prosjektet. Felles start med eventyrblanding .(L2 8b Tobias wolf 2015)", 1, "8a+", "Erik Massih", "01-01-2006", 0, 0));
//		data.add(new Data(6, "Strøm", "Hanshelleren Flaggermusmannen", "Hovdegjengen", T.BOLT, "En rissformasjon opp til liten hylle.", 1, "6c", "Oscar Hovde Berntsen", "01-01-2001", 0, 0));
//		data.add(new Data(7, "Strøm", "Hanshelleren Flaggermusmannen", "The trap", T.BOLT, null, 1, "8c+", "Adam Ondra", "01-01-2012", 0, 0));
//		data.add(new Data(8, "Strøm", "Hanshelleren Flaggermusmannen", "The trip", T.BOLT, null, 1, "8a+", "Adam Ondra", "01-01-2012", 0, 0));
//		data.add(new Data(9, "Strøm", "Hanshelleren Flaggermusmannen", "Berntsenbanden", T.BOLT, "Forlengelsen av Hovdegjengen ", 1, "7c", "Oscar Hovde Berntsen", "01-01-2003", 0, 0));
//		data.add(new Data(10, "Strøm", "Hanshelleren Flaggermusmannen", "Berntsenbanden extension", T.BOLT, null, 1, "8a", "Adam Ondra", "01-01-2013", 0, 0));
//		data.add(new Data(11, "Strøm", "Hanshelleren Flaggermusmannen", "The doorkeeper", T.BOLT, null, 1, "8b", "Adam Ondra", "01-01-2013", 0, 0));
//		data.add(new Data(12, "Strøm", "Hanshelleren Flaggermusmannen", "Break free", T.BOLT, null, 1, "8b+", "Adam Ondra", "01-01-2012", 0, 0));
//		data.add(new Data(13, "Strøm", "Hanshelleren Flaggermusmannen", "Waliserne kommer", T.BOLT, "En slags bratt diederformasjon", 1, "7c", "Pål Benum Reiten", "01-01-1999", 0, 0));
//		data.add(new Data(14, "Strøm", "Hanshelleren Flaggermusmannen", "waliserne kommer og kommer", T.BOLT, "Start nr 13", 1, "8b", "Erik Grandelius", "01-01-2013", 0, 0));
//		data.add(new Data(15, "Strøm", "Hanshelleren Flaggermusmannen", "Andre høyre", T.BOLT, null, 1, "7c", "Thomas Vekve/Gisle Andersen", "01-01-2000", 0, 0));
//		data.add(new Data(16, "Strøm", "Hanshelleren Flaggermusmannen", "The Right of Spring", T.BOLT, null, 1, "8c+", "Adam Ondra", "01-01-2017", 0, 0));
//		data.add(new Data(17, "Strøm", "Hanshelleren Flaggermusmannen", "Prosjekt", T.BOLT, null, 1, "9a+", "n/a", null, 0, 0));
//		data.add(new Data(18, "Strøm", "Hanshelleren Flaggermusmannen", "Banana Ballet", T.BOLT, null, 1, "8c+", "Adam Ondra", "01-01-2012", 0, 0));
//		data.add(new Data(19, "Strøm", "Hanshelleren Flaggermusmannen", "Dharma", T.BOLT, "Start nr 15", 1, "8c", "Adam Ondra", "01-01-2013", 0, 0));
//		data.add(new Data(20, "Strøm", "Hanshelleren Flaggermusmannen", "Ukjent", T.BOLT, null, 1, "6b+", "n/a", "01-01-2014", 0, 0));
//		data.add(new Data(21, "Strøm", "Hanshelleren Flaggermusmannen", "Prosjekt", T.BOLT, null, 1, "n/a", "n/a", null, 0, 0));
//		data.add(new Data(22, "Strøm", "Hanshelleren Flaggermusmannen", "Swiss Miniature", T.BOLT, null, 1, "8b", "Chris Frick", "01-01-2016", 0, 0));
//		data.add(new Data(23, "Strøm", "Hanshelleren Flaggermusmannen", "Last great climb", T.BOLT, null, 1, "7c", "Adam Pustelnik", "01-01-2014", 0, 0));
//		data.add(new Data(24, "Strøm", "Hanshelleren Flaggermusmannen", "Kakestykket", T.BOLT, "Felles anker med Flaggermusmannen. Gått på kiler første gang. ", 1, "7a", "Pål Benum Reiten", "01-01-1995", 0, 0));
//		data.add(new Data(25, "Strøm", "Hanshelleren Flaggermusmannen", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(26, "Strøm", "Hanshelleren Flaggermusmannen", "Gustanito", T.BOLT, null, 1, "8a+", "Luis Penn", "01-01-2013", 0, 0));
//		data.add(new Data(27, "Strøm", "Hanshelleren Flaggermusmannen", "Illusjonisten", T.BOLT, null, 1, "9a", "Adam Ondra", "01-01-2013", 0, 0));
//		data.add(new Data(28, "Strøm", "Hanshelleren Flaggermusmannen", "Flaggermusmannen", T.BOLT, "Starter ved å klippe første bolt og så dra seg opp til denne. ", 1, "8a", "Pål benum Reiten", "01-01-1996", 0, 0));
//		data.add(new Data(29, "Strøm", "Hanshelleren Flaggermusmannen", "Iron curtain", T.BOLT, null, 1, "9b", "Adam Ondra", "01-01-2013", 0, 0));
//		data.add(new Data(1, "Strøm", "Hanshelleren Muy Verdes", "Joker", T.BOLT, null, 1, "8b+", "Adam Ondra", "01-01-2014", 0, 0));
//		data.add(new Data(2, "Strøm", "Hanshelleren Muy Verdes", "Ronja", T.BOLT, null, 1, "7c", "Adam Ondra", "01-01-2012", 0, 0));
//		data.add(new Data(3, "Strøm", "Hanshelleren Muy Verdes", "Change ", T.BOLT, "(L1 9a+)", 1, "9b+", "Adam Ondra", "01-01-2012", 0, 0));
//		data.add(new Data(4, "Strøm", "Hanshelleren Muy Verdes", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(5, "Strøm", "Hanshelleren Muy Verdes", "Muy Verdes", T.BOLT, null, 1, "8c", "Erik Grandelius", "01-01-2012", 0, 0));
//		data.add(new Data(6, "Strøm", "Hanshelleren Muy Verdes", "Kangaroos Limb", T.BOLT, null, 1, "9a+", "Adam Ondra", "01-01-2013", 0, 0));
//		data.add(new Data(7, "Strøm", "Hanshelleren Muy Verdes", "Prosjekt", T.BOLT, null, 1, "9b", null, null, 0, 0));
//		data.add(new Data(8, "Strøm", "Hanshelleren Muy Verdes", "Odins Eye", T.BOLT, null, 1, "8c+", "Ethan Pringle", "01-01-2012", 0, 0));
//		data.add(new Data(9, "Strøm", "Hanshelleren Muy Verdes", "Vallhalla", T.BOLT, null, 1, "9a", "Adam Ondra", "01-01-2016", 0, 0));
//		data.add(new Data(1, "Strøm", "Hanshelleren Nordic Flower", "Brunhilde L1", T.BOLT, "jumar start", 1, "7c+", "Elliot Ashe", "01-01-2014", 0, 0));
//		data.add(new Data(2, "Strøm", "Hanshelleren Nordic Flower", "Brunhilde L2", T.BOLT, null, 1, "8c", "Adam Ondra", "01-01-2015", 0, 0));
//		data.add(new Data(3, "Strøm", "Hanshelleren Nordic Flower", "Thors Hammer", T.BOLT, null, 1, "9a", "Adam Ondra", "01-01-2012", 0, 0));
//		data.add(new Data(4, "Strøm", "Hanshelleren Nordic Flower", "Nordic Trav", T.BOLT, null, 1, "8a+", "Joe Kinder", "01-01-2014", 0, 0));
//		data.add(new Data(5, "Strøm", "Hanshelleren Nordic Flower", "Witchhammer", T.BOLT, null, 1, "8c+", "Adam Ondra", "01-01-2015", 0, 0));
//		data.add(new Data(6, "Strøm", "Hanshelleren Nordic Flower", "Nordic plumber", T.BOLT, null, 1, "8c", "Ethan Pringle", "01-01-2012", 0, 0));
//		data.add(new Data(7, "Strøm", "Hanshelleren Nordic Flower", "Move", T.BOLT, null, 1, "9b/9b+", "Adam Ondra", "01-01-2013", 0, 0));
//		data.add(new Data(8, "Strøm", "Hanshelleren Nordic Flower", "Little Badder", T.BOLT, null, 1, "9a", "Sebastian Bouin", "01-01-2013", 0, 0));
//		data.add(new Data(9, "Strøm", "Hanshelleren Nordic Flower", "Silence", T.BOLT, null, 1, "9c", "Adam Ondra", "01-01-2017", 0, 0));
//		data.add(new Data(10, "Strøm", "Hanshelleren Nordic Flower", "Nordic Flower L1", T.BOLT, null, 1, "8b+", "Jorg Verhoeven", "01-01-2012", 0, 0));
//		data.add(new Data(11, "Strøm", "Hanshelleren Nordic Flower", "Nordic Flower L2", T.BOLT, null, 1, "8c", "Jorg Verhoeven", "01-01-2012", 0, 0));
//		data.add(new Data(12, "Strøm", "Hanshelleren Nordic Flower", "Dvergtrollet", T.BOLT, null, 1, "8a+", "Magnus Midtbø", "01-01-2012", 0, 0));
//		data.add(new Data(13, "Strøm", "Hanshelleren Nordic Flower", "The Troll Hammer", T.BOLT, "End route 3", 1, "8c", "Henning Wang", "01-01-2015", 0, 0));
//		data.add(new Data(14, "Strøm", "Hanshelleren Nordic Flower", "Open Shoulders", T.BOLT, null, 1, "8a+", "Luca Bana", "01-01-2015", 0, 0));
//		data.add(new Data(15, "Strøm", "Hanshelleren Nordic Flower", "Art of Flights", T.BOLT, "jumar start", 1, "8c+/9a", "Adam Ondra", "01-01-2013", 0, 0));
//		data.add(new Data(16, "Strøm", "Hanshelleren Nordic Flower", "Valkyries", T.BOLT, "jumar start", 1, "8c", "Magnus Midtbø", "01-01-2014", 0, 0));
//		data.add(new Data(17, "Strøm", "Hanshelleren Nordic Flower", "Prosjekt", T.BOLT, "jumar start", 1, "n/a", "n/a", null, 0, 0));
//		data.add(new Data(1, "Strøm", "Hanshelleren Litt på kanten", "Prosjekt", T.BOLT, null, 1, "n/a", "n/a", null, 0, 0));
//		data.add(new Data(2, "Strøm", "Hanshelleren Litt på kanten", "120 Degree", T.BOLT, null, 1, "9a+", "Adam Ondra", "01-01-2016", 0, 0));
//		data.add(new Data(3, "Strøm", "Hanshelleren Litt på kanten", "Elden Inuti L1", T.BOLT, null, 1, "8a+", "Erik Grandelius", "01-01-2014", 0, 0));
//		data.add(new Data(4, "Strøm", "Hanshelleren Litt på kanten", "Elden Inuti,L2 9a,(Slutt 5)", T.BOLT, "L2 9a,slutt 5", 1, "8c+/9a", "Adam Ondra", "01-01-2017", 0, 0));
//		data.add(new Data(5, "Strøm", "Hanshelleren Litt på kanten", "Elden Inuti,L2 ", T.BOLT, null, 1, "8b+", "Erik Grandelius", "01-01-2014", 0, 0));
//		data.add(new Data(6, "Strøm", "Hanshelleren Litt på kanten", "Halibut", T.BOLT, null, 1, "8c+", "Adam Ondra", "01-01-2017", 0, 0));
//		data.add(new Data(7, "Strøm", "Hanshelleren Litt på kanten", "Alea iacta est", T.BOLT, null, 1, "7b", "Chris Frick", "01-01-2013", 0, 0));
//		data.add(new Data(8, "Strøm", "Hanshelleren Litt på kanten", "Litt på kanten", T.BOLT, null, 1, "8a+", "Magnus Midtbø", "01-01-2012", 0, 0));
//		data.add(new Data(9, "Strøm", "Hanshelleren Litt på kanten", "Nachoss", T.BOLT, null, 1, "8b+", "Adam Ondra", "01-01-2017", 0, 0));
//		data.add(new Data(10, "Strøm", "Hanshelleren Litt på kanten", "Sophisticaveman", T.BOLT, null, 1, "7a+", "Elliot Ashe", "01-01-2013", 0, 0));
//		data.add(new Data(11, "Strøm", "Hanshelleren Litt på kanten", "Soutern Invasion", T.BOLT, null, 1, "6c", "Chris Glastonbury", "01-01-2013", 0, 0));
//		data.add(new Data(1, "Strøm", "Hanshellern Footwork", "Overflatekjemi", T.BOLT, null, 1, "6c+", "Einar Sulheim", "01-01-2012", 0, 0));
//		data.add(new Data(2, "Strøm", "Hanshellern Footwork", "Gull i magen", T.BOLT, null, 1, "6a", "Gudmund Grønhaug", "01-01-2011", 0, 0));
//		data.add(new Data(3, "Strøm", "Hanshellern Footwork", "Riktig trivelig", T.BOLT, null, 1, "5a", "Martin Jenssen", "01-01-2011", 0, 0));
//		data.add(new Data(4, "Strøm", "Hanshellern Footwork", "Doplerflata", T.BOLT, null, 1, "5a", "Daniel Høyer Iversen", "01-01-2012", 0, 0));
//		data.add(new Data(5, "Strøm", "Hanshellern Footwork", "Gorillaglass", T.BOLT, null, 1, "4c", "Jon Petter Myklebust", "01-01-2012", 0, 0));
//		data.add(new Data(6, "Strøm", "Hanshellern Footwork", "Footwork", T.BOLT, null, 1, "5a", "Jeanette Wagelid", "01-01-2012", 0, 0));
//		data.add(new Data(1, "Strøm", "Oppvarmingsveggen", "Organiske forbindelser", T.TRAD, "Tydelig riss opp hele veggen", 1, "7-", null, "01-01-1999", 0, 0));
//		data.add(new Data(2, "Strøm", "Oppvarmingsveggen", "Kollisjonteori", T.TRAD, "Markert riss", 1, "7", null, "01-01-1998", 0, 0));
//		data.add(new Data(3, "Strøm", "Oppvarmingsveggen", "Frie radikaler", T.TRAD, "Tydelig riss opp hele veggen", 1, "7-", "Thomas Vekne/Kari Torgersen", "01-01-1998", 0, 0));
//		data.add(new Data(4, "Strøm", "Oppvarmingsveggen", "Bunnfall", T.TRAD, "Tydelig riss opp hele veggen,noe dårlig sikret i toppen", 1, "6-", "Thomas Vekne/Kari Torgersen", "01-01-1998", 0, 0));
//		data.add(new Data(5, "Strøm", "Oppvarmingsveggen", "Brownske Bevegelser", T.TRAD, "Rotete riss", 1, "6-", "Anders Kringstad/Eivind W. Nagell", "01-01-1999", 0, 0));
//		data.add(new Data(6, "Strøm", "Oppvarmingsveggen", "Det kritiske punkt", T.TRAD, "Rotete riss", 1, "6-", "Anders Kringstad/Eivind W. Nagell", "01-01-1999", 0, 0));
//		data.add(new Data(7, "Strøm", "Oppvarmingsveggen", "Likevekt", T.TRAD, null, 1, "6+", "Thomas Vekne", "01-01-2000", 0, 0));
//		data.add(new Data(8, "Strøm", "Oppvarmingsveggen", "Amatøren", T.TRAD, "Boltene er noe dårlig plassert", 1, "7-", "Thomas Vekne", "01-01-2000", 0, 0));
//		data.add(new Data(1, "Nordstrømmen", "Flatanger Elektro", "Prosjekt", T.BOLT, "Ikke boltet enda,blankt sva,anker satt av Eivind W.Nagell", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Nordstrømmen", "Flatanger Elektro", "Vellamellen elgburger", T.BOLT, null, 1, "7+", "Eivind W. Nagell", "01-01-2000", 0, 0));
//		data.add(new Data(3, "Nordstrømmen", "Flatanger Elektro", "Prosjekt", T.TRAD, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(4, "Nordstrømmen", "Flatanger Elektro", "Strømallergi", T.BOLT, null, 1, "7+", "Lars Audun Nordnes", "01-01-2003", 0, 0));
//		data.add(new Data(5, "Nordstrømmen", "Flatanger Elektro", "Rutebandittene", T.TRAD, "Diagonalt riss oppover mot venstre.", 1, "5+", "Thomas Vekne/Roger Ebeltoft", "01-01-2000", 0, 0));
//		data.add(new Data(1, "Dala ", "VVS Hovedveggen", "n/a", T.TRAD, null, 1, "8", "Erik massih", "01-01-2006", 0, 0));
//		data.add(new Data(2, "Dala ", "VVS Hovedveggen", "Gisle", T.BOLT, null, 1, "8/8+", "Pål benum Reiten", "01-01-2000", 0, 0));
//		data.add(new Data(3, "Dala ", "VVS Hovedveggen", "Gislefoss", T.BOLT, null, 1, "9-/9", "Pål benum Reiten", "01-01-2000", 0, 0));
//		data.add(new Data(4, "Dala ", "VVS Hovedveggen", "Kost og losji", T.BOLT, null, 1, "8+", "Eivind W. Nagell", "01-01-2000", 0, 0));
//		data.add(new Data(5, "Dala ", "VVS Hovedveggen", "Ymse vare", T.BOLT, null, 1, "8", "Thomas Vekne", "01-01-2000", 0, 0));
//		data.add(new Data(6, "Dala ", "VVS Hovedveggen", "Åpent prosjekt", T.BOLT, "Boltet av Pål benum reiten ", 1, "n/a", null, "01-01-2000", 0, 0));
//		data.add(new Data(7, "Dala ", "VVS Hovedveggen", "Åpent prosjekt", T.BOLT, null, 1, "n/a", null, "01-01-2000", 0, 0));
//		data.add(new Data(8, "Dala ", "VVS Hovedveggen", "Grøftefyll", T.BOLT, null, 1, "8", "Pål benum Reiten", "01-01-2001", 0, 0));
//		data.add(new Data(9, "Dala ", "VVS Hovedveggen", "Åpent prosjekt", T.BOLT, "Forlengelse av grøftefyll. Boltet av Pål Benum Reiten", 1, "9+", null, "01-01-2000", 0, 0));
//		data.add(new Data(1, "Dala ", "VVS Skoveggen", "BS2000", T.BOLT, "Samme anker som Scarpa Brio", 1, "6+", "Eivind W. Nagell/Pål Benum Reiten", "01-01-2000", 0, 0));
//		data.add(new Data(2, "Dala ", "VVS Skoveggen", "Scarpa Brio", T.BOLT, null, 1, "n/a", "Eivind W. Nagell/Gisle Andersen", "01-01-1999", 0, 0));
//		data.add(new Data(3, "Dala ", "VVS Skoveggen", "Åpent prosjekt", T.BOLT, "Åpent prosjekt, boltet av Thomas Vekne", 1, "8", null, "01-01-2000", 0, 0));
//		data.add(new Data(4, "Dala ", "VVS Skoveggen", "De nye hoppskoene reddet ham i landingen", T.BOLT, null, 1, "7+", "Eivind W. Nagell", "01-01-2000", 0, 0));
//		data.add(new Data(1, "Storfjellet ", "Nordsiden", "Sørmarksfjellets siste år", T.TRAD, "p1(5)60m opp til hylle,p2 60m(5)Opp tydelig riss til standplass under blokk,p3 ut venstre og over stor blokk,opp fin formasjon til topp50m noe taudrag.", 1, "5", "Morten Stamnes/Magnus Staven", "01-01-2019", 0, 0));
//		data.add(new Data(1, "Glasøyfjellet", "Manni der Hassel?", "Little Spiderman", T.BOLT, null, 1, "7a+", "Katinka Mühlschlegel", "01-01-2012", 0, 0));
//		data.add(new Data(2, "Glasøyfjellet", "Manni der Hassel?", "Manni der Hassler", T.BOLT, null, 1, "8a+", "Tom Thudium", "01-01-2012", 0, 0));
//		data.add(new Data(1, "Glasøyfjellet", "Golden Wand ", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Glasøyfjellet", "Golden Wand ", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(3, "Glasøyfjellet", "Golden Wand ", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(4, "Glasøyfjellet", "Golden Wand ", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(5, "Glasøyfjellet", "Golden Wand ", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(6, "Glasøyfjellet", "Golden Wand ", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(1, "Glasøyfjellet", "Aurora Borealis", "Frosken", T.BOLT, null, 1, "8b", "Martin Mobråten", "01-01-2014", 0, 0));
//		data.add(new Data(2, "Glasøyfjellet", "Aurora Borealis", "Kämpfer der Nacht", T.BOLT, null, 1, "7c", "Jochen Perschmann", "01-01-2012", 0, 0));
//		data.add(new Data(3, "Glasøyfjellet", "Aurora Borealis", "L2 Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(4, "Glasøyfjellet", "Aurora Borealis", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(5, "Glasøyfjellet", "Aurora Borealis", "Aurora borealis", T.BOLT, null, 1, "7b+", "Miriam Schulz", "01-01-2012", 0, 0));
//		data.add(new Data(6, "Glasøyfjellet", "Aurora Borealis", "Crack the Back", T.BOLT, null, 1, "8c", "Jochen Perschmann", "01-01-2012", 0, 0));
//		data.add(new Data(1, "Glasøyfjellet", "Berganstich", "Nr 1", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Glasøyfjellet", "Berganstich", "Nr 2", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(3, "Glasøyfjellet", "Berganstich", "Nr 3", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(4, "Glasøyfjellet", "Berganstich", "Nr 4", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(5, "Glasøyfjellet", "Berganstich", "Nr 5", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//
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
//		Problem p = new Problem(idArea, 0, null, idSector, 0, null, 0, 0, null, null, 0, 0, null, -1, 0, d.getNr(), d.getProblem(), d.getComment(), null, d.getGrade().replaceAll(" ", ""), d.getFaDate(), null, fa, d.getLat(), d.getLng(), null, 0, 0, false, null, t, false, 0);
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
//		Area a = new Area(REGION_ID, null, -1, 0, false, d.getArea(), null, 0, 0, -1, -1, null, null, 0);
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