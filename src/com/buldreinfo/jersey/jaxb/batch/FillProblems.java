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
	
	private class Data {
		private final int typeId;
		private final int nr;
		private final String area;
		private final String sector;
		private final String problem;
		private final String comment;
		private final String grade;
		private final String fa;
		private final String faDate;
		public Data(int nr, String area, String sector, String problem, String comment, String grade, String fa, String faDate) {
			if (problem.endsWith(" (nat)")) {
				this.typeId = 3;
			}
			else if (problem.endsWith(" (miks)")) {
				this.typeId = 4;
			}
			else {
				this.typeId = 2;
			}
			this.nr = nr;
			this.area = area;
			this.sector = sector;
			this.problem = problem.replace(" (nat)", "").replace(" (miks)", "");
			this.comment = comment;
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
	private final static String TOKEN = "ee5208f2-3384-40eb-b29f-f90143d3195f";
	private final static int REGION_ID = 4;

	public static void main(String[] args) {
		new FillProblems();
	}
	
	public FillProblems() {
		List<Data> data = new ArrayList<>();
		// FA-Date sdf="yyyy-MM-dd" TODO
		//data.add(new Data(1, "Dale", "Dalehammeren", "Maurstien", null, "4", "T. Stendal", null));
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
			String splitter = fa.contains("&")? "&" : "/";
			for (String user : fa.split(splitter)) {
				user = user.trim();
				int id = -1;
				List<User> users = c.getBuldreinfoRepo().getUserSearch(TOKEN, user);
				if (!users.isEmpty()) {
					id = users.get(0).getId();
				}
				int ix = user.lastIndexOf(" ");
				String firstname = ix > 0? user.substring(0, ix) : user;
				String lastname = ix > 0? user.substring(ix+1) : null;
				res.add(new FaUser(id, firstname, lastname, null));
			}
		}
		return res;
	}
	
	private void insertProblem(DbConnection c, int idArea, int idSector, Data d) throws IOException, SQLException, NoSuchAlgorithmException, InterruptedException, ParseException {
		logger.debug("insert {}", d);
		List<FaUser> fa = getFas(c, d.getFa());
		Type t = c.getBuldreinfoRepo().getTypes(REGION_ID).stream().filter(x -> x.getId() == d.getTypeId()).findFirst().get();
		Problem p = new Problem(idArea, 0, null, idSector, 0, null, 0, 0, -1, 0, d.getNr(), d.getProblem(), d.getComment(), null, d.getGrade(), d.getFaDate(), fa, 0, 0, null, 0, 0, false, null, t);
		c.getBuldreinfoRepo().setProblem(TOKEN, REGION_ID, p, null);
	}
	
	private int upsertArea(DbConnection c, Data d) throws IOException, SQLException, NoSuchAlgorithmException, InterruptedException {
		for (Area a : c.getBuldreinfoRepo().getAreaList(TOKEN, REGION_ID)) {
			if (a.getName().equals(d.getArea())) {
				return a.getId();
			}
		}
		Area a = new Area(REGION_ID, -1, 0, d.getArea(), null, 0, 0, -1, null, null);
		a = c.getBuldreinfoRepo().setArea(TOKEN, a, null);
		return a.getId();
	}

	private int upsertSector(DbConnection c, int idArea, Data d) throws IOException, SQLException, NoSuchAlgorithmException, InterruptedException {
		Area a = Preconditions.checkNotNull(c.getBuldreinfoRepo().getArea(TOKEN, idArea));
		for (Area.Sector s : a.getSectors()) {
			if (s.getName().equals(d.getSector())) {
				return s.getId();
			}
		}
		Sector s = new Sector(idArea, 0, a.getName(), -1, 0, d.getSector(), null, 0, 0, null, null, null);
		s = c.getBuldreinfoRepo().setSector(TOKEN, REGION_ID, s, null);
		return s.getId();
	}
}