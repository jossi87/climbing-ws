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
//import com.buldreinfo.jersey.jaxb.model.Redirect;
//import com.buldreinfo.jersey.jaxb.model.Sector;
//import com.buldreinfo.jersey.jaxb.model.Type;
//import com.buldreinfo.jersey.jaxb.model.UserSearch;
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
//	private final static int REGION_ID = 19;
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
//		data.add(new Data(1, "6,7 fjellet", "Enerveggen", "Nøkken", T.BOLT, null, 1, "5+", null, null, 0, 0));
//		data.add(new Data(2, "6,7 fjellet", "Enerveggen", "Orient Expressen", T.BOLT, null, 1, "6+", null, null, 0, 0));
//		data.add(new Data(3, "6,7 fjellet", "Enerveggen", "Generator", T.BOLT, null, 1, "5+", null, null, 0, 0));
//		data.add(new Data(4, "6,7 fjellet", "Enerveggen", "T-strengen", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(5, "6,7 fjellet", "Enerveggen", "Bimbo", T.BOLT, null, 1, "6+", null, null, 0, 0));
//		data.add(new Data(6, "6,7 fjellet", "Enerveggen", "Bjørka", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(7, "6,7 fjellet", "Enerveggen", "Flaggemusa", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(8, "6,7 fjellet", "Enerveggen", "Jetjager", T.BOLT, null, 1, "6+", null, null, 0, 0));
//		data.add(new Data(9, "6,7 fjellet", "Enerveggen", "Hotline", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(1, "6,7 fjellet", "Firerveggen", "Lalitrona", T.TRAD, "Snu i tre på toppen.", 1, "7-", null, null, 0, 0));
//		data.add(new Data(2, "6,7 fjellet", "Firerveggen", "Rausprekka", T.BOLT, "Snu i tre på toppen.", 1, "7-", null, null, 0, 0));
//		data.add(new Data(3, "6,7 fjellet", "Firerveggen", "Lasse", T.BOLT, "Det er så tett boltet at man kan snu i to fine Børrebolter.", 1, "5-", null, null, 0, 0));
//		data.add(new Data(4, "6,7 fjellet", "Firerveggen", "Pumpaway", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(5, "6,7 fjellet", "Firerveggen", "6,7 ruta", T.BOLT, "Bratt rute med gode tak.", 1, "6+", null, null, 0, 0));
//		data.add(new Data(6, "6,7 fjellet", "Firerveggen", "Delikatessen", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(7, "6,7 fjellet", "Firerveggen", "Gummiringen", T.BOLT, null, 1, "7-", null, null, 0, 0));
//		data.add(new Data(8, "6,7 fjellet", "Firerveggen", "Niggerfield", T.BOLT, "Morsom klatring i dieder. Crux mot toppen.", 1, "6-", null, null, 0, 0));
//		data.add(new Data(9, "6,7 fjellet", "Firerveggen", "Fjodor", T.BOLT, "Sloper og lister.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(10, "6,7 fjellet", "Firerveggen", "Whiteface", T.BOLT, "Små lister, smyging.", 1, "7-", null, null, 0, 0));
//		data.add(new Data(11, "6,7 fjellet", "Firerveggen", "Vorspiel", T.BOLT, "Frisk start i laybackriss. Små lister ved cruxet.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(1, "6,7 fjellet", "Toerveggen", "Pink Panter", T.BOLT, null, 1, "5-", null, null, 0, 0));
//		data.add(new Data(2, "6,7 fjellet", "Toerveggen", "Zorro", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(3, "6,7 fjellet", "Toerveggen", "Fantomet", T.BOLT, "mangler første bolt", 1, "5+", null, null, 0, 0));
//		data.add(new Data(4, "6,7 fjellet", "Toerveggen", "Jalla", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(5, "6,7 fjellet", "Toerveggen", "Batman", T.BOLT, null, 1, "6-", null, null, 0, 0));
//		data.add(new Data(6, "6,7 fjellet", "Toerveggen", "Second", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(7, "6,7 fjellet", "Toerveggen", "Min første", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(8, "6,7 fjellet", "Toerveggen", "Bjørnen sover", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(1, "Andøya", "Take It or Leave It", "Krabbeklo", T.TOPROPE, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Andøya", "Take It or Leave It", "Fiskesprett", T.TOPROPE, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(3, "Andøya", "Take It or Leave It", "Hopp i havet", T.TOPROPE, null, 1, "5-", null, null, 0, 0));
//		data.add(new Data(4, "Andøya", "Take It or Leave It", "Styggen på ryggen", T.TOPROPE, null, 1, "5+", null, null, 0, 0));
//		data.add(new Data(5, "Andøya", "Take It or Leave It", "Take It or Leave It", T.TOPROPE, null, 1, "5-", null, null, 0, 0));
//		data.add(new Data(6, "Andøya", "Take It or Leave It", "Tean i Tanga", T.TOPROPE, null, 1, "5-", null, null, 0, 0));
//		data.add(new Data(7, "Andøya", "Take It or Leave It", "Tangatruse", T.TOPROPE, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(8, "Andøya", "Take It or Leave It", "Grillpikene", T.TOPROPE, null, 1, "5+/6-", null, null, 0, 0));
//		data.add(new Data(1, "Asylet", "Hovedveggen", "Det er bare dumme folk somfryser", T.BOLT, "6bb", 1, "8-", "Geir Hugo Hansen", null, 0, 0));
//		data.add(new Data(2, "Asylet", "Hovedveggen", "Nils Rune gikk i klasse med Mette-Marit", T.BOLT, "7bb. Topp 50!!", 1, "8/8+", "Geir Hugo Hansen", null, 0, 0));
//		data.add(new Data(3, "Asylet", "Hovedveggen", "Pepper'n gror", T.BOLT, "8bb. Pass på tauslitasje i toppen ved nedfiring.", 1, "8-", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(4, "Asylet", "Hovedveggen", "Saddam og bror min", T.BOLT, "8bb. Særegen klaring på eggen", 1, "8/8+", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(5, "Asylet", "Hovedveggen", "Prosjekt", T.BOLT, "Kun etablert anker", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(6, "Asylet", "Hovedveggen", "Prosjekt", T.BOLT, "Tung layback som er gått på topptau. Mangler ledebolter.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(7, "Asylet", "Hovedveggen", "Svidd neger", T.BOLT, "8bb. To bratte opptak med god hvil imellom.", 1, "n/a", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(8, "Asylet", "Hovedveggen", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(9, "Asylet", "Hovedveggen", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(10, "Asylet", "Hovedveggen", "Kebab-Larsen", T.BOLT, "14bb. Overraskende lett til å være så bratt.", 1, "7-", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(11, "Asylet", "Hovedveggen", "Asylet", T.BOLT, "7bb. Helt herlig bratt klatring. Topp 50!", 1, "7+", "Lars Reiersen", null, 0, 0));
//		data.add(new Data(12, "Asylet", "Hovedveggen", "Zulu", T.BOLT, "15bb. Klassiker. Store men litt skarpe tak i toppen. 60m tau.", 1, "8-", "Jon Dalvang Andresen", null, 0, 0));
//		data.add(new Data(13, "Asylet", "Hovedveggen", "Ahmed i bua", T.BOLT, "14bb", 1, "7+/8-", null, null, 0, 0));
//		data.add(new Data(14, "Asylet", "Hovedveggen", "Mulla", T.BOLT, "15bb. Rå nytelse. Topp 50!", 1, "n/a", "Jon Dalvang Andresen", null, 0, 0));
//		data.add(new Data(15, "Asylet", "Hovedveggen", "Brynjar", T.BOLT, "8bb", 1, "7+", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(16, "Asylet", "Hovedveggen", "Prosjekt", T.BOLT, "9bb. Rå linje. Rensing og noe justering av bolter nødvendig.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(5, "Brennevinsmyra", "Strongman", "Gin and Tonic", T.BOLT, "7bb. For dem med fingerstyrke. Kreatør: Øystein Engen", 1, "n/a", "Søren Wold", "2008-01-01", 0, 0));
//		data.add(new Data(6, "Brennevinsmyra", "Strongman", "Bloody Mary", T.BOLT, "7bb. Bratt sva opp til markant formasjon. Kreatør: Jonny Grundeland", 1, "7+/8-", "Søren Wold", "2008-01-01", 0, 0));
//		data.add(new Data(7, "Brennevinsmyra", "Strongman", "Heimlaga", T.BOLT, "7bb. Fantastisk flott og vedvarende rute. Kreatør: Jon Flydal", 1, "7/7+", "Søren Wold", "2008-01-01", 0, 0));
//		data.add(new Data(8, "Brennevinsmyra", "Strongman", "Fylleangst", T.BOLT, "6bb. Flott klatring opp en grunn risslinje.", 1, "8-", "Ole Karsten Birkeland", "2007-01-01", 0, 0));
//		data.add(new Data(1, "Brennevinsmyra", "Vandalisme", "En lille en", T.BOLT, "5bb. Enkel rute med hyggelig klaring.", 1, "4+", "Jonny Grundeland", "2007-01-01", 0, 0));
//		data.add(new Data(2, "Brennevinsmyra", "Vandalisme", "Hei og hå og ei flaske fra Lom", T.BOLT, "5bb. Flott klatring opp dieder, avslutning i vertikalt parti.", 1, "6-", "Jonny Grundeland", "2007-01-01", 0, 0));
//		data.add(new Data(3, "Brennevinsmyra", "Vandalisme", "Knerten", T.BOLT, "7bb", 1, "6+", null, null, 0, 0));
//		data.add(new Data(4, "Brennevinsmyra", "Vandalisme", "I skyttergraven på hjemmefronten", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(9, "Brennevinsmyra", "Vattenfall", "Uten navn", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(10, "Brennevinsmyra", "Vattenfall", "Ingen fest uten skinnvest", T.BOLT, "3bb. Fin rute opp i en liten diederformasjon.", 1, "n/a", "Henning Wang", null, 0, 0));
//		data.add(new Data(11, "Brennevinsmyra", "Vattenfall", "Kaffeknerten", T.BOLT, "4bb. Artig svaklatring", 1, "6-", "Henning Wang", "2009-01-01", 0, 0));
//		data.add(new Data(12, "Brennevinsmyra", "Vattenfall", "Du bli itj fin uten makkasin", T.BOLT, "4bb. Sva med artige små hull", 1, "n/a", "Henning Wang", "2009-01-01", 0, 0));
//		data.add(new Data(13, "Brennevinsmyra", "Vattenfall", "Finn og Dull Memorial Route", T.BOLT, "4bb. Sva eller vertikalt.", 1, "n/a", "Henning Wang", "2009-01-01", 0, 0));
//		data.add(new Data(14, "Brennevinsmyra", "Vattenfall", "Grunemannen", T.BOLT, "4bb. Fin rute i formasjoner nær en risslinje.", 1, "5+", "Henning Wang", "2009-01-01", 0, 0));
//		data.add(new Data(15, "Brennevinsmyra", "Vattenfall", "Edle dråper", T.BOLT, "4bb. Veldig enkel rute i det som i våte perioder er en bekk.", 1, "n/a", "Henning Wang", "2009-01-01", 0, 0));
//		data.add(new Data(16, "Brennevinsmyra", "Vattenfall", "Lille promille", T.BOLT, "4bb. Veldig enkel rute. Fin for barn og til å øve på led.", 1, "n/a", "Halvtan Houge", null, 0, 0));
//		data.add(new Data(1, "Buråsen", "Buråsen", "Baller av luft", T.BOLT, "5bb. Fin begeynnerrute", 1, "n/a", "Jonny Grundeland", null, 0, 0));
//		data.add(new Data(2, "Buråsen", "Buråsen", "Vett i pannen, stål i blikket", T.BOLT, "4bb. Fin begynnerrute.", 1, "n/a", "Jonny Grundeland", "2004-01-01", 0, 0));
//		data.add(new Data(3, "Buråsen", "Buråsen", "Fru Huffameg", T.BOLT, "7bb", 1, "6+", "Jonny Grundeland", "1996-01-01", 0, 0));
//		data.add(new Data(4, "Buråsen", "Buråsen", "Vi trosset regn", T.BOLT, "6bb. Fin oppvarmingsrute. Første ruta på Buråsen.", 1, "6-", "Ole Karsten Birkeland", "1996-01-01", 0, 0));
//		data.add(new Data(5, "Buråsen", "Buråsen", "Zen", T.BOLT, "6bb. Ei rute som har litt preg av definisjons-hysteri.", 1, "7-", "Morten W. Rasmussen", "2002-01-01", 0, 0));
//		data.add(new Data(6, "Buråsen", "Buråsen", "Oppvarming", T.BOLT, "6bb. Kanskje den mest klatrede ruta på Buråsen.", 1, "n/a", "Andreas Loland", "1996-01-01", 0, 0));
//		data.add(new Data(7, "Buråsen", "Buråsen", "Tjukken og meg", T.BOLT, "6bb", 1, "6+", "Ole Karsten Birkeland", "2005-01-01", 0, 0));
//		data.add(new Data(8, "Buråsen", "Buråsen", "Det var jo meg", T.BOLT, "11bb. En fordel med lang slynge i fjerde bolt for å unngå taudrag.", 1, "n/a", "Øyvind Udø", null, 0, 0));
//		data.add(new Data(9, "Buråsen", "Buråsen", "Snyrild", T.BOLT, "11bb. Fin svaklatring i siste halvdel.", 1, "n/a", "Jonny Grundeland", null, 0, 0));
//		data.add(new Data(10, "Buråsen", "Buråsen", "Madonna", T.BOLT, "10bb. Først gått naturlig sikret...", 1, "n/a", "Dag Palmar Høiland", "1999-01-01", 0, 0));
//		data.add(new Data(11, "Buråsen", "Buråsen", "Bartebill", T.TRAD, null, 1, "n/a", "Jon Flydal", null, 0, 0));
//		data.add(new Data(12, "Buråsen", "Buråsen", "Felleskjøpet", T.BOLT, "9bb. Her trenger du kraft. Kreatør: Jonny Grundeland", 1, "n/a", "Lars Ole Gudvang", null, 0, 0));
//		data.add(new Data(13, "Buråsen", "Buråsen", "Fransk åpning", T.BOLT, "9bb. Klassikeren! Topp 50!", 1, "7-", "Nils Rune Birkeland", "1996-01-01", 0, 0));
//		data.add(new Data(14, "Buråsen", "Buråsen", "Sørlandsidyll", T.BOLT, "10bb. En direkte variant av 'Fransk åpning'. Kul klatring.", 1, "7+", "Ole Karsten Birkeland", "2002-01-01", 0, 0));
//		data.add(new Data(15, "Buråsen", "Buråsen", "Porsjekt", T.BOLT, "8bb. Bratt, bratt, bratt! Kreatør: Morten Finnes", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(16, "Buråsen", "Buråsen", "Flaket direkte", T.BOLT, "8bb. Direkte start til Flaket.", 1, "7+", null, null, 0, 0));
//		data.add(new Data(17, "Buråsen", "Buråsen", "Flaket", T.BOLT, "8bb. Bratt, brutal start, men super klatring. Kreatør: Andreas Loland", 1, "7/7+", null, null, 0, 0));
//		data.add(new Data(18, "Buråsen", "Buråsen", "Scoop", T.BOLT, "8bb", 1, "6-", "Ole Karsten Birkeland", "1996-01-01", 0, 0));
//		data.add(new Data(19, "Buråsen", "Buråsen", "Jam a lot", T.BOLT, "8bb. Pumpende for graden. Topp 50!", 1, "6+", "Andreas Loland", "1996-01-01", 0, 0));
//		data.add(new Data(20, "Buråsen", "Buråsen", "Sørlandsfører.com", T.BOLT, "7bb", 1, "7-", "Ole Karsten Birkeland", "2004-01-01", 0, 0));
//		data.add(new Data(21, "Buråsen", "Buråsen", "Lillemor Nielsen", T.BOLT, "5bb", 1, "n/a", "Kjetil Nielsen", "2004-01-01", 0, 0));
//		data.add(new Data(22, "Buråsen", "Buråsen", "Råtass", T.BOLT, "6bb. Pumpende!", 1, "8-", "Michael R. Helgestad", "2004-01-01", 0, 0));
//		data.add(new Data(23, "Buråsen", "Buråsen", "Knoll og Tott", T.BOLT, "6bb", 1, "n/a", "Ole Karsten Birkeland", "2004-01-01", 0, 0));
//		data.add(new Data(24, "Buråsen", "Buråsen", "Tellevimsen", T.BOLT, "4bb", 1, "7-", "Ole Karsten Birkeland", "2004-01-01", 0, 0));
//		data.add(new Data(25, "Buråsen", "Buråsen", "Sukkertøygenerasjonen", T.BOLT, "4bb. Lite eksponert.", 1, "5-", "Jonny Grundeland", null, 0, 0));
//		data.add(new Data(26, "Buråsen", "Buråsen", "Bugg", T.BOLT, "3bb. Dieder med markert crux", 1, "5+", "Jonny Grundeland", null, 0, 0));
//		data.add(new Data(27, "Buråsen", "Buråsen", "Just Like Hell", T.BOLT, "3bb. Bratt men kort.", 1, "6-", "Jonny Grundeland", null, 0, 0));
//		data.add(new Data(1, "Edens hage", "Edens hage Main wall", "Adam Returns to Paradise", T.BOLT, "6bb. Morsom langs skråhylla.", 1, "n/a", "Adam Renshaw", null, 0, 0));
//		data.add(new Data(2, "Edens hage", "Edens hage Main wall", "Stones Throw Away", T.BOLT, "6bb. Bøtteballett.", 1, "6+/7-", "Adam Renshaw", null, 0, 0));
//		data.add(new Data(3, "Edens hage", "Edens hage Main wall", "Guds finger", T.BOLT, "6bb. Balansegang og litt dynamikk mot toppen.", 1, "6+", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(4, "Edens hage", "Edens hage Main wall", "Syndefallet", T.BOLT, "5bb. Balanse og smyging. Tøff i toppen.", 1, "7-", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(5, "Edens hage", "Edens hage Main wall", "Gabriel den førstefødte", T.BOLT, "5bb. Dra på, gode tak.", 1, "6+", "Adam Renshaw", null, 0, 0));
//		data.add(new Data(6, "Edens hage", "Edens hage Main wall", "Lucky Lucifer", T.BOLT, "6bb. Interessant crux. Kjipt vil noen si.", 1, "6+/7-", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(7, "Edens hage", "Edens hage Main wall", "Evas eple", T.BOLT, "6bb. OK oppvarming", 1, "n/a", "Mats Kristian Støvik", null, 0, 0));
//		data.add(new Data(1, "Frøysland", "Frøysland", "Mathea", T.MIXED, "2bb", 1, "7-", "Michael R. Helgestad", "2006-01-01", 0, 0));
//		data.add(new Data(2, "Frøysland", "Frøysland", "Ina Lill", T.TRAD, null, 1, "n/a", "Ole Karsten Birkeland", "2006-01-01", 0, 0));
//		data.add(new Data(3, "Frøysland", "Frøysland", "Liv Anna", T.TRAD, null, 1, "n/a", "Ole Karsten Birkeland,Michael R. Helgestad", "2006-01-01", 0, 0));
//		data.add(new Data(4, "Frøysland", "Frøysland", "Ragnhild", T.TRAD, null, 1, "6+", "Ole Karsten Birkeland,Michael R. Helgestad", "2006-01-01", 0, 0));
//		data.add(new Data(5, "Frøysland", "Frøysland", "Isabella", T.TRAD, null, 1, "n/a", "Ole Karsten Birkeland,Michael R. Helgestad", "2006-01-01", 0, 0));
//		data.add(new Data(6, "Frøysland", "Frøysland", "Anne", T.TRAD, null, 1, "n/a", "Ole Karsten Birkeland,Michael R. Helgestad", "2006-01-01", 0, 0));
//		data.add(new Data(1, "Gladfjell", "Gladfjell", "Støvelruta", T.TOPROPE, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Gladfjell", "Gladfjell", "Blowaway", T.TOPROPE, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(3, "Gladfjell", "Gladfjell", "Dynamitt", T.TOPROPE, null, 1, "6+", null, null, 0, 0));
//		data.add(new Data(4, "Gladfjell", "Gladfjell", "Pensjonistruta", T.BOLT, "3bb", 1, "6-", null, null, 0, 0));
//		data.add(new Data(5, "Gladfjell", "Gladfjell", "Donald", T.BOLT, "4bb", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(6, "Gladfjell", "Gladfjell", "Dolly", T.BOLT, "4bb", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(7, "Gladfjell", "Gladfjell", "Farlige former", T.TOPROPE, null, 1, "6+", null, null, 0, 0));
//		data.add(new Data(8, "Gladfjell", "Gladfjell", "Fingeren i sprekka", T.TOPROPE, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(9, "Gladfjell", "Gladfjell", "Litt på kanten", T.BOLT, "4bb", 1, "6-", null, null, 0, 0));
//		data.add(new Data(10, "Gladfjell", "Gladfjell", "Brunetten", T.BOLT, "4bb", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(11, "Gladfjell", "Gladfjell", "Kokkens rute", T.TOPROPE, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(12, "Gladfjell", "Gladfjell", "Markens", T.BOLT, "4bb", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(13, "Gladfjell", "Gladfjell", "Cliffhanger", T.BOLT, "3bb", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(14, "Gladfjell", "Gladfjell", "Bøkeruta", T.BOLT, "Går på gamle tretak opp og ut gjennom helleren. Ruta anbefales ikke lenger på grunn av råtne tretak og rustne bolter. Men dog en kuriositet.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(1, "Haldywood", "Haldywood", "Der Untergang", T.BOLT, "5bb. Starter på venste siden opp mur ved grotte.", 1, "7/7+", "Nikolai Kolstad", "2013-01-01", 0, 0));
//		data.add(new Data(2, "Haldywood", "Haldywood", "An Unexpected Journey", T.BOLT, "4bb", 1, "n/a", "Lars Martin Løvlien", "2014-01-01", 0, 0));
//		data.add(new Data(3, "Haldywood", "Haldywood", "Prosjekt", T.BOLT, "7bb. Kreatør: Ole Karsten Birkeland.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(4, "Haldywood", "Haldywood", "The Matrix", T.BOLT, "6bb. Super klatring. Markert hardere klatring mot slutten.", 1, "8-", "Atle Ådnebergli", "2013-01-01", 0, 0));
//		data.add(new Data(5, "Haldywood", "Haldywood", "Ringenes Herre", T.BOLT, "6bb. Nydelig diederklatring med flere opptak. Rundt eggen.", 1, "5+", "Atle Ådnebergli", "2013-01-01", 0, 0));
//		data.add(new Data(6, "Haldywood", "Haldywood", "Into the Wild", T.BOLT, "6bb. Starter ved travers venstre ved et tre.", 1, "n/a", "Øystein Engen", "2013-01-01", 0, 0));
//		data.add(new Data(7, "Haldywood", "Haldywood", "Get Shorty", T.BOLT, "6bb. Kort og buldrete.", 1, "7-", "Sverre Aakvaag", "2013-01-01", 0, 0));
//		data.add(new Data(8, "Haldywood", "Haldywood", "Easy Rider", T.BOLT, "11bb. Flott diederklatring. Layback på gode tak.", 1, "6-", "Nikolai Kolstad", "2013-01-01", 0, 0));
//		data.add(new Data(9, "Haldywood", "Haldywood", "Childs play", T.BOLT, "11bb. Kort krimpete og buldrete sak.", 1, "7+", "Nikolai Kolstad", "2013-01-01", 0, 0));
//		data.add(new Data(10, "Haldywood", "Haldywood", "21 Jump Street", T.MIXED, "10bb. Kort og buldrete med et langt strekk til hull på toppen.", 1, "n/a", "Sverre Aakvaag", "2013-01-01", 0, 0));
//		data.add(new Data(1, "Hellemyr", "Brattveggen", "Lurifaksen", T.BOLT, "6bb", 1, "n/a", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(2, "Hellemyr", "Brattveggen", "Apefrykt", T.BOLT, "7bb", 1, "n/a", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(3, "Hellemyr", "Brattveggen", "Skomakerhelvete", T.BOLT, "7bb", 1, "7-/7", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(4, "Hellemyr", "Brattveggen", "Eggknuseren, åpent prosjekt", T.BOLT, "6bb", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(5, "Hellemyr", "Brattveggen", "Fortvilt nøtt, åpent prosjekt", T.BOLT, "5bb", 1, "8+", null, null, 0, 0));
//		data.add(new Data(1, "Hellemyr", "Slope-sva-veggen", "Vannsprut", T.BOLT, "6bb. Er det noen tak her?", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Hellemyr", "Slope-sva-veggen", "Triumf og tragedie", T.TOPROPE, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(3, "Hellemyr", "Slope-sva-veggen", "Optikeren", T.BOLT, "7bb. Ser lett ut. På tide å skifte briller?", 1, "6+/7-", null, null, 0, 0));
//		data.add(new Data(4, "Hellemyr", "Slope-sva-veggen", "Iskald", T.BOLT, "6bb", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(5, "Hellemyr", "Slope-sva-veggen", "Militærmarsjen", T.BOLT, "6bb", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(6, "Hellemyr", "Slope-sva-veggen", "Undertake", T.BOLT, "6bb. Sloper og undertak.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(7, "Hellemyr", "Slope-sva-veggen", "Den gyldne ape", T.TOPROPE, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(1, "Hellemyr", "Storveggen", "Tørketiden", T.BOLT, "7bb", 1, "6+", "Stian Reinhartsen", null, 0, 0));
//		data.add(new Data(2, "Hellemyr", "Storveggen", "Karlsen på taket", T.TOPROPE, null, 1, "7-/7", "Stian Reinhartsen", null, 0, 0));
//		data.add(new Data(3, "Hellemyr", "Storveggen", "Battery", T.BOLT, "8bb", 1, "6/6+", "Bjarte Vestøl", null, 0, 0));
//		data.add(new Data(4, "Hellemyr", "Storveggen", "Silje Sommerfugl", T.BOLT, "9bb", 1, "7-/7", "Bjarte Vestøl", null, 0, 0));
//		data.add(new Data(5, "Hellemyr", "Storveggen", "Flyveren", T.TOPROPE, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(6, "Hellemyr", "Storveggen", "Tingen", T.BOLT, "11bb. Bratt, psykende start, etter hvert mer vertikalt. klassiker.", 1, "n/a", "Markus Landrø", null, 0, 0));
//		data.add(new Data(7, "Hellemyr", "Storveggen", "Talking to The Birds", T.BOLT, "9bb. Vrient å komme i gang.", 1, "6+", null, null, 0, 0));
//		data.add(new Data(8, "Hellemyr", "Storveggen", "Sindres drøm", T.TOPROPE, null, 1, "5-", null, null, 0, 0));
//		data.add(new Data(1, "Hellemyr", "Vegg 5", "Jungeldyret", T.BOLT, "2bb. Et stilig flytt, så er du i mål.", 1, "6+", "Aleksander Engesland", null, 0, 0));
//		data.add(new Data(2, "Hellemyr", "Vegg 5", "Månen", T.TRAD, null, 1, "7-", "Lars Erik Omland", null, 0, 0));
//		data.add(new Data(3, "Hellemyr", "Vegg 5", "Vankelmodig", T.BOLT, "3bb. Fine bevegelser.", 1, "7-", "Thomas Aksnes", null, 0, 0));
//		data.add(new Data(4, "Hellemyr", "Vegg 5", "Umulig", T.TOPROPE, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(5, "Hellemyr", "Vegg 5", "Harehoppet.", T.BOLT, "5bb", 1, "n/a", "Jon Svendsen", null, 0, 0));
//		data.add(new Data(6, "Hellemyr", "Vegg 5", "Bydalsrisset", T.TRAD, null, 1, "6+", "Markus Landrø", null, 0, 0));
//		data.add(new Data(7, "Hellemyr", "Vegg 5", "Huren", T.BOLT, "4bb", 1, "8+", null, null, 0, 0));
//		data.add(new Data(8, "Hellemyr", "Vegg 5", "Konkav", T.BOLT, "6bb", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(9, "Hellemyr", "Vegg 5", "Tufa", T.BOLT, "6bb. Ja, en tufa i toppen, faktisk.", 1, "8-", "Herman Trosby Nesse", null, 0, 0));
//		data.add(new Data(10, "Hellemyr", "Vegg 5", "Kvantegeologi", T.BOLT, "5bb", 1, "8-", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(1, "Hille", "Jubileumsveggen", "Prosjekt*", T.TRAD, null, 1, "7-", null, null, 0, 0));
//		data.add(new Data(2, "Hille", "Jubileumsveggen", "Spurik", T.TRAD, "Hard start. Fin klatring i diederformasjon.", 1, "7-", "Kjetil Nielsen", "2008-01-01", 0, 0));
//		data.add(new Data(3, "Hille", "Jubileumsveggen", "Sekser-camalot", T.TRAD, "Flott, vedvarende rissformasjoner. Camalot nr 6!!", 1, "5-", "Kjetil Nielsen", "2008-01-01", 0, 0));
//		data.add(new Data(4, "Hille", "Jubileumsveggen", "Lianenes konge", T.TRAD, "Fin-fin klatring. Ikke så vanskelig. Gå et prosjekt!", 1, "6-", null, null, 0, 0));
//		data.add(new Data(1, "Hodnedalen", "Hodnedalen", "Linedanser", T.BOLT, "7bb", 1, "4-", "Jonny Grundeland", "2007-01-01", 0, 0));
//		data.add(new Data(2, "Hodnedalen", "Hodnedalen", "Radio Namnam", T.BOLT, "8bb", 1, "n/a", "Jonny Grundeland", "2007-01-01", 0, 0));
//		data.add(new Data(3, "Hodnedalen", "Hodnedalen", "Frykt ikke (få ræva i gir)", T.MIXED, "4bb", 1, "5-", "Andreas Loland", "1994-01-01", 0, 0));
//		data.add(new Data(4, "Hodnedalen", "Hodnedalen", "Per Melange", T.BOLT, "9bb", 1, "6+", "Per Arvid Lid", "1994-01-01", 0, 0));
//		data.add(new Data(5, "Hodnedalen", "Hodnedalen", "16år og ukysset", T.BOLT, "11bb", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(6, "Hodnedalen", "Hodnedalen", "Sirkus Godzilla", T.BOLT, "9bb", 1, "6-", null, null, 0, 0));
//		data.add(new Data(7, "Hodnedalen", "Hodnedalen", "Henget", T.BOLT, "9bb", 1, "6-", "Andreas Loland", "1994-01-01", 0, 0));
//		data.add(new Data(1, "Holskogen", "Bananveggen", "På den andre siden", T.BOLT, "11bb. Flott balansert klatring. Tre stjerner hvis pusset skikkelig!", 1, "n/a", "Ståle Bjørkestøl", null, 0, 0));
//		data.add(new Data(2, "Holskogen", "Bananveggen", "EU-direktivet", T.MIXED, "Krysser flere ruter, fin mixrute.", 1, "6+", "Ståle Bjørkestøl,Jan A. Bjørkestøl", null, 0, 0));
//		data.add(new Data(3, "Holskogen", "Bananveggen", "Vaffelruta", T.TRAD, null, 1, "6-", "Ståle Bjørkestøl,Jan A. Bjørkestøl", null, 0, 0));
//		data.add(new Data(4, "Holskogen", "Bananveggen", "Hero of The Day", T.BOLT, "8bb", 1, "7+/8-", "Ståle Bjørkestøl,Jan A. Bjørkestøl", null, 0, 0));
//		data.add(new Data(5, "Holskogen", "Bananveggen", "Hero is No Way", T.BOLT, "7bb", 1, "8-", "Ståle Bjørkestøl,Jan A. Bjørkestøl", null, 0, 0));
//		data.add(new Data(6, "Holskogen", "Bananveggen", "Slopy Bastard", T.BOLT, "8bb. Krevende slopere på traversen under/gjennom overhenget.", 1, "8/8+", "Runar Carlsen", null, 0, 0));
//		data.add(new Data(7, "Holskogen", "Bananveggen", "Frykt Forrykt", T.BOLT, "7bb. Klassiker. Den mest populære oppvarmingsruta.", 1, "n/a", "Ståle Bjørkestøl,Jan A. Bjørkestøl", null, 0, 0));
//		data.add(new Data(8, "Holskogen", "Bananveggen", "Caruso", T.BOLT, "9bb. Superfin sjuer. Småkjip i toppen.", 1, "n/a", "Ståle Bjørkestøl,Jan A. Bjørkestøl", null, 0, 0));
//		data.add(new Data(9, "Holskogen", "Bananveggen", "I mine sko", T.BOLT, "7bb. Enda en fin sjuer.", 1, "n/a", "Ståle Bjørkestøl,Jan A. Bjørkestøl", null, 0, 0));
//		data.add(new Data(10, "Holskogen", "Bananveggen", "In absurdum", T.BOLT, "5bb. Litt kortere, men fint likevel.", 1, "7-", "Ståle Bjørkestøl,Jan A. Bjørkestøl", null, 0, 0));
//		data.add(new Data(11, "Holskogen", "Bananveggen", "Troløs", T.BOLT, "5bb. Smådesperat i toppen, men mist ikke troen.", 1, "7+", "Ståle Bjørkestøl,Jan A. Bjørkestøl", null, 0, 0));
//		data.add(new Data(1, "Holskogen", "Dog Wall 1", "Bestefars drøm", T.BOLT, "5bb. Lokalt svennestykke. NB: I toppen belastes en stor hul blokk. Gitt I gave fra Markus til Mike.", 1, "8-", null, null, 0, 0));
//		data.add(new Data(2, "Holskogen", "Dog Wall 1", "Spainich Splits", T.TRAD, null, 1, "n/a", "Mike Tombs", null, 0, 0));
//		data.add(new Data(3, "Holskogen", "Dog Wall 1", "Dire Straits", T.BOLT, "4bb", 1, "7-/7", "Mike Tombs", null, 0, 0));
//		data.add(new Data(4, "Holskogen", "Dog Wall 1", "Mark Goes to Hollywood", T.BOLT, "4bb. Gave fra Mike til Markus da han skulle i militæret.", 1, "6+", "Markus Landrø", null, 0, 0));
//		data.add(new Data(5, "Holskogen", "Dog Wall 1", "Nude i pysjen", T.BOLT, "4bb. kontinuerlig", 1, "7-", "Markus Landrø", null, 0, 0));
//		data.add(new Data(6, "Holskogen", "Dog Wall 1", "Pandoras Box", T.BOLT, "4bb. Nydelig klaringn under tørre forhold.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(7, "Holskogen", "Dog Wall 1", "One Eyed Flie", T.BOLT, "5bb. Bratteste rute på feltet. Spektakulære flytt.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(8, "Holskogen", "Dog Wall 1", "Pancake", T.BOLT, "3bb", 1, "6+", "Mike Tombs", null, 0, 0));
//		data.add(new Data(9, "Holskogen", "Dog Wall 1", "Kneleren", T.TRAD, null, 1, "n/a", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(10, "Holskogen", "Dog Wall 1", "Espen Aseladd", T.TRAD, null, 1, "n/a", "Espen Slemdahl", null, 0, 0));
//		data.add(new Data(11, "Holskogen", "Dog Wall 1", "Lille faen", T.TRAD, null, 1, "n/a", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(12, "Holskogen", "Dog Wall 1", "Myggen", T.BOLT, "2bb", 1, "n/a", "Markus Landrø", null, 0, 0));
//		data.add(new Data(1, "Holskogen", "Dog Wall 2", "Zig Zag", T.BOLT, "6bb. Balansert rute som slanger seg.", 1, "7-", "Mike Tombs", null, 0, 0));
//		data.add(new Data(2, "Holskogen", "Dog Wall 2", "Doggy Style Pumping Action", T.BOLT, "6bb. Balansert og teknisk", 1, "7+", "Markus Landrø", null, 0, 0));
//		data.add(new Data(3, "Holskogen", "Dog Wall 2", "Mierda de perro", T.BOLT, "6bb. Veldig populær. Topp 50!", 1, "6-", "Mike Tombs", null, 0, 0));
//		data.add(new Data(4, "Holskogen", "Dog Wall 2", "Dog Bone", T.BOLT, "7bb", 1, "n/a", "Mike Tombs", null, 0, 0));
//		data.add(new Data(5, "Holskogen", "Dog Wall 2", "Dog Leg", T.BOLT, "5bb", 1, "5+", "Mike Tombs", null, 0, 0));
//		data.add(new Data(6, "Holskogen", "Dog Wall 2", "Dog Fart", T.BOLT, null, 1, "n/a", "Mike Tombs", null, 0, 0));
//		data.add(new Data(7, "Holskogen", "Dog Wall 2", "Talking Dog", T.BOLT, "5bb. Lett når du finner løsningen.", 1, "5-", "Mike Tombs", null, 0, 0));
//		data.add(new Data(8, "Holskogen", "Dog Wall 2", "Tall End", T.BOLT, "4bb", 1, "4+", "Mike Tombs", null, 0, 0));
//		data.add(new Data(9, "Holskogen", "Dog Wall 2", "Doggy bag", T.TRAD, null, 1, "5-", "Mike Tombs", null, 0, 0));
//		data.add(new Data(10, "Holskogen", "Dog Wall 2", "Kjempe", T.TRAD, null, 1, "4+", "Mike Tombs", null, 0, 0));
//		data.add(new Data(11, "Holskogen", "Dog Wall 2", "Kjempefin", T.TRAD, null, 1, "4+", "Mike Tombs", null, 0, 0));
//		data.add(new Data(12, "Holskogen", "Dog Wall 2", "Perro de calljero", T.BOLT, "2bb. Litt lengdeavhengig og vanskelig å definere.", 1, "n/a", "Børre Bergshaven", null, 0, 0));
//		data.add(new Data(1, "Holskogen", "Dynoveggen", "Brødskiva", T.TRAD, "Bruk tre til rappell", 1, "5+", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(2, "Holskogen", "Dynoveggen", "Corner", T.TRAD, "Dieder og stemming hele veien. Velsikret.", 1, "5-", null, null, 0, 0));
//		data.add(new Data(3, "Holskogen", "Dynoveggen", "Outsider", T.TOPROPE, null, 1, "6-", null, null, 0, 0));
//		data.add(new Data(4, "Holskogen", "Dynoveggen", "Blackjack", T.TOPROPE, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(5, "Holskogen", "Dynoveggen", "Jolly Good", T.TRAD, "Fest for fingrene, fest for nervene.", 1, "n/a", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(6, "Holskogen", "Dynoveggen", "Flight of Fancy", T.TRAD, "Fancy jammeriss i starten, en berusende tur.", 1, "6+", null, null, 0, 0));
//		data.add(new Data(7, "Holskogen", "Dynoveggen", "Balckadder", T.TRAD, "Rappell fra tre.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(8, "Holskogen", "Dynoveggen", "Teknikern", T.AIDTRAD, "A1", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(9, "Holskogen", "Dynoveggen", "Cut Direct", T.TRAD, "Krevende både i starten og på slutten.", 1, "7+", "Mike Tombs", null, 0, 0));
//		data.add(new Data(10, "Holskogen", "Dynoveggen", "Cut", T.TRAD, "Krever guts i toppen.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(11, "Holskogen", "Dynoveggen", "Scareface", T.TRAD, "Fantastisk, kjip layback.", 1, "7+", "Mike Tombs", null, 0, 0));
//		data.add(new Data(1, "Holskogen", "Kursveggen", "Hoppla vi lever", T.BOLT, "3bb. Gøyal tur opp til treet som også fungerer som snufeste.", 1, "4+", null, null, 0, 0));
//		data.add(new Data(2, "Holskogen", "Kursveggen", "Kompetansehelvete", T.BOLT, "5bb. Veggens svennestykke.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(3, "Holskogen", "Kursveggen", "Shit la gå", T.BOLT, "4bb", 1, "5-", null, null, 0, 0));
//		data.add(new Data(4, "Holskogen", "Kursveggen", "På tro og lære", T.BOLT, "4bb", 1, "4+", null, null, 0, 0));
//		data.add(new Data(5, "Holskogen", "Kursveggen", "Stramme pupper", T.BOLT, "4bb. Fint asted å starte.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(1, "Holskogen", "Stream Wall", "Pin Ball", T.BOLT, "4bb. Direktestart på Pinch, hard for graden.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Holskogen", "Stream Wall", "Pinch", T.BOLT, "4bb. Artige pincher.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(3, "Holskogen", "Stream Wall", "Animal Thing, prosjekt", T.BOLT, "4bb. Opp mellom rissene, desperat eliminasjon.", 1, "8+", null, null, 0, 0));
//		data.add(new Data(4, "Holskogen", "Stream Wall", "Plastic Jam", T.BOLT, "4bb. Nydelige formasjoner: Jamming og/eller layback", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(5, "Holskogen", "Stream Wall", "Disneyland After Dark", T.BOLT, "6bb. Kontinuerlig.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(6, "Holskogen", "Stream Wall", "Robotman", T.BOLT, "4bb. Stilige flytt.", 1, "6-", null, null, 0, 0));
//		data.add(new Data(7, "Holskogen", "Stream Wall", "Spaceman", T.BOLT, "4bb. Ja, hvis du faller, småkjipt boltet.", 1, "6-", null, null, 0, 0));
//		data.add(new Data(1, "Juvika", "Sjøveggen", "Dvergmåsen", T.BOLT, "7bb. Populært kalt Wang-femmeren.", 1, "6-", "Henning Wang", "2013-01-01", 0, 0));
//		data.add(new Data(2, "Juvika", "Sjøveggen", "Måkeskrik", T.BOLT, "8bb", 1, "7-", "Henning Wang", "2013-01-01", 0, 0));
//		data.add(new Data(3, "Juvika", "Sjøveggen", "Stormåsen", T.BOLT, "6bb. Bolting: OK. Birkeland. Hopp om du kan. Topp 50!", 1, "7+", "Ole Karsten Birkeland", "2013-01-01", 0, 0));
//		data.add(new Data(4, "Juvika", "Sjøveggen", "Ugangsmåka", T.BOLT, "6bb. Lurt å låne noen tak i naboruta til høyre.", 1, "n/a", "Henning Wang", "2013-01-01", 0, 0));
//		data.add(new Data(5, "Juvika", "Sjøveggen", "Snikende tiger, irriterende måke", T.BOLT, "6bb. Litt irriterende rute.", 1, "6+", "Ole Karsten Birkeland", null, 0, 0));
//		data.add(new Data(6, "Juvika", "Sjøveggen", "Måke i motvind", T.BOLT, "5bb. Starter med travers venstre ved et tre.", 1, "n/a", "Henning Wang", "2013-01-01", 0, 0));
//		data.add(new Data(7, "Juvika", "Sjøveggen", "Måke i medvind", T.BOLT, "4bb. Kort og buldrete.", 1, "4-", "Ole Karsten Birkeland", "2013-01-01", 0, 0));
//		data.add(new Data(8, "Juvika", "Skogsveggen", "Uten navn", T.TRAD, "Ta med camalot nr 3", 1, "7+", null, null, 0, 0));
//		data.add(new Data(9, "Juvika", "Skogsveggen", "Kjempemyggen", T.BOLT, "4bb", 1, "6+", "Øystein Engen", "2013-01-01", 0, 0));
//		data.add(new Data(10, "Juvika", "Skogsveggen", "Salt Peter", T.BOLT, "7bb. Prosjekt: Jonny Grundeland", 1, "7-", null, null, 0, 0));
//		data.add(new Data(11, "Juvika", "Skogsveggen", "E-rotic", T.BOLT, "6bb. Kreatør: Jonny Grundeland.", 1, "7-", "Øystein Engen", "2014-01-01", 0, 0));
//		data.add(new Data(12, "Juvika", "Skogsveggen", "Mange muligheter", T.BOLT, "6bb", 1, "6+", "Øystein Engen", "2014-01-01", 0, 0));
//		data.add(new Data(1, "Kilefjorden", "Aggression Wall", "Slip and Slide", T.TRAD, "35m. Vanlig å dele opp i to taulengder. Tre gamle bankebolter i topdiederet bør skiftes ut.", 2, "n/a", "Greg Hall,Allan Price,Craig Offless", null, 0, 0));
//		data.add(new Data(2, "Kilefjorden", "Aggression Wall", "Aggression", T.TRAD, "30m. Klassisk diederlinje. hardest i starten.", 1, "4+", "Graham Drinkwater, Ben Benjamin", null, 0, 0));
//		data.add(new Data(1, "Kilefjorden", "Central Wall", "Odin", T.TRAD, "20m", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Kilefjorden", "Central Wall", "Thor", T.TRAD, "20m. Muligens noe hardere etter at blokk har rast ut.", 1, "5-", null, null, 0, 0));
//		data.add(new Data(3, "Kilefjorden", "Central Wall", "Little Red Monkey", T.TRAD, "40m. Passerer utstikkende blokk og går opp det åpnebare dobbeltrisset.", 1, "n/a", "Ian Leigh", null, 0, 0));
//		data.add(new Data(4, "Kilefjorden", "Central Wall", "Angels Staircase", T.TRAD, "40m. Obs: Passerer utstikkende jernstang.", 1, "n/a", "Ian Leigh,H. Mortimer", null, 0, 0));
//		data.add(new Data(5, "Kilefjorden", "Central Wall", "Allans  Alley", T.TRAD, "35m. To klassiske kaminer passeres underveis.", 1, "n/a", "Tim Taylor", null, 0, 0));
//		data.add(new Data(6, "Kilefjorden", "Central Wall", "Cat People", T.TRAD, "37m. Traverserer ut i hjørnet til høyre etter første del av 'Allans Alley'.", 1, "n/a", "Phil Watkins,Dave Rees", null, 0, 0));
//		data.add(new Data(1, "Kilefjorden", "Highball Wall", "Highball", T.TRAD, "6m. Fin liten test-piece. Gamle slynger i toppankeret bør skiftes ut.", 1, "6-", "Ian Leigh", null, 0, 0));
//		data.add(new Data(1, "Kilefjorden", "Maggot Wall", "Maggot", T.TRAD, "15m. Trekker lit mot høyre i starten, deretter rett opp.", 1, "3+", null, null, 0, 0));
//		data.add(new Data(2, "Kilefjorden", "Maggot Wall", "Consulation", T.TRAD, "15m. Følger den åpenbare renna til høyre for 'Maggot'", 1, "3+", null, null, 0, 0));
//		data.add(new Data(3, "Kilefjorden", "Maggot Wall", "Training Day 1", T.TRAD, "17m. Klassisk kursrute, flere varianter mulig. Mulig å benytte bolter for rappell/standplass.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(4, "Kilefjorden", "Maggot Wall", "Training Day 2", T.TRAD, "17m. Klassisk kursrute, Mulig å benytte bolter for rappell/standplass.", 1, "3+", null, null, 0, 0));
//		data.add(new Data(5, "Kilefjorden", "Maggot Wall", "Initiation", T.TRAD, "15m. Travers over i høyre riss ved 'letterbox'.", 1, "6-", null, null, 0, 0));
//		data.add(new Data(6, "Kilefjorden", "Maggot Wall", "The Viking Traverse", T.TRAD, "Grad 5-7 (avhengig av veivalg). Travers over vannet som runder neset og går inn i liten bukt. jo lavere, desto hardere.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(1, "Kilefjorden", "Overhanging Wall", "Uten navn", T.TRAD, "35m. Gammel teknisk rute med mulig friklatringspotensial. Gamle bolter bør skiftes ut.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Kilefjorden", "Overhanging Wall", "Uten navn", T.TRAD, "35m. Gammel teknisk rute med mulig friklatringspotensial. Gamle bolter bør skiftes ut.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(1, "Kilefjorden", "Slugs Wall", "Diverse ruter", T.TRAD, "Grad 3 til 6. Vegghøyde/klatrelengde 5-8 meter. Fint lite treningsfelt for kurs og kameratredning.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(1, "Kilefjorden", "Southern Shuffle", "Southend Shuffle", T.TRAD, "45m. Populær begynnerrute. Vanlig å avslutte på hylla ved det store treet. Mulig variant inn i og til topps på 'Spider' blir ofte gått.", 1, "n/a", "Lan Leigh", null, 0, 0));
//		data.add(new Data(2, "Kilefjorden", "Southern Shuffle", "Southend Shuffle Direct", T.TRAD, "13m. Det åpenbare diederet rett opp. Avsluttes på hylla.", 1, "n/a", "John May", null, 0, 0));
//		data.add(new Data(7, "Kilefjorden", "Spider Wall", "Diagonal", T.TRAD, "35m", 1, "n/a", "Tim Taylor", null, 0, 0));
//		data.add(new Data(8, "Kilefjorden", "Spider Wall", "Diagonal Direct", T.TRAD, "40m. Brekker rett opp i nisjen ca halvveis oppe.", 1, "4+", null, null, 0, 0));
//		data.add(new Data(9, "Kilefjorden", "Spider Wall", "Spider", T.TRAD, "35m. Mye brukt 'begynnerrute' med godt sikret første del. Flere varianter og standplasser mulig. 4+ ved travers inn i Southend Shuffle etter første taulengde.", 1, "n/a", "Mort Mortimer", null, 0, 0));
//		data.add(new Data(1, "Kilefjorden", "Super Wall", "Drop Out", T.TRAD, "40m. Passerer skarpskårent overheng og følger deretter rynt riss i dieder til toppen.", 1, "6+/7-", "Allan Price,Trudie Mitchell,Graham Drinkwater", null, 0, 0));
//		data.add(new Data(2, "Kilefjorden", "Super Wall", "A: Nutmeg", T.TRAD, "30m. Rett opp diederet og ut til h øyre langs riss i toppen.", 1, "n/a", "Tim Taylor", null, 0, 0));
//		data.add(new Data(3, "Kilefjorden", "Super Wall", "B: Nutmeg Direct", T.TRAD, "30m. Følger diederet hele veien til topps. noe bøssete.", 1, "6-", "Haydn Thomas", null, 0, 0));
//		data.add(new Data(4, "Kilefjorden", "Super Wall", "Super Wall", T.TRAD, "30m. Brutal start, deretter opp riss, før travers inn i senter av veggen og så rett opp. Tynt sikret. Gamle bankebolter som bør erstattes.", 1, "7-/7", "Greg Hall,Paul Rogers", null, 0, 0));
//		data.add(new Data(5, "Kilefjorden", "Super Wall", "A: Spex 13", T.TRAD, "30m. Klassisk overhengende hjørne, godt sikret.", 1, "6-", "Henry Day,Mort Mortimer", null, 0, 0));
//		data.add(new Data(6, "Kilefjorden", "Super Wall", "B : Spex 13 Right", T.TRAD, "30m. Litt mer pumpende variant. Noe tynnere sikret.", 1, "6-/6", null, null, 0, 0));
//		data.add(new Data(1, "Kilefjorden", "Traffic Wall", "Sinister Gully", T.TRAD, "40m. Grusserenne, tidliere brukt til utsatt retur.", 1, "n/a", "Tim Taylor", null, 0, 0));
//		data.add(new Data(2, "Kilefjorden", "Traffic Wall", "Uten navn", T.TRAD, "40m. Hard start, muligens ikke gått i fri men omgått med innsteg fra Sinister gully.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(3, "Kilefjorden", "Traffic Wall", "Sinister Crack", T.TRAD, "40m. Off width grufseriss. Ofte våt.", 1, "n/a", "Ian Leigh", null, 0, 0));
//		data.add(new Data(4, "Kilefjorden", "Traffic Wall", "Walls End", T.TRAD, "40m. Diagonal linje gjennom hele veggen. Gamle bankebolter må muligens erstates.", 1, "6-", "Lan Leigh", null, 0, 0));
//		data.add(new Data(5, "Kilefjorden", "Traffic Wall", "B: Traffic Direct", T.TRAD, "42m. Rett opp venstre risset. Noe tynt sikret i toppen.", 1, "n/a", "Phil Watkins", null, 0, 0));
//		data.add(new Data(5, "Kilefjorden", "Traffic Wall", "A: Traffic", T.TRAD, "42m. Travers over i høyre riss ved 'letterbox'", 1, "6-", "Phil Watkins,Dave Rees", null, 0, 0));
//		data.add(new Data(6, "Kilefjorden", "Traffic Wall", "Narcotic", T.TRAD, "42m. Noe tynt sikret. Gamle bankebolter må muligens erstattes.", 1, "6+/7-", "Greg Hall,Alan Richards,Haydn Thomas", null, 0, 0));
//		data.add(new Data(7, "Kilefjorden", "Traffic Wall", "Space Walk", T.TRAD, "50m. Gås i to taulengder med standplass på hylle. Tynt sikret i starten.", 2, "6-/6", "M. Holman", null, 0, 0));
//		data.add(new Data(8, "Kilefjorden", "Traffic Wall", "Cornish Rhapsody", T.TRAD, "45m. Gås i to taulengder med stand på hylle. Tynt sikret i starten.", 2, "6-/6", "Lan Leigh", null, 0, 0));
//		data.add(new Data(9, "Kilefjorden", "Traffic Wall", "Glutton", T.TRAD, "45m. Går i to taulengder med stand på hylle. Tynt sikret i første taulengde og muligens noe hardere i starten etter at ei blokk har rast ut.", 2, "n/a", "Greg Hall,Paul Rogers,M. Holman,Len Atkinson,D. Howie", null, 0, 0));
//		data.add(new Data(10, "Kilefjorden", "Traffic Wall", "Think Again", T.TRAD, "17m. Svært marginalt sikret egg til venstre for 'Easter crack'", 1, "n/a", "S. Samson", null, 0, 0));
//		data.add(new Data(11, "Kilefjorden", "Traffic Wall", "Easter Crack", T.TRAD, "22m. Dieder. Ruta kan også avsluttes på hylla litt over halvveis med rappell fra tre der 'Think again' topper ut.", 1, "5+", null, null, 0, 0));
//		data.add(new Data(12, "Kilefjorden", "Traffic Wall", "uten navn", T.TRAD, "23m. Opprinnelig teknisk rute med friklatringspotensiale. En bankebolt står igjen. Avslutter som 'Easter Crack'", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(13, "Kilefjorden", "Traffic Wall", "A: K2 Corner Direct", T.TRAD, "20m. Bratt med løse blokker i toppen.", 1, "6-", "Ian Leigh", null, 0, 0));
//		data.add(new Data(13, "Kilefjorden", "Traffic Wall", "B: K2 Corner", T.TRAD, "27m. Travers over i høyre riss ved 'Letterbox'.", 1, "4+", "Lan Leigh", null, 0, 0));
//		data.add(new Data(14, "Kilefjorden", "Traffic Wall", "Plymsoll", T.TRAD, "27m. Flott klaring i diederet, skummel traversering med dårlig sikring i toppen: Først mot høyre og så tilbake igjen mot venstre før mantling opp på hylla.", 1, "6+", "Greg Hall,Paul Rogers,Len Atkinson", null, 0, 0));
//		data.add(new Data(15, "Kilefjorden", "Traffic Wall", "New Era", T.TRAD, "26m. Tilnærmet usikret direktevariant til  høyre for diederet på 'Plymsoll'. Avslutter som Plymsoll, men med enda mer marginale sikringer!", 1, "n/a", "M. Holman,G. Sheppard", null, 0, 0));
//		data.add(new Data(16, "Kilefjorden", "Traffic Wall", "Root", T.TRAD, "Tynt riss som er OK sikret med minikiler og små kammer. Enklere i toppen.", 1, "n/a", "Greg Hall,Graham Drinkwater,Graham Bond", null, 0, 0));
//		data.add(new Data(17, "Kilefjorden", "Traffic Wall", "Flea", T.TRAD, "20m. Trapperenne, deler toppen med 'Root'.", 1, "4+", "Tim Taylor", null, 0, 0));
//		data.add(new Data(13, "Kjos", "Bautasteinen", "Venstreradikalen", T.TRAD, "Kort skiveriss på kanten, ikke optimalt sikret.", 1, "5-", null, null, 0, 0));
//		data.add(new Data(14, "Kjos", "Bautasteinen", "Demonens vals", T.BOLT, "2bb. Fingerkiller, husk oppvarming.", 1, "7-", null, null, 0, 0));
//		data.add(new Data(15, "Kjos", "Bautasteinen", "Striks med triks", T.BOLT, "2bb. Skjulte nøkkeltak.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(1, "Kjos", "Innerste sektor", "Lynkurset", T.TRAD, "Variert klatring.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Kjos", "Innerste sektor", "De varige verdier", T.BOLT, "5bb. Bølgende fjell med lister og små pincher", 1, "5+", null, null, 0, 0));
//		data.add(new Data(3, "Kjos", "Innerste sektor", "Lambada", T.BOLT, "6bb. Nydelig rute med smygende bevegelser.", 1, "5+", null, null, 0, 0));
//		data.add(new Data(4, "Kjos", "Innerste sektor", "Vertikalt menasjeri", T.BOLT, "4bb. Stilig avslutning", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(5, "Kjos", "Innerste sektor", "Bom, du er hypnotisert", T.BOLT, "3bb. Fascinerende. enklere enn det kan se ut som.", 1, "6+", null, null, 0, 0));
//		data.add(new Data(6, "Kjos", "Innerste sektor", "Først betatt, så besatt", T.BOLT, "5bb. Elske eller hate den? Gå dynamisk på små slappe lister.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(7, "Kjos", "Innerste sektor", "Fotogjenger", T.TRAD, "Snu i tre eller i ruta til venstre.", 1, "n/a", "John Amund Lund", null, 0, 0));
//		data.add(new Data(8, "Kjos", "Innerste sektor", "Noen ganger er det ålreit", T.TRAD, "Vrient minioverheng.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(9, "Kjos", "Innerste sektor", "Lister for franske turister", T.BOLT, "2bb. Mange merkelige løsninger. Kort rute.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(10, "Kjos", "Innerste sektor", "Vertikal yoga", T.BOLT, "2bb. Balanse og fotarbeid!", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(11, "Kjos", "Innerste sektor", "Risset med det rare i", T.TRAD, "Kort, dyp naturlig sikret sprekk.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(12, "Kjos", "Innerste sektor", "Live and Let Die", T.TOPROPE, "Krystallisk midtparti.", 1, "5+", null, null, 0, 0));
//		data.add(new Data(1, "Kjos", "LOS-veggen", "Solois(t)", T.BOLT, "7bb. Førstebesteget solo. Gode tak og hyller.", 1, "3+", "Stig Atle Steffensen", "2014-01-01", 0, 0));
//		data.add(new Data(2, "Kjos", "LOS-veggen", "Sabeltann-is", T.BOLT, "8bb. Samme toppsekvens som Solois(t).", 1, "n/a", "Anne Kari Skarpodde", "2014-01-01", 0, 0));
//		data.add(new Data(2, "Kjos", "LOS-veggen", "Grønt batteri", T.BOLT, "9bb. Starter i rute 2 og traverserer ut i rute 3.", 1, "n/a", "Nils Rune Birkeland", "2014-01-01", 0, 0));
//		data.add(new Data(3, "Kjos", "LOS-veggen", "Grønn energi", T.BOLT, "9bb. Finn de gode takene rett over startbulken - ikke gå til høyre.", 1, "4+", "Anne Kari Skarpodde", "2014-01-01", 0, 0));
//		data.add(new Data(4, "Kjos", "LOS-veggen", "Softis", T.BOLT, "5bb", 1, "4+", "Anne Kari Skarpodde", "2014-01-01", 0, 0));
//		data.add(new Data(5, "Kjos", "LOS-veggen", "Los-ruta", T.BOLT, "8bb. Et luftig parti med gode tak, artig klatring på denne graden!", 1, "4+", "Kjetil Grønskag", "2014-01-01", 0, 0));
//		data.add(new Data(6, "Kjos", "LOS-veggen", "Easy-is", T.BOLT, "5bb. Gode tak, ruta slutter før du aner det.", 1, "4+", "Ingunn Trosby", "2014-01-01", 0, 0));
//		data.add(new Data(7, "Kjos", "LOS-veggen", "Sørlandsis", T.BOLT, "Plutselig ikke bare gode tak.", 1, "5-", "Kjetil Grønskag", "2014-01-01", 0, 0));
//		data.add(new Data(1, "Laksefeltet", "Hovedveggen", "Hva skal barnet hete?", T.BOLT, null, 1, "7-", "Nils Rune Birkeland", "2003-01-01", 0, 0));
//		data.add(new Data(2, "Laksefeltet", "Hovedveggen", "Brudevalsen", T.BOLT, null, 1, "7/7+", "Nils Rune Birkeland", "2003-01-01", 0, 0));
//		data.add(new Data(3, "Laksefeltet", "Hovedveggen", "Syndige tanker", T.BOLT, "Delikate løsninger og lekkert fjell.", 1, "7+/8-", "Nils Rune Birkeland", "2003-01-01", 0, 0));
//		data.add(new Data(4, "Laksefeltet", "Hovedveggen", "Pillarguri", T.BOLT, null, 1, "6+", "Nils Rune Birkeland", "2003-01-01", 0, 0));
//		data.add(new Data(5, "Laksefeltet", "Hovedveggen", "Laksetrappa", T.BOLT, "Fin oppvarming", 1, "n/a", "Nils Rune Birkeland", "2003-01-01", 0, 0));
//		data.add(new Data(6, "Laksefeltet", "Hovedveggen", "Tiden det tar", T.BOLT, null, 1, "7/7+", "Marit Widding", "2003-01-01", 0, 0));
//		data.add(new Data(7, "Laksefeltet", "Hovedveggen", "Tiden denne tar er dobbelt så lang", T.BOLT, null, 1, "7+", "Nils Rune Birkeland", "2003-01-01", 0, 0));
//		data.add(new Data(8, "Laksefeltet", "Hovedveggen", "Navnetyven", T.BOLT, null, 1, "7+/8-", "Hugo Hermansen", "2003-01-01", 0, 0));
//		data.add(new Data(9, "Laksefeltet", "Hovedveggen", "Vesterled", T.BOLT, null, 1, "n/a", "Kim Kjærgård", null, 0, 0));
//		data.add(new Data(10, "Laksefeltet", "Hovedveggen", "Vestavind", T.BOLT, null, 1, "7+/8-", "Kim Kjærgård", null, 0, 0));
//		data.add(new Data(11, "Laksefeltet", "Hovedveggen", "Navnekverulanten", T.BOLT, null, 1, "7+", "Nils Rune Birkeland", "2003-01-01", 0, 0));
//		data.add(new Data(12, "Laksefeltet", "Hovedveggen", "Fjells gylne fallskjerm", T.BOLT, null, 1, "6+", "Nils Rune Birkeland", "2003-01-01", 0, 0));
//		data.add(new Data(13, "Laksefeltet", "Hovedveggen", "Bergensersvaet", T.BOLT, "Er det sva, eller?", 1, "7+", "Kim Kjærgård", "2003-01-01", 0, 0));
//		data.add(new Data(1, "Laksefeltet", "Skogklippen", "Lommelerka", T.BOLT, null, 1, "7+", "Øyvind Tangen Ødegaard", "2005-01-01", 0, 0));
//		data.add(new Data(2, "Laksefeltet", "Skogklippen", "Salige er de som børster", T.BOLT, null, 1, "6+", "Øyvind Tangen Ødegaard", null, 0, 0));
//		data.add(new Data(3, "Laksefeltet", "Skogklippen", "Vis sprekkdannelse", T.TRAD, null, 1, "6-", "Øyvind Tangen Ødegaard", null, 0, 0));
//		data.add(new Data(4, "Laksefeltet", "Skogklippen", "Vielse uten sikring", T.TRAD, null, 1, "5+", "Øyvind Tangen Ødegaard", null, 0, 0));
//		data.add(new Data(5, "Laksefeltet", "Skogklippen", "Som en klisje", T.BOLT, null, 1, "4+", "Tove Anette Sandel Hepburn", "2004-01-01", 0, 0));
//		data.add(new Data(6, "Laksefeltet", "Skogklippen", "Søvndeprivert", T.BOLT, null, 1, "n/a", "Tove Anette Sandel Hepburn", "2004-01-01", 0, 0));
//		data.add(new Data(9, "Louisesvei", "Høyre sektor", "Tora", T.TRAD, "Kort tur på vesleveggen", 1, "n/a", "Nils Rune Birkeland", "2013-01-01", 0, 0));
//		data.add(new Data(10, "Louisesvei", "Høyre sektor", "Ingrid", T.TRAD, "Litt tynt i starten, Nydelig fjell.", 1, "5+/6-", "John Amund Lund", "2013-01-01", 0, 0));
//		data.add(new Data(11, "Louisesvei", "Høyre sektor", "Margret", T.TRAD, "NB. Toppfeste i tre.", 1, "5-", "John Amund Lund", "2013-01-01", 0, 0));
//		data.add(new Data(12, "Louisesvei", "Høyre sektor", "Ingebjørg", T.TRAD, "Crux forbi første bulken.", 1, "5+", "Janne Lund", "2013-01-01", 0, 0));
//		data.add(new Data(13, "Louisesvei", "Høyre sektor", "Malmfrid", T.TRAD, "Trøblete sikret i starten. Toppfeste i tre.", 1, "6+", "Adam Renshaw", "2013-01-01", 0, 0));
//		data.add(new Data(14, "Louisesvei", "Høyre sektor", "Kristin", T.MIXED, "2bb. Miks med to borebolter.", 1, "n/a", "Nils Rune Birkeland", "2013-01-01", 0, 0));
//		data.add(new Data(15, "Louisesvei", "Høyre sektor", "Ingerid", T.MIXED, "Miks med en borebolt", 1, "7-", "Nils Rune Birkeland", "2013-01-01", 0, 0));
//		data.add(new Data(1, "Louisesvei", "Midtre sektor", "Gyda", T.TRAD, "Vær obspå langt fallpotensiale i toppen.", 1, "6+", "Pär Landmark", "2013-01-01", 0, 0));
//		data.add(new Data(2, "Louisesvei", "Midtre sektor", "Ragnhild", T.TRAD, "Innbydende, tydelig riss.", 1, "6-", "Adam Renshaw", "2013-01-01", 0, 0));
//		data.add(new Data(3, "Louisesvei", "Midtre sektor", "Gunhild", T.TRAD, "Friskt steg fra diederet og ut i veggen, men greit sikret.", 1, "n/a", "Nils Rune Birkeland", "2013-01-01", 0, 0));
//		data.add(new Data(4, "Louisesvei", "Midtre sektor", "Gudrun", T.TRAD, "Kort riss med klassisk jamming.", 1, "6-", "Nils Rune Birkeland", "2013-01-01", 0, 0));
//		data.add(new Data(5, "Louisesvei", "Midtre sektor", "Tyra, åpent prosjekt", T.MIXED, "Kreatør: Nils Rune Birkeland", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(6, "Louisesvei", "Midtre sektor", "Astrid", T.TRAD, "Lang, vandrende rute.", 1, "n/a", "Nils Rune Birkeland", "2013-01-01", 0, 0));
//		data.add(new Data(7, "Louisesvei", "Midtre sektor", "Emma", T.TRAD, "En litt tøffere toppvariant til Ellisiv.", 1, "6+/7-", "Nils Rune Birkeland", "2013-01-01", 0, 0));
//		data.add(new Data(8, "Louisesvei", "Midtre sektor", "Ellisiv", T.TRAD, "Bratt klassiker, flott jamming og layback.", 1, "6+", "Nils Rune Birkeland", "2013-01-01", 0, 0));
//		data.add(new Data(9, "Louisesvei", "Vesle sektor", "Tora", T.TRAD, "Kort tur på Vesleveggen.", 1, "n/a", "Nils Rune Birkeland", "2013-01-01", 0, 0));
//		data.add(new Data(1, "Modalshorn", "Eskedalshelleren", "Via Ferrata", T.TOPROPE, "50 meter lett og fin via-ferrata. Bruk via-ferrata utstyr.", 1, "n/a", "Børre Bergshaven", null, 0, 0));
//		data.add(new Data(2, "Modalshorn", "Eskedalshelleren", "Prosjekt", T.BOLT, "9bb. 9 x 4 bolter på 2 taulengder.", 2, "n/a", null, null, 0, 0));
//		data.add(new Data(3, "Modalshorn", "Eskedalshelleren", "Tauruta", T.BOLT, "5bb. Starter ved å gå opp tau på jumar. NB:Gammelt tau.", 1, "n/a", "Henning Wang", null, 0, 0));
//		data.add(new Data(4, "Modalshorn", "Eskedalshelleren", "Tredjeruta", T.BOLT, "5bb. Tekniske flytt.", 1, "6+", "Henning Wang", null, 0, 0));
//		data.add(new Data(5, "Modalshorn", "Eskedalshelleren", "Skog og fjell", T.BOLT, "10bb. Kul fjellrute. Store formasjoner og variert klatring.", 1, "n/a", "Henning Wang", null, 0, 0));
//		data.add(new Data(6, "Modalshorn", "Eskedalshelleren", "Ikke bare spade", T.BOLT, "6bb. Store bøttetak.", 1, "5-", "Henning Wang", null, 0, 0));
//		data.add(new Data(7, "Modalshorn", "Eskedalshelleren", "Slipp unna", T.BOLT, "7bb. Tung start, så lettere. OBS: løs blokk til høyre mot toppen.", 1, "6-/6", "Henning Wang", null, 0, 0));
//		data.add(new Data(8, "Modalshorn", "Eskedalshelleren", "Slapp", T.BOLT, "4bb. Slappe tak og slappe riss. Tung start.", 1, "6-/6", "Henning Wang", null, 0, 0));
//		data.add(new Data(9, "Modalshorn", "Eskedalshelleren", "Strunk", T.BOLT, "4bb. Små tak eller lange flytt.", 1, "n/a", "Henning Wang", null, 0, 0));
//		data.add(new Data(10, "Modalshorn", "Eskedalshelleren", "Bongo", T.BOLT, "5bb. Buldrete og tung for graden, men meget gode tak.", 1, "5-", "Henning Wang", null, 0, 0));
//		data.add(new Data(11, "Modalshorn", "Eskedalshelleren", "Zenbudist", T.BOLT, "6bb. ", 1, "7", null, null, 0, 0));
//		data.add(new Data(12, "Modalshorn", "Eskedalshelleren", "Elfnudist", T.BOLT, "5bb", 1, "7", null, null, 0, 0));
//		data.add(new Data(13, "Modalshorn", "Eskedalshelleren", "Navnløs", T.BOLT, "5bb. Ruta helt til høyre.", 1, "7", null, null, 0, 0));
//		data.add(new Data(1, "Modalshorn", "Hovedvegg", "Tommys regnfrakk", T.BOLT, "4bb. Fin tur på lister, sloper og riss.", 1, "n/a", "Børre Bergshaven", null, 0, 0));
//		data.add(new Data(2, "Modalshorn", "Hovedvegg", "Knallhardt", T.BOLT, "5bb. Hard tur på lister, sloper og i riss.", 1, "7+", "Børre Bergshaven", null, 0, 0));
//		data.add(new Data(3, "Modalshorn", "Hovedvegg", "Tommys hanske", T.BOLT, "7bb. Kjempefin klatring, høyre eller venstre i toppen?", 1, "n/a", "Børre Bergshaven", null, 0, 0));
//		data.add(new Data(4, "Modalshorn", "Hovedvegg", "Tommys boblejakke", T.BOLT, "8bb. Fin klatring. Lange boltestrekk.", 1, "8-", "Børre Bergshaven", null, 0, 0));
//		data.add(new Data(5, "Modalshorn", "Hovedvegg", "Modalshorn", T.BOLT, "12bb. En fantastisk rute. Borret av Børre bergshaven.", 1, "8/8+", "Stefan Landrø", null, 0, 0));
//		data.add(new Data(6, "Modalshorn", "Hovedvegg", "Prosjekt", T.BOLT, "6bb. Kreatør: Børre Bergshaven", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(7, "Modalshorn", "Hovedvegg", "Modalsfanden", T.BOLT, "6bb. Superklassiker. Layback", 1, "9-/9", "Runar Carlsen", null, 0, 0));
//		data.add(new Data(1, "Modalshorn", "Stillehavsveggen", "Stillehavsruta", T.TRAD, "En fin sprekk på vegg med to mulige startvarianter.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Modalshorn", "Stillehavsveggen", "Navn", T.TRAD, "Diederet.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(3, "Modalshorn", "Stillehavsveggen", "Navn", T.TRAD, "Naturlig riss til høyre.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(4, "Modalshorn", "Stillehavsveggen", "Inne i dalen", T.BOLT, "3bb. Kort rute, gå 150 meter videre innover i dalen.", 1, "n/a", "Johannes Kjevik", null, 0, 0));
//		data.add(new Data(1, "Paulen", "Trollfjellet", "Splitter pine", T.TRAD, "Åpent prosjekt. Antatt grad 5+.", 2, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Paulen", "Trollfjellet", "Åpent prosjekt", T.MIXED, "2c Åpent prosjekt, Utstegsvariant på ca. 15 meter. Naturlig sikret, men med behov for noen bolter.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Paulen", "Trollfjellet", "Trollsplinten", T.TRAD, "2b Lukket prosjekt. Utstegsvariant på ca. 15 meter.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Paulen", "Trollfjellet", "Trollsplitter", T.TRAD, "2a Naturlig sikret kongelinje! Grad taulengder: 5+,6-", 2, "6-", "Nils Rune Birkeland,John Amund Lund", "2014-01-01", 0, 0));
//		data.add(new Data(3, "Paulen", "Trollfjellet", "Trolljegeren", T.TRAD, "Lukket prosjekt", 3, "n/a", null, null, 0, 0));
//		data.add(new Data(4, "Paulen", "Trollfjellet", "Troll av eske", T.BOLT, "Boreboltet med snufeste i toppen. Ukjent grad.", 2, "n/a", null, null, 0, 0));
//		data.add(new Data(5, "Paulen", "Trollfjellet", "Sporveksleren aka 'Trollspeilet'", T.TRAD, "Naturlig sikret drøm eller mareritt? Anbefales uansett på det sterkeste! Grad taulengder: 6+,7-", 2, "7-", "Nils Rune Birkeland", "2014-01-01", 0, 0));
//		data.add(new Data(6, "Paulen", "Trollfjellet", "Lukket prosjekt", T.TRAD, "Lukket prosjekt. Naturlig sikret off-width!", 2, "n/a", null, null, 0, 0));
//		data.add(new Data(1, "Ravnedalen", "Bånetjønn", "Great Arête", T.BOLT, "10bb", 1, "7-", null, null, 0, 0));
//		data.add(new Data(2, "Ravnedalen", "Bånetjønn", "Av plast er du kommet til stein skal du bli", T.BOLT, "6bb. La deg ikke forlede av gamle bolter brukt til fjellsikring til venstre for linja.", 1, "6+/7-", null, null, 0, 0));
//		data.add(new Data(3, "Ravnedalen", "Bånetjønn", "Sandells dieder", T.BOLT, "5bb. Bolterute nr 2 langs stien. Stopper midt oppe i veggen.", 1, "7-", "Jan A. Bjørkestøl", null, 0, 0));
//		data.add(new Data(4, "Ravnedalen", "Bånetjønn", "Psycho Machine", T.BOLT, "5bb. Det slappe diederet etter at du har passert maltraktert boreboltet rute i det bratteste partiet langs stien. Noe høy førstebolt.", 1, "7+", null, null, 0, 0));
//		data.add(new Data(5, "Ravnedalen", "Bånetjønn", "Skråplanet", T.BOLT, "3bb. Gammel testpiece. Opp det bratte henget over platået i stien rett nedenfor dammen på Bånetjønn.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(6, "Ravnedalen", "Bånetjønn", "Nøtte", T.TRAD, "Kort lite riss lengst til høyre.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(9, "Ravnedalen", "Black Wall", "The Spell", T.BOLT, "8bb. Ble du forhekset?", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(10, "Ravnedalen", "Black Wall", "Crack", T.BOLT, "15bb (40m+). Kan med fordel gjøres som to taulengder med standplass på hylla.", 1, "7-", null, null, 0, 0));
//		data.add(new Data(1, "Ravnedalen", "Blank Wall", "Child´s play 1", T.TRAD, "Kort rute helt til venstre på nedre del av veggen.", 1, "4+", null, null, 0, 0));
//		data.add(new Data(2, "Ravnedalen", "Blank Wall", "Child´s play 2", T.BOLT, "4bb. Kort rute. Første del av 'Wish You Were Here'.", 1, "4-", null, null, 0, 0));
//		data.add(new Data(3, "Ravnedalen", "Blank Wall", "Wish You Were Here", T.BOLT, "Tre taulengder, 4+, 6, 7- på hhv. 25, 35 og 15 meter", 3, "7-", null, null, 0, 0));
//		data.add(new Data(4, "Ravnedalen", "Blank Wall", "Thank God You Are Not", T.BOLT, "12bb (35m). Høyrevariant av andretaulengden på rute 3.", 1, "6-", null, null, 0, 0));
//		data.add(new Data(5, "Ravnedalen", "Blank Wall", "Veslefrikk", T.BOLT, "6bb", 1, "6+", null, null, 0, 0));
//		data.add(new Data(6, "Ravnedalen", "Blank Wall", "British Boldness", T.BOLT, "6bb. Kraftflytt i overhenget og et lite run i toppen.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(1, "Ravnedalen", "Caféveggen (stengt/closed)", "Café 1 (stengt/closed)", T.TRAD, "Høyrevarianten er tynt sikret i starten.", 1, "7-", null, null, 0, 0));
//		data.add(new Data(2, "Ravnedalen", "Caféveggen (stengt/closed)", "Café 2 (stengt/closed)", T.TRAD, "Tynt sikret i starten, pump i toppen.", 1, "6+", null, null, 0, 0));
//		data.add(new Data(3, "Ravnedalen", "Caféveggen (stengt/closed)", "Café 3 (stengt/closed)", T.TRAD, "Vedvarende i toppen.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(1, "Ravnedalen", "Diamond Wall", "Inbetween", T.MIXED, "4bb. 2 taulengder, begge 7-", 2, "7-", null, null, 0, 0));
//		data.add(new Data(2, "Ravnedalen", "Diamond Wall", "Crack Down", T.MIXED, "2bb. Starter fra hylla etter 1. taulengde av 'Inbetween'.", 1, "7+", null, null, 0, 0));
//		data.add(new Data(3, "Ravnedalen", "Diamond Wall", "Crack Down Direct", T.MIXED, "2bb. Direktevariant rett opp risset fra Crack down.", 1, "8-", null, null, 0, 0));
//		data.add(new Data(4, "Ravnedalen", "Diamond Wall", "Old Spice", T.MIXED, "3bb. Begge taulengder er på ca. 20 meter. Hvis du ruta i ett, når du likevel bakken ved nedfiring med et 70-meterstau.", 2, "n/a", null, null, 0, 0));
//		data.add(new Data(4, "Ravnedalen", "Diamond Wall", "4a Spicy", T.TRAD, "Høyrevariant ut på eggen i starten av 2. taulengde.", 1, "6+", null, null, 0, 0));
//		data.add(new Data(5, "Ravnedalen", "Hanging Wall", "Mørkets makt. Prosjekt", T.TRAD, "Opp det venstre av de bratte rissene. Kryss det åpne diederet og traverser inn i renne som leder til svahylle med rappell fra slynge i eiketre. Antatt grad 8.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(7, "Ravnedalen", "Hanging Wall", "Troika. Prosjekt", T.TRAD, "Drømmeprosjektet. Antatt grad 7+ (25m), A2 (20m) og 8- (25m). Første taulengde opp til snufestet på rute 8 (Tross alt) kan gås som egen rute. 7a er alternativ 2. taulengde.", 1, "7+?", null, null, 0, 0));
//		data.add(new Data(8, "Ravnedalen", "Hanging Wall", "Tross alt. Prosjekt", T.TRAD, "Går rett opp til snufestet. Antatt grad 8-.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(4, "Ravnedalen", "Main Wall (stengt/closed)", "Keys to Imagination (stengt/closed)", T.BOLT, "Vedvarende i toppen. Begge taulengder 7+", 2, "7+", null, null, 0, 0));
//		data.add(new Data(5, "Ravnedalen", "Main Wall (stengt/closed)", "Fingertips (stengt/closed)", T.BOLT, "7bb. Vedvarende i toppen.", 1, "8-/8", null, null, 0, 0));
//		data.add(new Data(1, "Ravnedalen", "Meny Wall", "Generalen Spesial", T.BOLT, "8bb. Litt psykende mot toppen.", 1, "6+", null, null, 0, 0));
//		data.add(new Data(2, "Ravnedalen", "Meny Wall", "Oscar", T.BOLT, "7bb. Mest sva og smygeklatring.", 1, "6-", "Ingunn Trosby", null, 0, 0));
//		data.add(new Data(3, "Ravnedalen", "Meny Wall", "Sterke Oscar", T.BOLT, "11bb. Den barske forlengelsen av Oscar.", 1, "n/a", "Tom Egil Rosseland", null, 0, 0));
//		data.add(new Data(4, "Ravnedalen", "Meny Wall", "Blueberry Hill", T.BOLT, "8bb. Grisehale i toppen.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(5, "Ravnedalen", "Meny Wall", "Reker i en haug", T.BOLT, "10bb. Ikke særlig lett femmer.", 1, "5+", "Linett Eriksen Birkeland", null, 0, 0));
//		data.add(new Data(6, "Ravnedalen", "Meny Wall", "Tikas favoritt", T.BOLT, "7bb. Ikke så soft denne heller.", 1, "n/a", "Steffan Nilsen", null, 0, 0));
//		data.add(new Data(6, "Ravnedalen", "Overlapping Wall (stengt/closed)", "Slit (stengt/closed)", T.TRAD, "2 taulengder; 5+, 6- på hhv. 20 og 50 meter. Første taulengde felles med Drain Pipe.", 2, "6-", null, null, 0, 0));
//		data.add(new Data(7, "Ravnedalen", "Overlapping Wall (stengt/closed)", "Drain Pipe (stengt/closed)", T.TRAD, "Klassisk håndjamming gjennom cruxet i andre taulengde. 2 taulengder; 5+, 6- på hhv. 20 og 50 meter.", 2, "6-", null, null, 0, 0));
//		data.add(new Data(8, "Ravnedalen", "Overlapping Wall (stengt/closed)", "Big K (stengt/closed)", T.TRAD, "2 taulengder; 5+, 6- på hhv. 20 og 50 m, første taulengde felles med Drain Pipe. 2. taulengde mangler bolt i henget . Tryggest med travers inn i Drain Pipe her.", 2, "6-", null, null, 0, 0));
//		data.add(new Data(7, "Ravnedalen", "Prow & Gully Wall", "Prow Direct", T.BOLT, "9bb. Starter på hylla, adkomst via første del av Edge. Rappellretur ned Edge.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(8, "Ravnedalen", "Prow & Gully Wall", "Edge", T.BOLT, "12bb. Klassisk, flott ravnedalstur som ender på hylla 30 meter over bakken.", 1, "6+", null, null, 0, 0));
//		data.add(new Data(9, "Ravnedalen", "Prow & Gully Wall", "Edge Direct", T.BOLT, "12bb. Åpent prosjekt. Følger boltelinja til Edge slavisk gjennom den første lille overlappen. Åpent prosjekt.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(10, "Ravnedalen", "Prow & Gully Wall", "Inside Out", T.BOLT, "12bb. Flott rute som bør gjøres på egne sikringer. Trenger jevnlig rensing.", 1, "5-", null, null, 0, 0));
//		data.add(new Data(11, "Ravnedalen", "Red Wall", "Red Wall Direct", T.BOLT, "2 taulengder 8-, 8+/9-", 2, "8+/9-", null, null, 0, 0));
//		data.add(new Data(12, "Ravnedalen", "Red Wall", "Block", T.MIXED, "Tøff liten nøtt.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(13, "Ravnedalen", "Red Wall", "Red Wall. Prosjekt", T.MIXED, "2bb. Antatt grad 7. Fortsettelsen av Block.", 1, "7", null, null, 0, 0));
//		data.add(new Data(14, "Ravnedalen", "Red Wall", "Red Wall Crack. Prosjekt", T.MIXED, "2bb. Antatt grad 8-. Direktevariant av Red Wall, opp risset. Skarpt fjell.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(15, "Ravnedalen", "Red Wall", "Flash Down", T.BOLT, "7bb. Fine flytt, noen litt dynamiske.", 1, "7-", null, null, 0, 0));
//		data.add(new Data(16, "Ravnedalen", "Red Wall", "Flash Up", T.BOLT, "8bb. Litt vandrende rute med et sting i toppen.", 1, "7-", null, null, 0, 0));
//		data.add(new Data(1, "Skråstad", "Skråstad", "Hjørnetann", T.TRAD, "Flott dieder. Standard rack med kiler og friends. Grad taulengder: 5+,6,4", 3, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Skråstad", "Skråstad", "Var jeg bare en apekatt", T.TRAD, "Variert og utfordrende. Vanskelig å sikre cruxet. Grad taulengder: 6,7,4+", 3, "n/a", null, null, 0, 0));
//		data.add(new Data(3, "Skråstad", "Skråstad", "Mørkt når det gjelder", T.TRAD, "Mest sva, men også en bratt taulengde. Ta med ekstra friends. Grad taulengder: 5,6,7-,6,1,4+", 6, "7-", null, null, 0, 0));
//		data.add(new Data(4, "Skråstad", "Skråstad", "Skråstaddiederet", T.TRAD, "topp50 Klassikeren. Variert og fin. Grad taulengder: 3+,5,5+", 3, "5+", null, null, 0, 0));
//		data.add(new Data(5, "Skråstad", "Skråstad", "Nordvestveien", T.AIDTRAD, "Variert og fin. Teknisk sistetaulengde. Grad taulengder: 6-,5+,A1", 3, "6-", null, null, 0, 0));
//		data.add(new Data(6, "Skråstad", "Skråstad", "Nålesprekken(e)", T.TRAD, "Fint S-formet riss i andre taulengde. Borebolt i starten på den eksponerte sistetaulengden. Grad taulengder: 4+,6,5,7", 4, "n/a", null, null, 0, 0));
//		data.add(new Data(7, "Skråstad", "Skråstad", "Big Bang", T.TRAD, "Felles med 'Skråstaddiederet' i siste taulengde. Grad taulengder: 6,5+", 2, "n/a", null, null, 0, 0));
//		data.add(new Data(8, "Skråstad", "Skråstad", "Fødselssprekken", T.TRAD, "Tynt sikret førstetaulengde. Toppen av rute 9 er den naturlige forlengelsen om det er psyke igjen... Grad taulengder: 6-,4+", 2, "6-", null, null, 0, 0));
//		data.add(new Data(9, "Skråstad", "Skråstad", "Kjempekaminen/Evening over Rooftops", T.TRAD, "Meget spesiell førstetaulengde. Vær obs på taudrag! Grad taulengder: 6-,3+,5+,6-", 4, "6-", null, null, 0, 0));
//		data.add(new Data(10, "Skråstad", "Skråstad", "Hansen&Hansen 1", T.TRAD, "Prosjekt (grad 7?)", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(11, "Skråstad", "Skråstad", "Hansen&Hansen 2", T.TRAD, "Prosjekt (grad 7?)", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(12, "Skråstad", "Skråstad", "Den ufullendte", T.TRAD, "NB! Mangler en bolt! Psykende - eller galskap om du vil. Ender i toppfeste til Hansen&Hansen 2.", 1, "6-", null, null, 0, 0));
//		data.add(new Data(1, "Slettafjell", "Slettafjell", "Rett på sak", T.TRAD, "100 meter intro + 7 taulengder. (2/100m), 4+/50m, 5/50m, 6+/55m, 5/50m, 5/55m, 5-/40m, 3/20m. Kort off width-parti i andre taulengde og ikke helt triviell overhengshekling i tredje taulengde gir god motstand og interessant klatring. Snaut to taulengder er felles med rute nummer 2. Mange muligheter og diffuse linjer i toppen. Utstyr: Kjekt med #3 / #4 Camalot samt en del mindre kammer.", 7, "6+", "Nils Rune Birkeland,Tom Egil Rosseland", "2011-01-01", 0, 0));
//		data.add(new Data(2, "Slettafjell", "Slettafjell", "Helt hjem", T.TRAD, "9 taulengder + intro (2+/150m), 4/50m, 5/45m, 5-/50m, 5-/50m, 5-/50m, 5/50m, 5-/50m, 4+/50m, 2+/45m Gild rute som stormer den høyeste delen av veggen. Standard kilesett og noen medium store kamkiler", 9, "5-", "Nils Rune Birkeland, Lars Jacobsen,Fred Arne Wergeland", "1997-01-01", 0, 0));
//		data.add(new Data(3, "Slettafjell", "Slettafjell", "Ikke bare blåbær", T.TRAD, "8 taulengder + intro(2+/150m), 4/50m, 5+/50m, 6-/25m, 5+/50, 4/50, 4/50, 4+/20m, 4+/50m.. Fin rute i reine formasjoner. 3. taulengde er dårlig sikret.", 8, "6-", "Nils Rune Birkeland,Torleiv Nordgaarden", "1996-01-01", 0, 0));
//		data.add(new Data(4, "Slettafjell", "Slettafjell", "Halvveis til himmelen", T.TRAD, "5 taulengder+ intro og utsteg. (2/100m), 4-/45m, 5+/50m, 6-/50m, 5+/50m, 4+/40m. Artig, variert tur. Tynn travers i 3. taulengde, og litt stifinning i 4. taulengde. Utsteg langs skrårampe til rappellfestene på 'Trygg og glad'.", 5, "6-", "Ulf Chr. Bentsen,Nils Rune Birkeland", "1995-01-01", 0, 0));
//		data.add(new Data(5, "Slettafjell", "Slettafjell", "B: Tørr og glad", T.TRAD, null, 1, "6-", null, null, 0, 0));
//		data.add(new Data(5, "Slettafjell", "Slettafjell", "Trygg og glad", T.BOLT, "7 taulengder. 2+/30m, 3+/40m, 4/45m, 5-/40m, 5/40m, 6/50m, 5+/40m. Hyggelig klatring på bolter. Taudrag i 6. taulengde. Cruxet kan løses ved teknisk klatring (pga tett og fin bolting). 12 kortslynger+ en 60 cm slynge. 2x50 m tau for rappellen.", 7, "6", "Nils Rune Birkeland", "2000-01-01", 0, 0));
//		data.add(new Data(5, "Slettafjell", "Slettafjell", "A: Trygg og glad helt hjem", T.TRAD, "Tilfører ytterligere fire taulengder (5-/35m, 5/30m, 5+/50m, 4/35m) til «Trygg og glad». Naturlig sikret, men tre boltede standplasser underveis (andre, tredje og fjerde), som gir rappell retur hele veien ned fjellet. Taulengde en og to kan linkes med 60m tau med litt god strekk.", 4, "5+", "Nils Rune Birkeland, Tom Egil Rosseland,Halvor Danielsen", null, 0, 0));
//		data.add(new Data(6, "Slettafjell", "Slettafjell", "Prikken over i'en", T.TRAD, "4 taulengder +intro (lett). 4/55m, 4+/50m, 5-/55m, 5+/50m Lettere skive- og rissklatring avsluttes med litt heftigere sva og dieder i toppen. Denne ruten er ikke helt trivielt sikret, og det er en mulig escape inn i 'Trygg og glad' før den mer krevende 5+ sistetaulengden. Retur: 25 m diagonal rappell fra tre inn i rute 5. 2x50 m tau, standard kilesett + noen små og en stor kamkile.", 4, "5+", null, null, 0, 0));
//		data.add(new Data(7, "Slettafjell", "Slettafjell", "Heteslag", T.TRAD, "6 taulengder + intro (lett) 5/40m, 6-/50m, 6-/45m, 5+/45m, 6+/50m, 5/60m. Nydelig rute! Beskrivelse lengde for lengde: perfekt sva/dieder, tynt sikra riss/dieder, svacrux, blokk med slynge, drømmelengde med småkiler og en borebolt, siste lengde kan med fordel deles opp i to. Små kamkiler i tillegg til standardrack.", 6, "6+", "Gisle Andersen,Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(1, "Sødal", "Jubelveggen", "Jakten på den gode følelsen", T.BOLT, "6bb. Flott egg- og listeklatring. Blant topp 50", 1, "8-/8", "Markus Landrø", "1992-01-01", 0, 0));
//		data.add(new Data(2, "Sødal", "Jubelveggen", "Prosjekt, åpent", T.BOLT, "6bb. Kreatør: Markus Landrø/Stefan Landrø", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(3, "Sødal", "Jubelveggen", "Glemte sorger", T.BOLT, "6bb. Onsight-vennlig.", 1, "8-", "Msrkus Landrø", "1992-01-01", 0, 0));
//		data.add(new Data(4, "Sødal", "Jubelveggen", "Fin flyt", T.BOLT, "7bb. Finn flyten i fine flytt.", 1, "7+", "Markus Landrø", null, 0, 0));
//		data.add(new Data(5, "Sødal", "Jubelveggen", "Prosjekt, åpent", T.BOLT, "9bb. Kreatør: Nils Rune Birkeland", 1, "8+", null, null, 0, 0));
//		data.add(new Data(6, "Sødal", "Jubelveggen", "Frihetstraversen", T.BOLT, "10bb. Start i rute 4 og traverser til høyre og kysser rute 7.", 1, "n/a", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(7, "Sødal", "Jubelveggen", "Prosjekt", T.BOLT, "10bb. Kreatør: Joshua Shulz.Topper ut i rute fem.", 1, "8+", null, null, 0, 0));
//		data.add(new Data(8, "Sødal", "Jubelveggen", "Kjærlighetens tapere", T.BOLT, "6bb. Spiser hud, men veldig fin likevel.", 1, "8+", "Klaus Sandvik", null, 0, 0));
//		data.add(new Data(9, "Sødal", "Jubelveggen", "Prosjekt, åpent", T.BOLT, "Kreatør: Claus Sandvik", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(10, "Sødal", "Jubelveggen", "Halleluja", T.BOLT, "4bb. Særlig om du henger igjen på topptaket. Blant topp 50.", 1, "8/8+", "Klaus Sandvik", null, 0, 0));
//		data.add(new Data(11, "Sødal", "Jubelveggen", "Livets lister", T.BOLT, "5bb. Oppvarming før krimpefesten. Toppcrux.", 1, "8-", "Markus Landrø", null, 0, 0));
//		data.add(new Data(12, "Sødal", "Jubelveggen", "Rebell Yell", T.BOLT, "6bb. Topper ut i rute 11 etter at man klipper bolt i høyde med rute 11-ankeret og så traverserer til dette.", 1, "7+", "Joshua Schulz", null, 0, 0));
//		data.add(new Data(1, "Sødal", "Queens", "Navnløs 1", T.BOLT, "4bb. Kort, lite klatret rute.", 1, "5+", null, null, 0, 0));
//		data.add(new Data(2, "Sødal", "Queens", "Navnløs 2", T.BOLT, "3bb. Kort, intens for graden.", 1, "5+", null, null, 0, 0));
//		data.add(new Data(3, "Sødal", "Queens", "Hammer to Fall", T.BOLT, "3bb. Klassiker, noen elsker den andre hater den.", 1, "6+", null, null, 0, 0));
//		data.add(new Data(4, "Sødal", "Queens", "Brak Free", T.BOLT, "4bb. Småkjip crux-sekvens.", 1, "7-", "Børre Bergshaven", "1993-01-01", 0, 0));
//		data.add(new Data(5, "Sødal", "Queens", "Radio Gaga", T.BOLT, "8bb. Tynn og litt skarp i starten, går inn i Inuendo i toppen.", 1, "7-", "Kim Kjærgaard", "2002-01-01", 0, 0));
//		data.add(new Data(6, "Sødal", "Queens", "Innuendo", T.BOLT, "7bb. Fin og variert klatring med litt intens start.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(7, "Sødal", "Queens", "Little This", T.BOLT, "8bb. Fingertung veggklatring. Kontinuerlig.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(8, "Sødal", "Queens", "Made in Heaven", T.BOLT, "8bb. Byens snilleste 7+?", 1, "7+", null, null, 0, 0));
//		data.add(new Data(9, "Sødal", "Queens", "It`s a Hard Life", T.BOLT, "9bb. Intens vertikal klatring på små tak. Dynamikk hjelper.", 1, "8-", "Makrus Landrø", "1993-01-01", 0, 0));
//		data.add(new Data(10, "Sødal", "Queens", "A Kind of Magic", T.BOLT, "8bb. Absolutt magisk, men finner du løsningen? 'Blant topp 50'", 1, "7-", null, null, 0, 0));
//		data.add(new Data(11, "Sødal", "Queens", "Loves Me", T.BOLT, "6bb. Lett midtparti, men desto mer krevende mot slutten.", 1, "6+", "Nils Rune Birkeland", "2001-01-01", 0, 0));
//		data.add(new Data(12, "Sødal", "Queens", "Mercury i hekken", T.BOLT, "8bb. Svært variert rute, men hard for graden.", 1, "6-", null, null, 0, 0));
//		data.add(new Data(13, "Sødal", "Queens", "7a-testen", T.BOLT, "6bb. En klassisk 7+. Test deg selv. Crux i toppen.", 1, "7+", null, null, 0, 0));
//		data.add(new Data(14, "Sødal", "Queens", "Prosjkt", T.BOLT, "Samme topp som Snett åt venster,", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(15, "Sødal", "Queens", "Snett åt venster", T.BOLT, "6bb. Fin tur", 1, "6+", "Nils Rune Birkeland", "2001-01-01", 0, 0));
//		data.add(new Data(16, "Sødal", "Queens", "Ukjent", T.BOLT, "5bb", 1, "6+", null, null, 0, 0));
//		data.add(new Data(17, "Sødal", "Queens", "Branten på kanten", T.BOLT, "5bb. Typisk Queens-klatring. Starter fra hylla.", 1, "6+", null, null, 0, 0));
//		data.add(new Data(18, "Sødal", "Queens", "Mercury 9,9", T.BOLT, "5bb. Artig, balansert klatring. Starter nedenfor hylla.", 1, "n/a", "Nils Rune Birkeland", "1999-01-01", 0, 0));
//		data.add(new Data(19, "Sødal", "Queens", "Ståle Krapyl", T.BOLT, "5bb. Lite klatret. Noe løst fjell.", 1, "n/a", "Ståle Bjørkestøl", "2001-01-01", 0, 0));
//		data.add(new Data(20, "Sødal", "Queens", "Lis Jorid", T.BOLT, "5bb. Starter med små lister og avslutter med store formasjoner.", 1, "n/a", "Ørnulf Kittelsen", "1994-01-01", 0, 0));
//		data.add(new Data(21, "Sødal", "Queens", "Tuvas drøm", T.BOLT, "6bb. God teknikk og litt mot må til for å drømme søtt.", 1, "7-", "Tove Annette Sandell", null, 0, 0));
//		data.add(new Data(22, "Sødal", "Queens", "Trygves skrekk", T.BOLT, "4bb. Her må du dra til på cruxet.", 1, "6+", "Ståle Bjørkestøl", "2001-01-01", 0, 0));
//		data.add(new Data(23, "Sødal", "Queens", "Navn?", T.BOLT, "6bb. Forholdsavhengig. Eliminasjonsrute, ikke benytt diederkanten til venstre!", 1, "7+", "Ståle Bjørkestøl", "1997-01-01", 0, 0));
//		data.add(new Data(24, "Sødal", "Queens", "Kosekroken", T.TRAD, "Noen løse steiner helt i toppen. Snufeste i naboruta eller bjørketre.", 1, "n/a", "Nils Rune Birkeland", "1994-01-01", 0, 0));
//		data.add(new Data(25, "Sødal", "Queens", "Toppen er kaka", T.BOLT, "6bb. God friksjon og laybackteknikk gjør susen.", 1, "n/a", "Marius Sunde", "1996-01-01", 0, 0));
//		data.add(new Data(26, "Sødal", "Queens", "Navnløs", T.BOLT, "6bb. Hold balansen i starten, og kjenn pumpen mot slutten.", 1, "6+", null, null, 0, 0));
//		data.add(new Data(27, "Sødal", "Queens", "Fatman", T.BOLT, "5bb. Hard på flaten etter overhenget.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(28, "Sødal", "Queens", "Fat Bottomed Girl", T.BOLT, "6bb. Kort, morsomt overheng. Blant topp 50.", 1, "6+", null, null, 0, 0));
//		data.add(new Data(29, "Sødal", "Queens", "Bottom", T.BOLT, "2bb. Populær til kurs.", 1, "4+", null, null, 0, 0));
//		data.add(new Data(30, "Sødal", "Queens", "Bohemian Rhapsody", T.BOLT, "6bb. 7 Bohemian Rhapsody. 7- 6x. Overhenget er utfordringen.", 1, "7-", null, null, 0, 0));
//		data.add(new Data(31, "Sødal", "Queens", "Back", T.BOLT, "2bb. Populær begynnerrute.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(32, "Sødal", "Queens", "6a-testen", T.BOLT, "6bb. En herlig tur. Fotarbeid avgjør i cruxet.", 1, "6-", "Markus Landrø", null, 0, 0));
//		data.add(new Data(33, "Sødal", "Queens", "Slash", T.BOLT, "4bb. Hold balansen før ankeret.", 1, "4+", null, null, 0, 0));
//		data.add(new Data(34, "Sødal", "Queens", "Kronisk 29", T.BOLT, "4bb. En en litt luftigere variant til samme anker som Slash.", 1, "4+", null, null, 0, 0));
//		data.add(new Data(35, "Sødal", "Queens", "Prosjekt", T.BOLT, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(36, "Sødal", "Queens", "Get Shorty", T.BOLT, "4bb. Morsom rute hvor teknikken settes på prøve.", 1, "4+", null, null, 0, 0));
//		data.add(new Data(37, "Sødal", "Queens", "Long John", T.BOLT, "5bb. Som navnet sier.", 1, "7-", null, null, 0, 0));
//		data.add(new Data(38, "Sødal", "Queens", "Nummer en", T.BOLT, "8bb. Fin tur. Balanse og skulderstyrke settes på prøve.", 1, "8-", "Jon Dalvang Andresen", null, 0, 0));
//		data.add(new Data(39, "Sødal", "Queens", "Inn er ut", T.BOLT, "7bb. Spesielt crux der ruta svinger ut på eggen.", 1, "7-", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(1, "Solsletta", "Høilands hule", "Moscito", T.TRAD, null, 1, "n/a", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(2, "Solsletta", "Høilands hule", "Malaria", T.BOLT, "4bb. Rett opp gjennom det lille overhenget.", 1, "n/a", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(3, "Solsletta", "Høilands hule", "Gjennom granskauen", T.BOLT, "8bb. Fin klatring på gode tak.", 1, "8-", "Vidar Ørebek", null, 0, 0));
//		data.add(new Data(4, "Solsletta", "Høilands hule", "La fingrene blomstre.", T.BOLT, "7bb. Hard topp på små lister.", 1, "8-", "Vidar Ørebek", null, 0, 0));
//		data.add(new Data(5, "Solsletta", "Høilands hule", "Sabotager", T.BOLT, "7bb. Hard topp på sloper.", 1, "n/a", "Vidar Ørebek", null, 0, 0));
//		data.add(new Data(6, "Solsletta", "Høilands hule", "Easy Does It", T.BOLT, "7bb", 1, "8+", "Kristian Vallevik", null, 0, 0));
//		data.add(new Data(7, "Solsletta", "Høilands hule", "Cracky Fingers", T.BOLT, "5bb", 1, "n/a", "Kristian Vallevik", null, 0, 0));
//		data.add(new Data(8, "Solsletta", "Høilands hule", "Å takk Gud for mine fingre.", T.BOLT, "5bb", 1, "7+", "Kristian Vallevik", null, 0, 0));
//		data.add(new Data(9, "Solsletta", "Høilands hule", "Feil i hjernens bolteminne.", T.BOLT, "4bb", 1, "7+", "Kristian Vallevik", null, 0, 0));
//		data.add(new Data(1, "Solsletta", "Main Wall", "Ragnarok For the Moose", T.BOLT, "5bb. Bratt, kort og intens.", 1, "7-", null, null, 0, 0));
//		data.add(new Data(2, "Solsletta", "Main Wall", "Virtual Death", T.BOLT, "4bb. Skikkelig vrang i starten, siden lettere sva.", 1, "6+", null, null, 0, 0));
//		data.add(new Data(3, "Solsletta", "Main Wall", "No Fear", T.BOLT, "Litt vrang i starten, siden lettere sva.", 1, "6-", null, null, 0, 0));
//		data.add(new Data(4, "Solsletta", "Main Wall", "Green Line", T.BOLT, "6bb. Krevende i starten, siden lettere sva.", 1, "6/6+", null, null, 0, 0));
//		data.add(new Data(5, "Solsletta", "Main Wall", "La Forza Del Destino", T.BOLT, "7bb. Fint og litt krevede opp til midten, deretter sva.", 1, "6-", null, null, 0, 0));
//		data.add(new Data(6, "Solsletta", "Main Wall", "Just Around the Corner", T.BOLT, "4bb. Kort rute med artig start og vrien topp.", 1, "5+", null, null, 0, 0));
//		data.add(new Data(7, "Solsletta", "Main Wall", "Superlangbein", T.BOLT, "5bb. Kort rute. Ser lettere ut enn det er.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(8, "Solsletta", "Main Wall", "Le Quattro Staggioni", T.TRAD, "Følg sprekk skrått mot venstre, deretter rundt flaten og til topps.", 1, "5+", null, null, 0, 0));
//		data.add(new Data(9, "Solsletta", "Main Wall", "Ørnulf Ørn", T.BOLT, "3bb. Kort rute. Balanse og teknikk.", 1, "6-", null, null, 0, 0));
//		data.add(new Data(10, "Solsletta", "Main Wall", "Farlige Fiffus", T.BOLT, "3bb. Kort rute. Mer balansering.", 1, "6-", null, null, 0, 0));
//		data.add(new Data(11, "Solsletta", "Main Wall", "Micro Midas", T.BOLT, "3bb. Kort rute. Samme topp som Farlige Fiffus.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(1, "Solsletta", "Piece Wall", "Another Piece Of Meat.", T.MIXED, "Miksrute.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Solsletta", "Piece Wall", "The Missing Piece.", T.BOLT, "4bb. Samme snufeste som rute 3.", 1, "4-", null, null, 0, 0));
//		data.add(new Data(3, "Solsletta", "Piece Wall", "Piece Of Cupcake.", T.BOLT, "6bb", 1, "4+", null, null, 0, 0));
//		data.add(new Data(4, "Solsletta", "Piece Wall", "Piece Of Cake", T.BOLT, "6bb. Ny variant.", 1, "4+", null, null, 0, 0));
//		data.add(new Data(5, "Solsletta", "Piece Wall", "The Last Piece", T.BOLT, "6bb", 1, "5-", null, null, 0, 0));
//		data.add(new Data(6, "Solsletta", "Piece Wall", "Picking Up the Pieces", T.BOLT, "6bb", 1, "5+", null, null, 0, 0));
//		data.add(new Data(7, "Solsletta", "Piece Wall", "Piece of Mind", T.TRAD, null, 1, "4-", null, null, 0, 0));
//		data.add(new Data(8, "Solsletta", "Piece Wall", "Piece of Nothing", T.TRAD, null, 1, "4+", null, null, 0, 0));
//		data.add(new Data(9, "Solsletta", "Piece Wall", "Piece of Shit", T.BOLT, "4bb", 1, "5-", null, null, 0, 0));
//		data.add(new Data(10, "Solsletta", "Piece Wall", "Piece of the Action", T.TRAD, null, 1, "5-", null, null, 0, 0));
//		data.add(new Data(11, "Solsletta", "Piece Wall", "Rest in Pieces", T.TRAD, null, 1, "4-", null, null, 0, 0));
//		data.add(new Data(1, "Steikeplata", "Steikeplata", "Stekte snabber", T.BOLT, "8bb. Starter bak kanten. Kort vertikalt og bratt parti.", 1, "6+", "Børre Bergshaven", null, 0, 0));
//		data.add(new Data(2, "Steikeplata", "Steikeplata", "Grillbein", T.BOLT, "9bb. Fin tur.", 1, "6-", "Børre Bergshaven", null, 0, 0));
//		data.add(new Data(3, "Steikeplata", "Steikeplata", "Omelett", T.BOLT, "10bb. Tynne tak og balansert klatring.", 1, "7-", "Børre Bergshaven", null, 0, 0));
//		data.add(new Data(4, "Steikeplata", "Steikeplata", "Svidde fingre", T.BOLT, "6bb. Mye mose, Samme start som rute 3 'omelett'", 1, "6+", "Børre Bergshaven", null, 0, 0));
//		data.add(new Data(5, "Steikeplata", "Steikeplata", "Grillspyd", T.BOLT, "7bb", 1, "7+", "Børre Bergshaven", null, 0, 0));
//		data.add(new Data(6, "Steikeplata", "Steikeplata", "Bjarte og panserbilene", T.BOLT, "8bb", 1, "7/7+", "Børre Bergshaven", null, 0, 0));
//		data.add(new Data(7, "Steikeplata", "Steikeplata", "Grenlandsvisitten", T.BOLT, "6bb. Tynt sva", 1, "8-", "Jarle Mosaker", null, 0, 0));
//		data.add(new Data(8, "Steikeplata", "Steikeplata", "Prosjekt", T.BOLT, "6bb. kreatør: Børre Bergshaven", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(9, "Steikeplata", "Steikeplata", "Prosjekt", T.BOLT, "6bb. Kreatør: Børre Bergshaven", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(10, "Steikeplata", "Steikeplata", "Prosjekt", T.BOLT, "6bb. Kreatør: Børre Bergshaven", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(11, "Steikeplata", "Steikeplata", "Kuk i klemme", T.BOLT, "4bb. Kreatør: Børre Bergshaven", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(12, "Steikeplata", "Steikeplata", "Grillmix", T.BOLT, "6bb", 1, "7+", "Børre Bergshaven", null, 0, 0));
//		data.add(new Data(13, "Steikeplata", "Steikeplata", "Grønn salat", T.BOLT, "6bb", 1, "6-", "Børre Bergshaven", null, 0, 0));
//		data.add(new Data(14, "Steikeplata", "Steikeplata", "Stekt løk", T.BOLT, "6bb", 1, "5+", "Børre Bergshaven", null, 0, 0));
//		data.add(new Data(15, "Steikeplata", "Steikeplata", "Grillsaus", T.BOLT, "6bb", 1, "6+", "Børre Bergshaven", null, 0, 0));
//		data.add(new Data(16, "Steikeplata", "Steikeplata", "Marinade", T.BOLT, "4bb", 1, "6+", "Børre Bergshaven", null, 0, 0));
//		data.add(new Data(17, "Steikeplata", "Steikeplata", "Grillspyd", T.BOLT, "3bb", 1, "5+", "Børre Bergshaven", null, 0, 0));
//		data.add(new Data(1, "Storheia", "Backstage", "Silnet Warrior", T.BOLT, "7bb. Bratt klatring på overraskende gode tak.", 1, "n/a", "Nils Rune Birkeland,Mike Tombs", "2001-01-01", 0, 0));
//		data.add(new Data(2, "Storheia", "Backstage", "Ensom kriger", T.BOLT, "9bb. NB. Utrygt flak midt i ruta.", 1, "7-", "Nils Rune Birkeland", "2001-01-01", 0, 0));
//		data.add(new Data(3, "Storheia", "Big Bird Wall", "Mea Culpa", T.BOLT, "7bb. Teknisk crux.", 1, "6+", "Nils Rune Birkeland,Mike Tombs", "2001-01-01", 0, 0));
//		data.add(new Data(4, "Storheia", "Big Bird Wall", "Second Chapter", T.BOLT, "7bb", 1, "7-", "Mike Tombs", null, 0, 0));
//		data.add(new Data(5, "Storheia", "Big Bird Wall", "Screaming Jews", T.BOLT, "8bb. Tøff stemming i starten", 1, "7+", "Nils Rune Birkeland,Mike Tombs", "2001-01-01", 0, 0));
//		data.add(new Data(6, "Storheia", "Big Bird Wall", "Voice of the Snake", T.BOLT, "8bb. Et vertikalt eventyr", 1, "7-", "Mike Tombs", null, 0, 0));
//		data.add(new Data(7, "Storheia", "Big Bird Wall", "Return to Innocence", T.BOLT, "8bb. Samme topp som foregående rute, samme fine klatring.", 1, "7+", "Mike Tombs", null, 0, 0));
//		data.add(new Data(8, "Storheia", "Big Bird Wall", "Hugo hoggtann", T.BOLT, "7bb. Fortsatt fin klatring, bare hardere.", 1, "8-", "Runar Carlsen", "2001-01-01", 0, 0));
//		data.add(new Data(9, "Storheia", "Big Bird Wall", "Svikern", T.BOLT, "5bb. Var dette et prosjektrov? De lærde strides.", 1, "7+", "Nils Rune Birkeland", "2001-01-01", 0, 0));
//		data.add(new Data(5, "Storheia", "Dolphin Wall", "Saxxon Ways", T.BOLT, "9bb. Avslutter på delfinens nese. Spektakulært.", 1, "7-", "Mike Tombs", null, 0, 0));
//		data.add(new Data(6, "Storheia", "Dolphin Wall", "Into the Deep", T.BOLT, "8bb. Høyrevariant som også avsluttes på delfinens nese.", 1, "6+", "Mike Tombs", null, 0, 0));
//		data.add(new Data(7, "Storheia", "Dolphin Wall", "Deep Direct", T.TOPROPE, null, 1, "n/a", "Mike Tombs", null, 0, 0));
//		data.add(new Data(8, "Storheia", "Dolphin Wall", "Out from the Deep", T.TOPROPE, null, 1, "6+", "Mike Tombs", null, 0, 0));
//		data.add(new Data(9, "Storheia", "Dolphin Wall", "Dolphin Dream", T.BOLT, "10bb. Klasikker. Bruk boltene til Dolphin Direct.", 1, "6+", "Mike Tombs", null, 0, 0));
//		data.add(new Data(10, "Storheia", "Dolphin Wall", "Dolphin Direct", T.BOLT, "10bb. Rett opp hele veien.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(11, "Storheia", "Dolphin Wall", "Rivers of Belief", T.BOLT, "9bb. Småsykende travers, deretter bratt og fin helt til topps.", 1, "7-", "Mike Tombs,Raymond Moi", null, 0, 0));
//		data.add(new Data(12, "Storheia", "Dolphin Wall", "Free Ranch Chicken", T.TOPROPE, null, 1, "6+", "Mike Tombs", null, 0, 0));
//		data.add(new Data(13, "Storheia", "Dolphin Wall", "Keltic Dream, prosjekt", T.TOPROPE, "Kreatør: Mike Tombs", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(14, "Storheia", "Dolphin Wall", "TV Hero", T.BOLT, "10bb. Bratt og luftig klassiker. Blant topp50.", 1, "8-", "Mike Tombs,Nils Rune Birkeland", "2001-01-01", 0, 0));
//		data.add(new Data(15, "Storheia", "Dolphin Wall", "I’m the Egg Man,prosjekt.", T.TOPROPE, "Spektakulær linje, men går det?", 1, "n/a", "Mike Tombs", null, 0, 0));
//		data.add(new Data(1, "Storheia", "Pine Wall", "Sylfest", T.BOLT, "3bb", 1, "4-", "Lars Reiersen", "2001-01-01", 0, 0));
//		data.add(new Data(2, "Storheia", "Pine Wall", "Ingunns sva", T.BOLT, "4bb. Lekkert sva. God friksjon.", 1, "3+", "Lars Reiersen", "2001-01-01", 0, 0));
//		data.add(new Data(3, "Storheia", "Pine Wall", "Hiltisoloisten", T.BOLT, "4bb", 1, "4-", "Lars Reiersen", "2001-01-01", 0, 0));
//		data.add(new Data(4, "Storheia", "Pine Wall", "Slikkepinn", T.BOLT, "6bb. Plutselig litt bratt og luftig mot toppen.", 1, "4+", "Nils Rune Birkeland", "2001-01-01", 0, 0));
//		data.add(new Data(5, "Storheia", "Pine Wall", "Lovens lange arm", T.BOLT, "7bb. Et tak har brukket, men artig travers om du kommer forbi.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(6, "Storheia", "Pine Wall", "Anitas dans", T.BOLT, "6bb. Litt hardt for graden, men artig.", 1, "n/a", "Axelia Larsen", "2001-01-01", 0, 0));
//		data.add(new Data(7, "Storheia", "Pine Wall", "Baconsvaet", T.BOLT, "6bb. Funker med balansekunst.", 1, "6-", "Claire Peadecerf", "2000-01-01", 0, 0));
//		data.add(new Data(8, "Storheia", "Pine Wall", "Spy og vær glad", T.BOLT, "7bb. Ikke så lett å finne ut av.", 1, "6-", null, null, 0, 0));
//		data.add(new Data(9, "Storheia", "Pine Wall", "Snikende tiger", T.BOLT, "7bb. Mer sniking og balanse. Hard for graden.", 1, "6-", null, null, 0, 0));
//		data.add(new Data(10, "Storheia", "Pine Wall", "Tilbake til Grorud", T.BOLT, "8bb. Først smyge, gå så dynamisk og til slutt cruise på gode tak.", 1, "6-", null, null, 0, 0));
//		data.add(new Data(11, "Storheia", "Pine Wall", "Svenskedødaren", T.BOLT, "9bb. God teknikk og styrke kan gi deg en herlig tur.", 1, "7-", "Sebastian Zartman", "2001-01-01", 0, 0));
//		data.add(new Data(12, "Storheia", "Pine Wall", "Gressenkemann", T.BOLT, "9bb. Ikke mye klatret. Fortjener mer trafikk.", 1, "6+", "Nils Rune Birkeland", "2001-01-01", 0, 0));
//		data.add(new Data(13, "Storheia", "Pine Wall", "Juggernaut", T.BOLT, "9bb. Artig crux og klatring på store formasjoner mot toppen.", 1, "n/a", "Nils Rune Birkeland", "2001-01-01", 0, 0));
//		data.add(new Data(14, "Storheia", "Pine Wall", "Kampen om Sampo", T.BOLT, "8bb. Fin oppvarming, men markert mer krevende i toppen.", 1, "5+", "Hugo Hermansen", "2001-01-01", 0, 0));
//		data.add(new Data(15, "Storheia", "Pine Wall", "Verdensrommet slår tilbake.", T.BOLT, "8bb. Nydelig nesten vertikal klatring.", 1, "7-", "Hugo Hermansen", "2001-01-01", 0, 0));
//		data.add(new Data(16, "Storheia", "Pine Wall", "Le Disciple", T.BOLT, "4bb. Kort og konsist. Buldrete på små tak.", 1, "n/a", "Hugo Hermansen", "2001-01-01", 0, 0));
//		data.add(new Data(17, "Storheia", "Pine Wall", "Tidstyvene", T.BOLT, "8bb. Smyging på et høyt nivå, pluss litt fingerkraftklatring.", 1, "8-", "Hugo Hermansen", "2001-01-01", 0, 0));
//		data.add(new Data(18, "Storheia", "Pine Wall", "Runout-gutta", T.TRAD, "Skremmende navn, ikke mye klatret.", 1, "6-", "Raymond Moi,Lars Reiersen", null, 0, 0));
//		data.add(new Data(4, "Storheia", "Second Floor", "Janus", T.BOLT, "5bb", 1, "6+", "Nils Rune Birkeland", "2001-01-01", 0, 0));
//		data.add(new Data(5, "Storheia", "Second Floor", "Eros", T.BOLT, "6bb. Tynne tak gjennom overhenget.", 1, "7+", "Nils Rune Birkeland", "2001-01-01", 0, 0));
//		data.add(new Data(6, "Storheia", "Second Floor", "Thanatos", T.BOLT, "6bb. Oppkalt etter storebror i Nord.", 1, "8-", "Nils Rune Birkeland", "2001-01-01", 0, 0));
//		data.add(new Data(7, "Storheia", "Second Floor", "Kim Salbim", T.BOLT, "3bb", 1, "7-", "Nils Rune Birkeland", "2001-01-01", 0, 0));
//		data.add(new Data(8, "Storheia", "Second Floor", "Anettes", T.BOLT, "4bb. Hyggelig liten svatur.", 1, "5-", "Torsten Tollestrup", null, 0, 0));
//		data.add(new Data(1, "Storheia", "Slab Wall", "Vannfallet", T.TOPROPE, "Dette er en flott isrute om vinteren.", 1, "n/a", "Lars Reiersen", null, 0, 0));
//		data.add(new Data(2, "Storheia", "Slab Wall", "Joking Apart", T.BOLT, "7bb. Slappe hyller", 1, "6-", "Mike Tombs", null, 0, 0));
//		data.add(new Data(3, "Storheia", "Slab Wall", "De fattige er Europas negre", T.BOLT, "7bb. Fattig på tak mot toppen", 1, "6+", "Nils Rune Birkeland", "2001-01-01", 0, 0));
//		data.add(new Data(4, "Storheia", "Slab Wall", "Catwalk", T.BOLT, "7bb. Put on your blue suede shoes.", 1, "7-", "Mike Tombs", null, 0, 0));
//		data.add(new Data(1, "Storheia", "Traktorveggen", "Østra sjukhuset", T.BOLT, "5bb. Litt bøss i starten, fin og balansert i toppen.", 1, "7-", "Hugo Hermansen", "2001-01-01", 0, 0));
//		data.add(new Data(2, "Storheia", "Traktorveggen", "Prosjekt", T.TOPROPE, "Kun toppfeste", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(3, "Storheia", "Traktorveggen", "Bøsseleden", T.BOLT, "4bb. Fin rødlig svapillar.", 1, "4+", "Nils Rune Birkeland", "2001-01-01", 0, 0));
//		data.add(new Data(19, "Storheia", "Trophy", "Fransk feeling", T.BOLT, "8bb. DEN følelsen. Punktcrux i overhenget, og det er ikke alt!", 1, "8/8+", "Jan Eivind Danielsen", "2002-01-01", 0, 0));
//		data.add(new Data(20, "Storheia", "Trophy", "Boller & mus", T.BOLT, "8bb. Fingerkiller med flyposensial.", 1, "8/8+", "Runar Carlsen", null, 0, 0));
//		data.add(new Data(21, "Storheia", "Trophy", "Et lite stykke Norge", T.BOLT, "5bb. Verdt strevet.", 1, "n/a", "Lars Reiersen", "2001-01-01", 0, 0));
//		data.add(new Data(22, "Storheia", "Trophy", "Den lille ruta bak treet", T.BOLT, "3bb. Smått kan være godt.", 1, "7-", "Hugo Hermansen", "2002-01-01", 0, 0));
//		data.add(new Data(23, "Storheia", "Trophy", "Plantasjen", T.TRAD, "Litt mye buskevekster kanskje. Luftig 2. taulengde. Grad på taulengder: 4, 6.", 2, "n/a", "Lars Reiersen", "2001-01-01", 0, 0));
//		data.add(new Data(24, "Storheia", "Trophy", "Trophy", T.BOLT, "15bb. Lang klasikker på store formasjoner. 60 m tau. Ta med lange slynger. Taudrag mot toppen. Blant topp50.", 1, "7-", "Lars Reiersen", "2001-01-01", 0, 0));
//		data.add(new Data(25, "Storheia", "Trophy", "Flyskolen 2", T.BOLT, "Tør du?", 1, "8/8+", "Runar Carlsen", null, 0, 0));
//		data.add(new Data(26, "Storheia", "Trophy", "Camel Trophy", T.BOLT, "5bb. Starten på Trophy, snufeste midt i veggen.", 1, "5-", "Lars Reiersen", "2001-01-01", 0, 0));
//		data.add(new Data(27, "Storheia", "Trophy", "Brask og bram", T.BOLT, "6bb. Overraskende fin bonusrute.", 1, "7-", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(1, "Tango Wall", "Main wall", "Bookmaker", T.BOLT, "9bb", 1, "n/a", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(2, "Tango Wall", "Main wall", "Reisen", T.BOLT, "17bb", 1, "8/8+", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(3, "Tango Wall", "Main wall", "Pinocchios bryllupsdag", T.BOLT, "18bb", 1, "8+/9-", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(4, "Tango Wall", "Main wall", "Prosjekt", T.BOLT, "18bb. Pinocchios gravferd. Kreatør: Nils Rune Birkeland", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(5, "Tango Wall", "Main wall", "Runars psykohelvete", T.BOLT, "15bb. Skummel rute", 1, "7+/8-", "Runar Carlsen", null, 0, 0));
//		data.add(new Data(6, "Tango Wall", "Main wall", "Bursdagsgaven", T.TRAD, null, 1, "5+", "Christine Kielland Larsen", null, 0, 0));
//		data.add(new Data(7, "Tango Wall", "Main wall", "Bakkeslask", T.BOLT, "8bb. Topp 50!", 1, "7+", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(8, "Tango Wall", "Main wall", "Adams Family", T.BOLT, "18bb", 1, "8-", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(9, "Tango Wall", "Main wall", "Never Ending Story", T.BOLT, "20bb. It's aint over before it's over", 1, "n/a", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(10, "Tango Wall", "Main wall", "Drillpikene", T.TRAD, null, 1, "n/a", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(11, "Tango Wall", "Main wall", "Gla' på sva", T.BOLT, "6bb. Grei oppvarming.", 1, "5-", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(12, "Tango Wall", "Main wall", "Gla' på sva direkte", T.BOLT, "6bb. Noe sær variant. Lengdeavhengig.", 1, "7-/7", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(13, "Tango Wall", "Main wall", "Beach-party", T.BOLT, "19bb", 1, "7+", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(14, "Tango Wall", "Main wall", "Ut i det blå", T.BOLT, "18bb. Faktisk ikke vanskeligere, er det mulig? Fjellklatrere vil le hele veien til toppen.", 1, "7-", "Nils Rune Birkeland,Arild Solbakken", null, 0, 0));
//		data.add(new Data(15, "Tango Wall", "Main wall", "Tobias i tårnet", T.BOLT, "14bb", 1, "7+", "Tobias Brodahl", null, 0, 0));
//		data.add(new Data(16, "Tango Wall", "Main wall", "Prosjekt", T.BOLT, "Åpent prosjekt. Mangler 3 bolter i starten. Kreatør: Michael Helgestad.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(17, "Tango Wall", "Main wall", "Mystisk kjærlighet", T.BOLT, "13bb. Veggens definitive klassiker.", 1, "9-", "Runar Carlsen", null, 0, 0));
//		data.add(new Data(18, "Tango Wall", "Main wall", "Ekte følelser", T.BOLT, "22bb. Klar en klassiker, men klarer du denne?", 1, "8+", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(19, "Tango Wall", "Main wall", "Ekte kjærlighet", T.BOLT, "18bb. Første del av 'Mystisk kjærlighet' + andre del av 'Ekte følelser'.", 1, "9-/9", "Nils Rune Birkeland", null, 0, 0));
//		data.add(new Data(20, "Tango Wall", "Main wall", "Tøffe Tøflus", T.BOLT, "5bb. Introsvaet.", 1, "n/a", "Runar Carlsen", null, 0, 0));
//		data.add(new Data(21, "Tango Wall", "Main wall", "Sex Pluss", T.BOLT, "15bb. NB: 70m tau! Spektakulær klatring.Topp 50!", 1, "6+", "Runar Carlsen", null, 0, 0));
//		data.add(new Data(22, "Tango Wall", "Main wall", "Flying Start", T.BOLT, "7bb", 1, "n/a", "Øyvind Skjeggstad", null, 0, 0));
//		data.add(new Data(23, "Tango Wall", "Main wall", "Flying Start Direkte", T.BOLT, "7bb", 1, "6+", "Øyvind Skjeggstad", null, 0, 0));
//		data.add(new Data(24, "Tango Wall", "Main wall", "Jenter som kommer og jenter som går", T.BOLT, "6bb. Boltet av Nils Rune Birkeland, pusset av Jan Are Otnes.", 1, "6+", "Christine Kielland Larsen", null, 0, 0));
//		data.add(new Data(25, "Tango Wall", "Main wall", "Gutten i røyken", T.BOLT, "5bb", 1, "5+", "Arild Solbakken", null, 0, 0));
//		data.add(new Data(26, "Tango Wall", "Main wall", "Sikker som banken", T.BOLT, "4bb. Sponset av den lokale banken. Ruta het opprinnelig 'Men du kan jo aldri være trygg'...", 1, "6+", "Linett Eriksen Birkeland", null, 0, 0));
//		data.add(new Data(1, "Tjømsvågen", "Ramsdalen", "Walt Disney", T.TRAD, "Lett og ganske fin klatring.", 2, "n/a", "Trond Årre,Morgen G. Larsen,Jonny Grundeland", "1998-01-01", 0, 0));
//		data.add(new Data(1, "Tjømsvågen", "Tjømsvågen", "Kjetils ukjente?", T.TRAD, null, 1, "n/a", "Kjetil Nielsen", null, 0, 0));
//		data.add(new Data(2, "Tjømsvågen", "Tjømsvågen", "Jomfruturen", T.TRAD, "Fin klatring. Noe kompakt.", 2, "n/a", "Andreas Loland,Arnt Rino Høiland,Jonny Grundeland", "1994-01-01", 0, 0));
//		data.add(new Data(3, "Tjømsvågen", "Tjømsvågen", "Lengten hjem", T.TRAD, "Fin klatring og mer direkte linje opp tilo markert midt i veggen. Deretter følges rute nr 2 'jomfruturen'.", 1, "5+", "Dag Palmar Høiland,Jonny Grundeland", "1995-01-01", 0, 0));
//		data.add(new Data(4, "Tjømsvågen", "Tjømsvågen", "Prosjekt*", T.TRAD, "Super rissklatring i siste taulengde.", 2, "6+", null, null, 0, 0));
//		data.add(new Data(5, "Tjømsvågen", "Tjømsvågen", "Hævvl", T.TRAD, "Grumsete klatring i markert renne til høyre på veggen. Anbefales ikke.", 1, "5+", "Dag Palmar Høiland,Jonny Grundeland", "1995-01-01", 0, 0));
//		data.add(new Data(1, "Ulvsvika", "Doveggen", "Bimmelim", T.BOLT, "4bb. Traverserende mot venstre i en formasjon.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Ulvsvika", "Doveggen", "Bommelom", T.BOLT, "4bb", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(3, "Ulvsvika", "Doveggen", "Ditt navn er svakhet", T.BOLT, "3bb", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(1, "Ulvsvika", "Hengestabben", "Fiskeren", T.BOLT, "4bb. Kort  men artig rute like over vannet. Pass på bølgeskvulp.", 1, "6-", "Jul Eirik Olsen", null, 0, 0));
//		data.add(new Data(2, "Ulvsvika", "Hengestabben", "Salt & Vinegar", T.TRAD, "Kort kaminklatring.", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(3, "Ulvsvika", "Hengestabben", "Brillefin", T.BOLT, "3bb", 1, "n/a", "Erlend Gauksås", null, 0, 0));
//		data.add(new Data(4, "Ulvsvika", "Hengestabben", "Piece of cake", T.BOLT, "4bb", 1, "4-", "Jul Eirik Olsen", null, 0, 0));
//		data.add(new Data(5, "Ulvsvika", "Hengestabben", "Kryssordnerd", T.BOLT, "5bb. Ganske enkel, men flott svaklatring.", 1, "n/a", "Erlend Gauksås", null, 0, 0));
//		data.add(new Data(6, "Ulvsvika", "Hengestabben", "Dra til sjøs", T.BOLT, "5bb. Flott svaklatring i tynne formasjoner.", 1, "4+", "Jul Eirik Olsen", null, 0, 0));
//		data.add(new Data(1, "Ulvsvika", "Tean i tanga", "Salto Fritz", T.BOLT, "5bb", 1, "n/a", "Christian Busk Jervelund", "2012-01-01", 0, 0));
//		data.add(new Data(2, "Ulvsvika", "Tean i tanga", "Verbalvold", T.BOLT, "5bb", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(3, "Ulvsvika", "Tean i tanga", "Tean i tanga", T.BOLT, "6bb. Flott klatring i artige formasjoner.", 1, "n/a", "Tor Olav Gauksås", null, 0, 0));
//		data.add(new Data(4, "Ulvsvika", "Tean i tanga", "Åndebrettet", T.BOLT, "5bb", 1, "7-", "Christian Busk Jervelund", "2012-01-01", 0, 0));
//		data.add(new Data(5, "Ulvsvika", "Tean i tanga", "Ulv, ulv", T.BOLT, "4bb. Ganske enkel, men flott klaring på gode tak.", 1, "n/a", "Eirik Olsen", null, 0, 0));
//		data.add(new Data(6, "Ulvsvika", "Tean i tanga", "Hilsen fra han far", T.BOLT, "4bb. Flott svaklatring i tynne formasjoner.", 1, "n/a", "Siri Hesland Engen", null, 0, 0));
//		data.add(new Data(1, "Voieveggen", "Sektor 2", "Åpent prosjekt", T.TOPROPE, null, 1, "n/a", null, null, 0, 0));
//		data.add(new Data(2, "Voieveggen", "Sektor 3", "Åpent Prosjekt 2", T.BOLT, "5bb", 1, "n/a", null, null, 0, 0));
//		data.add(new Data(3, "Voieveggen", "Sektor 4", "Jomfruen", T.BOLT, "5bb", 1, "5+", null, null, 0, 0));
//		data.add(new Data(1, "Voieveggen", "Wall of blame", "Blomstergutten", T.BOLT, "5bb", 1, "4+", null, null, 0, 0));
//		data.add(new Data(2, "Voieveggen", "Wall of blame", "MacGyver", T.BOLT, "6bb", 1, "6-", null, null, 0, 0));
//		data.add(new Data(3, "Voieveggen", "Wall of blame", "Lommedamen", T.TRAD, null, 1, "5-", null, null, 0, 0));
//		data.add(new Data(4, "Voieveggen", "Wall of blame", "Samen", T.BOLT, "6bb. Balanse på lister og artige undertak.", 1, "6-", null, null, 0, 0));
//		data.add(new Data(5, "Voieveggen", "Wall of blame", "Barbie", T.BOLT, "7bb. Balanse på lister og små tak.", 1, "6-", null, null, 0, 0));
//		data.add(new Data(6, "Voieveggen", "Wall of blame", "Stompen", T.BOLT, "7bb. Artig start.", 1, "6-", null, null, 0, 0));
//		data.add(new Data(7, "Voieveggen", "Wall of blame", "Grisen", T.MIXED, "Kjempesprekk.", 1, "5+", null, null, 0, 0));
//		data.add(new Data(8, "Voieveggen", "Wall of blame", "Kattekvinnen", T.BOLT, "4bb. Tøffest i starten.", 1, "5+", null, null, 0, 0));
//		data.add(new Data(9, "Voieveggen", "Wall of blame", "Lillemor", T.BOLT, "4bb. Bratt og kort.", 1, "7+", null, null, 0, 0));
//		data.add(new Data(10, "Voieveggen", "Wall of blame", "Kristiansands Fineste", T.BOLT, "4bb. Bratt og kort, ikke nødvendigvis byens fineste rute.", 1, "n/a", null, null, 0, 0));
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
//				List<UserSearch> users = c.getBuldreinfoRepo().getUserSearch(AUTH_USER_ID, user);
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
//		Problem p = new Problem(idArea, false, false, null, null, null, false, idSector, false, false, null, null, null, 0, 0, null, null, -1, -1, null, -1, false, false, false, d.getNr(), d.getProblem(), null, d.getComment(), null, d.getGrade().replaceAll(" ", ""), d.getFaDate(), null, fa, d.getLat(), d.getLng(), null, 0, 0, false, null, t, false, 0, null, null, null, null, null, null);
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
//		Area a = new Area(REGION_ID, null, -1, false, false, false, false, null, null, false, d.getArea(), null, 0, 0, 0, 0, null, null, null, 0);
//		Redirect r = c.getBuldreinfoRepo().setArea(new MetaHelper().getSetup(REGION_ID), AUTH_USER_ID, a, null);
//		return r.getIdArea();
//	}
//
//	private int upsertSector(DbConnection c, int idArea, Data d) throws IOException, SQLException, NoSuchAlgorithmException, InterruptedException {
//		Area a = Preconditions.checkNotNull(c.getBuldreinfoRepo().getArea(new MetaHelper().getSetup(REGION_ID), AUTH_USER_ID, idArea));
//		for (Area.Sector s : a.getSectors()) {
//			if (s.getName().equals(d.getSector())) {
//				return s.getId();
//			}
//		}
//		Sector s = new Sector(false, idArea, false, false, null, null, false, a.getName(), null, -1, false, false, false, d.getSector(), null, null, null, 0, 0, null, null, null, null, null, 0);
//		Redirect r = c.getBuldreinfoRepo().setSector(AUTH_USER_ID, false, new MetaHelper().getSetup(REGION_ID), s, null);
//		return r.getIdSector();
//	}
//}