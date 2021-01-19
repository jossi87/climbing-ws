package com.buldreinfo.jersey.jaxb.batch;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.metadata.MetaHelper;
import com.buldreinfo.jersey.jaxb.metadata.beans.Setup;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.FaUser;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Sector;
import com.buldreinfo.jersey.jaxb.model.Type;
import com.buldreinfo.jersey.jaxb.model.User;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class FillProblems {
	private static Logger logger = LogManager.getLogger();
	public static enum T {BOLT, TRAD, MIXED, TOPROPE, AID, AIDTRAD, ICE};

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
		private final double lat;
		private final double lng;
		public Data(int nr, String area, String sector, String problem, T t, String comment, int numPitches, String grade, String fa, String faDate, double lng, double lat) { // TODO REVERT
			if (t.equals(T.BOLT)) {
				this.typeId = 2;
			}
			else if (t.equals(T.TRAD)) {
				this.typeId = 3;
			}
			else if (t.equals(T.MIXED)) {
				this.typeId = 4;
			}
			else if (t.equals(T.TOPROPE)) {
				this.typeId = 5;
			}
			else if (t.equals(T.AID)) {
				this.typeId = 6;
			}
			else if (t.equals(T.AIDTRAD)) {
				this.typeId = 7;
			}
			else if (t.equals(T.ICE)) {
				this.typeId = 10;	
			}
			else {
				throw new RuntimeException("Invalid t=" + t);
			}
			this.nr = nr;
			this.area = area;
			this.sector = sector;
			this.problem = problem.replace(" (nat)", "").replace(" (miks)", "");
			this.comment = comment;
			this.numPitches = numPitches;
			this.grade = grade;
			this.fa = fa;
			this.faDate = faDate;
			this.lat = lat;
			this.lng = lng;
		}
		public double getLng() {
			return lng;
		}
		public double getLat() {
			return lat;
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
		@Override
		public String toString() {
			return "Data [typeId=" + typeId + ", nr=" + nr + ", area=" + area + ", sector=" + sector + ", problem="
					+ problem + ", comment=" + comment + ", grade=" + grade + ", fa=" + fa + ", faDate=" + faDate + "]";
		}
	}
	private final static int AUTH_USER_ID = 1;
	private final static int REGION_ID = 16;
	private final Setup setup;

	public static void main(String[] args) {
		new FillProblems();
	}

	public FillProblems() {
		this.setup = new MetaHelper().getSetup(REGION_ID);
		List<Data> data = new ArrayList<>();
		// FA-date: yyyy-MM-dd
		data.add(new Data(1, "Sandnes and Gjesdal", "Kråkedal", "Krågedal-is", T.ICE, "50m - Short friendly option in the farmland. Walk off to climbers right.", 1, "WI3", null, null, 5.93097222222222,58.8411388888889));
		data.add(new Data(1, "Sandnes and Gjesdal", "Svihus", "Svihus-is", T.ICE, "80m - Often fragile ice on first pitch. Second pitch is easy mixed. Walk off to climbers right.", 1, "WI4", null, null, 5.93519444444444,58.81675));
		data.add(new Data(1, "Sandnes and Gjesdal", "Dansen", "Dansen", T.ICE, "100m - Easy and very low altitude options close to the summer crag. Rap down the routes on trees.", 1, "WI3", "Harald Bjørgen, Nils Engelstad", "1983-01-01", 0,0));
		data.add(new Data(2, "Sandnes and Gjesdal", "Dansen", "Mazurka", T.ICE, "50m - Easy and very low altitude options close to the summer crag. Rap down the routes on trees.", 1, "WI4", "Ture Bjørgen, Harald Bjørgen, L. Bjørge", "1996-01-01", 5.96138888888889,58.8953333333333));
		data.add(new Data(1, "Sandnes and Gjesdal", "Madland", "Nevland-is", T.ICE, "45m - Thin slab and short pillar. Rap down off a tree.", 1, "WI3+", null, null, 6.02283333333333,58.7821388888889));
		data.add(new Data(1, "Sandnes and Gjesdal", "Oltedal", "Røyrbekken", T.ICE, "250m - Consistent route up a stream. Walk down the top half to climbers left then rap on abalakovs/trees.", 1, "WI3", "John Fivelsdal, Roberts", null, 6.14211111111111,58.8374722222222));
		data.add(new Data(2, "Sandnes and Gjesdal", "Oltedal", "Mørkaisen", T.ICE, "150m - A fine sustained route with few ledges. Not often fat. Rap down the route on abalakovs.", 1, "WI4+", null, null, 0,0));
		data.add(new Data(1, "Sandnes and Gjesdal", "Oltesvik quarry", "Various lines", T.ICE, "WI4-5, 20-80m - Low level cragging in a quarry. Usually thin ice. Might be affected by active quarrying from above!", 1, "WI4", null, null, 6.12955555555556,58.8383611111111));
		data.add(new Data(1, "Sandnes and Gjesdal", "Bersagel", "Bersagel", T.ICE, "120m", 1, "WI3", "Rolf Motland, Jensen", "1996-01-01", 5.9501634,58.9377847));
		data.add(new Data(1, "Dirdal and Frafjord", "Dirdal", "Seaside Ice", T.ICE, "180m - The closest route to the sea in Dirdal. Rap off trees.", 1, "WI4", null, null, 6.17225,58.834));
		data.add(new Data(2, "Dirdal and Frafjord", "Dirdal", "Frøylandsbekken", T.ICE, "300m - Ca. 6 fine and varied pitches, not all visible from the road. Walk off to climbers left.", 1, "WI4", "Rolf Motland, Ture Bjørgen, L. Bjørge", "1997-01-01", 6.19658333333333,58.8188055555556));
		data.add(new Data(3, "Dirdal and Frafjord", "Dirdal", "Lindelandsbekken", T.ICE, "450m - Aesthetic long route at the end of Dirdal. Forms most years but rarely looks fat. Best with no snow. Walk off to climbers right passing below a pillar (Den Lange Anmarsjen, WI5).", 1, "WI4", null, null, 6.22336111111111,58.8091111111111));
		data.add(new Data(4, "Dirdal and Frafjord", "Dirdal", "Bjødnakrå", T.ICE, "100m - Sustained steepness with a rough approach.", 1, "WI5+", null, null, 6.18180555555556,58.8225277777778));
		data.add(new Data(1, "Dirdal and Frafjord", "Giljejuvet", "Various lines", T.ICE, "WI3-5, 30-60m - Cragging venue with several single pitch and one 2-pitch line. Rap off trees.", 1, "WI3", null, null, 6.28186111111111,58.7888333333333));
		data.add(new Data(1, "Dirdal and Frafjord", "Frafjord", "Various lines", T.ICE, "WI4-5+, 200-500m - Low altitude, needs a sustained freeze to sea level. Mega lines.", 1, "WI4", null, null, 0,0));
		data.add(new Data(1, "Dirdal and Frafjord", "Månådalen", "Lille Månåfossen", T.ICE, "180m - High quality moderate route, impressive when fat. Walk ca 300m to climbers right and rap from trees.", 1, "WI3+", "John Fivelsdal, Price", "1979-01-01", 6.39477777777778,58.8577777777778));
		data.add(new Data(2, "Dirdal and Frafjord", "Månådalen", "The Snail Trail", T.ICE, "WI3+/M3, 80m - A thin ice/mixed route right of Lille Månofossen. Walk climbers right and rap off trees", 1, "WI3+", null, null, 6.3935,58.8569444444444));
		data.add(new Data(1, "Gloppedalen and Bjerkheim", "Gloppedalen", "Jyritona", T.ICE, "180m - Hidden gully with three good pitches and walking in between. Walk / rap off trees to climbers right.", 1, "WI3+", null, null, 6.30175,58.7730833333333));
		data.add(new Data(2, "Gloppedalen and Bjerkheim", "Gloppedalen", "Unknown Gloppedalen", T.ICE, "120m - Easy p1, a steep curtain on p2, short p3 to top out. Rap from trees.", 1, "WI4+", null, null, 6.30686111111111,58.7739166666667));
		data.add(new Data(1, "Gloppedalen and Bjerkheim", "Bjerkheim", "Lindelibekken", T.ICE, "250m - Four pitches of easy climbing leads to a much steeper finish. Rock gear useful. Walk off climbers left down a steep", 1, "WI5", null, null, 6.22180555555556,58.7368333333333));
		data.add(new Data(1, "Hunnedalen", "Lower", "Lunchbreak", T.ICE, "90m - The first route on Hunnedalen’s south side. A beautiful exposed top pitch. Rap down from trees.", 1, "WI5+", null, null, 6.33305555555556,58.78725));
		data.add(new Data(2, "Hunnedalen", "Lower", "Byrkjedalsfossen Direct", T.ICE, "350m - One of the most impressive lines in the region but hard to find in good condition from top to bottom. Ca. 8 pitches of climbing split into two tiers. Walk off the back and return via the road.", 1, "WI5+", "Morten Diesen, John Fivelsdal", null, 6.33447222222222,58.7875));
		data.add(new Data(3, "Hunnedalen", "Lower", "Almabekken", T.ICE, "150m - Often formed but catches the sun. Rap off trees climbers right.", 1, "WI3", null, null, 6.33025,58.7939444444444));
		data.add(new Data(4, "Hunnedalen", "Lower", "Island", T.ICE, "200m - Reliable high quality ice in a deep gully. Rap off abalakovs down the route.", 1, "WI4", "Harald Bjørgen, John Fivelsdal", "1982-01-01", 6.34672222222222,58.7924722222222));
		data.add(new Data(5, "Hunnedalen", "Lower", "Aleksandersfossen", T.ICE, "160m - A wide easy flow. Walk off climbers left or rap off abalakovs down the route.", 1, "WI3", null, null, 6.35213888888889,58.80475));
		data.add(new Data(6, "Hunnedalen", "Lower", "Motlandsisen", T.ICE, "350m - Long high quality gully. The best pitches are hidden from view. Rap down the route on abalakovs/trees.", 1, "WI4+", "Nils Engelstad, Mathiesen", null, 6.36722222222222,58.8062777777778));
		data.add(new Data(7, "Hunnedalen", "Lower", "Storebekken", T.ICE, "200m - A load of easy rambling ice to some hidden steeper pitches. Walk / rap off trees climbers right.", 1, "WI4", null, null, 6.50311111111111,58.8431944444444));
		data.add(new Data(8, "Hunnedalen", "Lower", "Tingeling", T.ICE, "120m - Quick to form, but gets buried in deep snow. Walk off climbers right", 1, "WI4", "Ture Bjørgen, Rolf Motland", "1994-01-01", 6.44027777777778,58.8173888888889));
		data.add(new Data(9, "Hunnedalen", "Lower", "50:50", T.ICE, "120m - Walk off climbers right", 1, "WI4+", null, null, 6.44027777777778,58.8173888888889));
		data.add(new Data(10, "Hunnedalen", "Lower", "Pedigri Pål", T.ICE, "200m - High quality streak in the back of an atmospheric cleft. Rap down the route on abalakovs/trees.", 1, "WI4+", "John Fivelsdal, Trym Sæland", "1998-01-01", 6.47861111111111,58.8395555555556));
		data.add(new Data(11, "Hunnedalen", "Lower", "Jonny go Home", T.ICE, "200m - The cave on p2 takes a while to build up enough ice", 1, "WI5", "John Fivelsdal, Glenn Gjessing", "1996-01-01", 6.48038888888889,58.84));
		data.add(new Data(12, "Hunnedalen", "Lower", "Sigbjørnsfossen", T.ICE, "150m - Reliable and high quality early season option. Gets buried with snow. Walk off down a ramp climbers right.", 1, "WI3+", "Nils Engelstad, Berge", "1979-01-01", 6.53513888888889,58.8580277777778));
		data.add(new Data(1, "Hunnedalen", "Upper", "Røde Kors isen", T.ICE, "250m - Worthwhile if it’s not buried. Can be high water flow under the ice. Walk off climbers left.", 1, "WI3+", "Harald Bjørgen, Nils Engelstad", "1982-01-01", 6.55788888888889,58.8598888888889));
		data.add(new Data(2, "Hunnedalen", "Upper", "Come On Ice", T.ICE, "80m - Easy, early season only. Rap off trees.", 1, "WI3", null, null, 6.59319444444444,58.874));
		data.add(new Data(3, "Hunnedalen", "Upper", "Tveitaisen", T.ICE, "200m - Often thin and crappy ice on the slabs at the start. Walk off climbers right.", 1, "WI4", "Harald Bjørgen, H. Lie, Ture Bjørgen", "1982-01-01", 6.60558333333333,58.8764444444444));
		data.add(new Data(4, "Hunnedalen", "Upper", "Borderline", T.ICE, "150m - Early season options when there’s little snow. Long walk off climbers right, or down the gully climbers left (beware avalanche conditions)", 1, "WI3+", null, null, 6.61175,58.8781944444444));
		data.add(new Data(5, "Hunnedalen", "Upper", "Fylkesgrense", T.ICE, "150m - Early season options when there’s little snow. Long walk off climbers right, or down the gully climbers left (beware avalanche conditions)", 1, "WI3+", null, null, 6.61161111111111,58.8782222222222));
		data.add(new Data(6, "Hunnedalen", "Upper", "Knekkebrørenna", T.ICE, "WI4/M4, 80m - Mixed gully climbing, with ice, snow and chockstones. Grade will vary greatly with snow depth. Walk off climbers left or rap down the route on abalokovs/rock gear", 1, "WI4", "John Fivelsdal, Trym Sæland", "1999-01-01", 6.63305555555556,58.8848888888889));
		data.add(new Data(7, "Hunnedalen", "Upper", "Memphis", T.ICE, "100m - Reliable high quality routes. Rap down routes on abalakovs, or a long walk climbers right.", 1, "WI4+", "Ture Bjørgen, Jensen", "1995-01-01", 6.64938888888889,58.8864722222222));
		data.add(new Data(8, "Hunnedalen", "Upper", "Elvis", T.ICE, "100m - Reliable high quality routes. Rap down routes on abalakovs, or a long walk climbers right.", 1, "WI4+", "Ture Bjørgen, Jensen, Sindre Bø", "1995-01-01", 6.64938888888889,58.8864722222222));
		data.add(new Data(9, "Hunnedalen", "Upper", "Sick Boy", T.ICE, "120m - A wild and steep route; one of the hardest in Hunnedalen. A mixed start is possible up the gully if the ice doesn’t reach the ground. Descend by walking south then a single rap where the cliff is lower.", 1, "WI5+", null, null, 6.64952777777778,58.8854444444444));
		data.add(new Data(10, "Hunnedalen", "Upper", "Various lines", T.ICE, "WI3-WI4s, 60m - Many cragging options in this area, some close to the road and others further in, beyond Elvis/Memphis. Rap off generally", 1, "WI3", null, null, 6.64933333333333,58.8855555555556));
		data.add(new Data(11, "Hunnedalen", "Upper", "Roadside Iceclimbers", T.ICE, "180m - One of Hunnedalens best - two steep tiers with pillars. Walk off climbers left or rap on abalakovs.", 1, "WI5+", null, null, 6.65230555555556,58.8893611111111));
		data.add(new Data(12, "Hunnedalen", "Upper", "Prime Pussey", T.ICE, "70m - Short freestanding pillar to start, not always touching down. Walk off climbers right.", 1, "WI4+", "John Fivelsdal, Christian Jønsson", "1997-01-01", 6.66947222222222,58.8923611111111));
		data.add(new Data(13, "Hunnedalen", "Upper", "Erik Blødoks", T.ICE, "80m - Two interesting pitches. Variations possible. Rap down from abalakovs/trees.", 1, "WI4", "John Fivelsdal, E. Nyhus", "1996-01-01", 6.66952777777778,58.8923611111111));
		data.add(new Data(14, "Hunnedalen", "Upper", "Brøytebilmannen", T.ICE, "80m - One of the most reliable routes in Hunnedalen a wide flow then a narrow chimney. Rap off trees climbers right.", 1, "WI4", "John Fivelsdal, A. Gjessing", "1996-01-01", 6.66952777777778,58.8923611111111));
		data.add(new Data(15, "Hunnedalen", "Upper", "Captain Powder", T.ICE, "50m", 1, "WI4", "Morten Diesen", "1998-01-01", 6.67111111111111,58.893));
		data.add(new Data(16, "Hunnedalen", "Upper", "Ice Cream Man", T.ICE, "50m", 1, "WI4", "John Fivelsdal, Edvard Middelthon", "1998-01-01", 6.67111111111111,58.893));
		data.add(new Data(17, "Hunnedalen", "Upper", "Svenskerute", T.ICE, "50m", 1, "WI4", null, null, 6.67111111111111,58.893));
		data.add(new Data(18, "Hunnedalen", "Upper", "Turistisen", T.ICE, "100m - Steepest on the first pitch, the rest gets buried in snow. Walk/rap off trees climbers right", 1, "WI3+", "Rolf Bae, Edvard Middelthon", "1995-01-01", 6.65783333333333,58.8950277777778));
		data.add(new Data(19, "Hunnedalen", "Upper", "Various Link Ups", T.ICE, "100m - Linking up of two tiers of cliffs with walking between can give some good pitches. Walk off climbers right.", 1, "WI4", null, null, 6.67830555555556,58.8968055555556));
		data.add(new Data(20, "Hunnedalen", "Upper", "M-Isen", T.ICE, "40m - Single pitch with some possibilities either side. Sun affected. Rap off trees or abalakovs.", 1, "WI5", "Ture Bjørgen, Christian Jønsson", "1997-01-01", 6.66322222222222,58.89675));
		data.add(new Data(21, "Hunnedalen", "Upper", "Buemerkeveggen various lines", T.ICE, "WI4-WI5, 25m - Short steep cragging options. Sun affected.", 1, "WI4", null, null, 6.68125,58.9013333333333));
		data.add(new Data(22, "Hunnedalen", "Upper", "Glassmagasinet", T.ICE, "70m", 1, "WI5", "John Fivelsdal, Ø. Stangeland", "1999-01-01", 6.69005555555556,58.89825));
		data.add(new Data(23, "Hunnedalen", "Upper", "November-is", T.ICE, "120m - An aesthetic bonded pillar. Rap down the route on abalakovs.", 1, "WI3", null, null, 6.68769444444444,58.8966944444444));
		data.add(new Data(24, "Hunnedalen", "Upper", "Sesilåmi", T.ICE, "70m - An aesthetic bonded pillar. Rap down the route on abalakovs.", 1, "WI5", "John Fivelsdal, A. Gjessing", "1995-01-01", 6.71430555555556,58.903));
		data.add(new Data(25, "Hunnedalen", "Upper", "Various routes Lorten area", T.ICE, "WI3-WI5, 20-50m - Single pitch cragging options either side of the FV45 road.", 1, "WI3", null, null, 6.72725,58.91025));
		data.add(new Data(26, "Hunnedalen", "Upper", "Lysebekken", T.ICE, "80m - The highest route around and often the first to form. A steeper left hand start is sometimes possible. Faces south west - the sun destroys it quickly. Walk off climbers left or rap off boulders.", 1, "WI4", "Harald Bjørgen, Ben Johnsen", "1982-01-01", 6.70797222222222,58.93));
		data.add(new Data(27, "Hunnedalen", "Upper", "Tredje advent", T.ICE, "100m - A few hundred metres right of Lysebekken", 1, "WI4+", "John Fivelsdal, Christian Jønsson", "1997-01-01", 6.71027777777778,58.9293611111111));
		data.add(new Data(1, "Sirdal", "Sirdal", "Klubbejeina", T.ICE, "80m - Two nice pitches. Walk to climbers left and rap off trees", 1, "WI3", null, null, 6.80211111111111,58.8986388888889));
		data.add(new Data(2, "Sirdal", "Sirdal", "Fidjelandskåret", T.ICE, "120m - Rap off trees to climbers left.", 1, "WI3+", null, null, 6.92711111111111,58.9886111111111));
		data.add(new Data(3, "Sirdal", "Sirdal", "Unknown, Sirdal", T.ICE, "100m - Two good pitches. Rap off trees / abalokovs.", 1, "WI4+", null, null, 6.80655555555556,58.8618055555556));
		data.add(new Data(4, "Sirdal", "Sirdal", "Sinnes Ice Crag – various lines", T.ICE, "WI4-WI5, 15-25m - Fairly reliable steep cragging with many options. Plenty of trees for top roping.", 1, "WI4", null, null, 6.78333333333333,58.8253333333333));
		data.add(new Data(5, "Sirdal", "Sirdal", "Riverdance", T.ICE, "250m - A quality line of yellow ice on the east side of the river (needs to be crossed to approach). Rap down on trees. The pic is taken in poor conditions.", 1, "WI4", null, null, 6.78738888888889,58.8179444444444));
		data.add(new Data(1, "Sirdal", "Øvre Sirdal", "Diederfossen", T.ICE, null, 1, "WI4", null, null, 6.74872222222222,58.7971111111111));
		data.add(new Data(1, "Forsand and Espedal", "Forsand", "Unknown name", T.ICE, "140m - Low level route seen from the ferry to Forsand. Needs a sustained freeze to sea level.", 1, "WI4", null, null, 6.11813888888889,58.8917777777778));
		data.add(new Data(2, "Forsand and Espedal", "Forsand", "Too Fat to Fly", T.ICE, "140m - Thin slabs, then a pillar, followed by an easy gully. Walk / rap off trees to climbers right", 1, "WI4+", null, null, 6.11794444444444,58.89175));
		data.add(new Data(3, "Forsand and Espedal", "Forsand", "Feeding the Rat", T.ICE, "WI4/M3, 70m - An aesthetic narrow ice streak with a mixed start. Walk / rap off trees climbers right.", 1, "WI4", null, null, 0,0));
		data.add(new Data(1, "Forsand and Espedal", "Espedal", "Tungebekken WI3", T.ICE, "350m - A long and scenic route. Walk off climbers left.", 1, "WI3", null, null, 6.28313888888889,58.9127222222222));
		data.add(new Data(2, "Forsand and Espedal", "Espedal", "Migaren", T.ICE, "300m?", 1, "WI5+", null, null, 6.25830555555556,58.9201944444444));
		data.add(new Data(3, "Forsand and Espedal", "Espedal", "Bjørnen Sover", T.ICE, "180m", 1, "WI4+", null, null, 6.32930555555556,58.9403888888889));
		data.add(new Data(1, "Hjelmeland", "Hjelmeland", "Sendingfossen - Various lines", T.ICE, "WI3-WI5, 30m - Cragging from the riverbed of a gorge. Lots of potential but low altitude. Rap in to the routes.", 1, "WI3", null, null, 6.37733333333333,59.1513333333333));

		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			for (Data d : data) {
				final int idArea = upsertArea(c, d);
				final int idSector = upsertSector(c, idArea, d);
				insertProblem(c, idArea, idSector, d);
			}
			c.setSuccess();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}

	private List<FaUser> getFas(DbConnection c, String fa) throws SQLException {
		List<FaUser> res = new ArrayList<>();
		if (!Strings.isNullOrEmpty(fa)) {
			String splitter = fa.contains("&")? "&" : ",";
			for (String user : fa.split(splitter)) {
				user = user.trim();
				int id = -1;
				List<User> users = c.getBuldreinfoRepo().getUserSearch(AUTH_USER_ID, user);
				if (!users.isEmpty()) {
					id = users.get(0).getId();
				}
				res.add(new FaUser(id, user, null));
			}
		}
		return res;
	}

	private void insertProblem(DbConnection c, int idArea, int idSector, Data d) throws IOException, SQLException, NoSuchAlgorithmException, InterruptedException, ParseException {
		logger.debug("insert {}", d);
		List<FaUser> fa = getFas(c, d.getFa());
		Type t = c.getBuldreinfoRepo().getTypes(REGION_ID).stream().filter(x -> x.getId() == d.getTypeId()).findFirst().get();
		Problem p = new Problem(idArea, false, false, null, idSector, false, false, null, 0, 0, null, null, -1, -1, null, -1, false, false, d.getNr(), d.getProblem(), d.getComment(), null, d.getGrade().replaceAll(" ", ""), d.getFaDate(), null, fa, d.getLat(), d.getLng(), null, 0, 0, false, null, t, false, 0);
		if (d.getNumPitches() > 1) {
			for (int nr = 1; nr <= d.getNumPitches(); nr++) {
				p.addSection(-1, nr, null, "n/a", new ArrayList<>());
			}
		}
		c.getBuldreinfoRepo().setProblem(AUTH_USER_ID, setup, p, null);
	}

	private int upsertArea(DbConnection c, Data d) throws IOException, SQLException, NoSuchAlgorithmException, InterruptedException {
		for (Area a : c.getBuldreinfoRepo().getAreaList(AUTH_USER_ID, REGION_ID)) {
			if (a.getName().equals(d.getArea())) {
				return a.getId();
			}
		}
		Area a = new Area(REGION_ID, null, -1, false, false, false, d.getArea(), null, 0, 0, 0, 0, null, null, 0);
		a = c.getBuldreinfoRepo().setArea(AUTH_USER_ID, REGION_ID, a, null);
		return a.getId();
	}

	private int upsertSector(DbConnection c, int idArea, Data d) throws IOException, SQLException, NoSuchAlgorithmException, InterruptedException {
		Area a = Preconditions.checkNotNull(c.getBuldreinfoRepo().getArea(AUTH_USER_ID, REGION_ID, idArea));
		for (Area.Sector s : a.getSectors()) {
			if (s.getName().equals(d.getSector())) {
				return s.getId();
			}
		}
		Sector s = new Sector(false, idArea, false, false, a.getName(), null, -1, false, false, d.getSector(), null, 0, 0, null, null, null, null, 0);
		s = c.getBuldreinfoRepo().setSector(AUTH_USER_ID, false, new MetaHelper().getSetup(REGION_ID), s, null);
		return s.getId();
	}
}