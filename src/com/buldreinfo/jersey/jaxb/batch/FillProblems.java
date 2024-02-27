package com.buldreinfo.jersey.jaxb.batch;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.helpers.MetaHelper;
import com.buldreinfo.jersey.jaxb.helpers.Setup;
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
		public Data(int nr, String area, String sector, String problem, T t, String comment, int numPitches, String grade, String fa, String faDate) {
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
	private final static int REGION_ID = -1; // TODO Fill region
	private final Setup setup;

	public static void main(String[] args) {
		new FillProblems();
	}

	public FillProblems() {
		Preconditions.checkArgument(REGION_ID > 0, "Invalid REGION_ID=" + REGION_ID);
		this.setup = MetaHelper.getMeta().getSetup(REGION_ID);
		List<Data> data = new ArrayList<>();
		// TODO Fill data (FA-date: yyyy-MM-dd)
		data.add(new Data(1,"AREA","SECTOR","NAME", T.TRAD,"DESCRIPTION", 1,"6+","USER_1,USER_2&USER_3","9999-12-31"));
		Preconditions.checkArgument(data.size() > 1, "Invalid data");
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

	private List<User> getFas(DbConnection c, String fa) throws SQLException {
		List<User> res = new ArrayList<>();
		if (!Strings.isNullOrEmpty(fa)) {
			String splitter = fa.contains("&")? "&" : ",";
			for (String user : fa.split(splitter)) {
				user = user.trim();
				int id = -1;
				List<User> users = c.getBuldreinfoRepo().getUserSearch(AUTH_USER_ID, user);
				if (!users.isEmpty()) {
					id = users.get(0).id();
				}
				res.add(User.from(id, user));
			}
		}
		return res;
	}

	private void insertProblem(DbConnection c, int idArea, int idSector, Data d) throws IOException, SQLException, NoSuchAlgorithmException, InterruptedException, ParseException, ImageReadException, ImageWriteException {
		logger.debug("insert {}", d);
		List<User> fa = getFas(c, d.getFa());
		Type t = c.getBuldreinfoRepo().getTypes(REGION_ID).stream().filter(x -> x.id() == d.getTypeId()).findFirst().get();
		Problem p = new Problem(null, idArea, false, false, null, null, null, false, -1, -1, idSector, false, false, null, null, null, null, null, null, null, null, null, null, null, -1, null, false, false, false, d.getNr(), d.getProblem(), null, d.getComment(), null, d.getGrade().replaceAll(" ", ""), d.getFaDate(), null, fa, null, null, -1, 0, false, null, t, false, 0, null, null, null, null, null, null);
		if (d.getNumPitches() > 1) {
			for (int nr = 1; nr <= d.getNumPitches(); nr++) {
				p.addSection(-1, nr, null, "n/a", new ArrayList<>());
			}
		}
		c.getBuldreinfoRepo().setProblem(AUTH_USER_ID, setup, p, null);
	}

	private int upsertArea(DbConnection c, Data d) throws IOException, SQLException, NoSuchAlgorithmException, InterruptedException, ImageReadException, ImageWriteException, ParseException {
		for (Area a : c.getBuldreinfoRepo().getAreaList(AUTH_USER_ID, REGION_ID)) {
			if (a.getName().equals(d.getArea())) {
				return a.getId();
			}
		}
		Area a = new Area(null, REGION_ID, null, -1, false, false, false, false, null, null, false, 0, 0, d.getArea(), null, null, 0, 0, null, null, null, 0);
		Redirect r = c.getBuldreinfoRepo().setArea(MetaHelper.getMeta().getSetup(REGION_ID), AUTH_USER_ID, a, null);
		return r.idArea();
	}

	private int upsertSector(DbConnection c, int idArea, Data d) throws IOException, SQLException, NoSuchAlgorithmException, InterruptedException, ImageReadException, ImageWriteException, ParseException {
		Area a = Preconditions.checkNotNull(c.getBuldreinfoRepo().getArea(MetaHelper.getMeta().getSetup(REGION_ID), AUTH_USER_ID, idArea));
		for (AreaSector s : a.getSectors()) {
			if (s.getName().equals(d.getSector())) {
				return s.getId();
			}
		}
		Sector s = new Sector(null, false, idArea, false, false, null, null, false, idArea, idArea, null, null, -1, false, false, false, d.getSector(), null, null, null, null, null, null, null, null, null, null, null, 0);
		Redirect r = c.getBuldreinfoRepo().setSector(AUTH_USER_ID, false, MetaHelper.getMeta().getSetup(REGION_ID), s, null);
		return r.idSector();
	}
}