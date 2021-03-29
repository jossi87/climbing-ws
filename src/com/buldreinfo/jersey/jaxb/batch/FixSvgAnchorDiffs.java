package com.buldreinfo.jersey.jaxb.batch;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.google.common.collect.Sets;

public class FixSvgAnchorDiffs {
private static Logger logger = LogManager.getLogger();

	public class Topo {
		private final int nr;
		private final int id;
		private final String path;
		private final String pathStart;
		private final double x;
		private final double y;
		
		public Topo(int nr, int id, String path) {
			this.nr = nr;
			this.id = id;
			this.path = path;
			int ixY = path.lastIndexOf(" ");
			this.y = Double.parseDouble(path.substring(ixY).trim());
			int ixX = path.lastIndexOf(" ", ixY-1);
			this.x = Double.parseDouble(path.substring(ixX, ixY).trim());
			this.pathStart = path.substring(0, ixX).trim();
		}
		
		public int getNr() {
			return nr;
		}

		public int getId() {
			return id;
		}

		public String getPath() {
			return path;
		}

		public double getX() {
			return x;
		}

		public double getY() {
			return y;
		}
		
		public String getNewPath(double x, double y) {
			return pathStart + " " + (int)x + " " + (int)y;
		}
	}
	
	public static void main(String[] args) {
		new FixSvgAnchorDiffs();
	}

	public FixSvgAnchorDiffs() {
		Set<Integer> shouldNotHaveAnchor = Sets.newHashSet(
				5631, // Dale/Hovedveggen - venstre (Ataraxia)/Stepping on a Snake - 2. taulengde
				4251, // Dale/Fest for nervene/Siste skrik (åpent prosjekt)
				5259 // Øysteinshedlar/Øysteinshedlar/Millenium
				);
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			PreparedStatement ps = c.getConnection().prepareStatement("SELECT r.url region, a.name area_name, s.name sector_name, p.id problem_id, p.nr problem_nr, p.type_id, svg.id, svg.media_id, svg.path, svg.has_anchor, m.width, m.height FROM svg, media m, problem p, sector s, area a, region r WHERE svg.media_id=m.id AND svg.problem_id=p.id AND p.sector_id=s.id AND s.area_id=a.id AND a.region_id=r.id AND r.id IN (1,4) ORDER BY r.id, svg.media_id, p.nr");
			ResultSet rst = ps.executeQuery();
			int lastMediaId = 0;
			Set<Topo> topos = new LinkedHashSet<>();
			boolean first = true;
			while (rst.next()) {
				String region = rst.getString("region");
				String areaName = rst.getString("area_name");
				String sectorName = rst.getString("sector_name");
				int problemId = rst.getInt("problem_id");
				int problemNr = rst.getInt("problem_nr");
				int typeId = rst.getInt("type_id");
				int id = rst.getInt("id");
				int mediaId = rst.getInt("media_id");				
				String path = rst.getString("path").trim();
				boolean hasAnchor = rst.getBoolean("has_anchor");
				int size = Math.max(rst.getInt("width"), rst.getInt("height"));
				Topo topo = new Topo(problemNr, id, path);
				
				if (typeId == 2 && hasAnchor == shouldNotHaveAnchor.contains(problemId)) {
					logger.debug("UPDATE svg SET has_anchor=1 WHERE id={}; -- {}/{}/{}", topo.getId(), region, areaName, problemNr);
				}
				
				if (mediaId != lastMediaId) {
					lastMediaId = mediaId;
					topos.clear();
					first = true;
				}
				if (!topos.isEmpty()) {
					final double maxDiff = size/150;
					for (Topo t : topos) {
						double distance = calculateDistanceBetweenPoints(t.getX(), t.getY(), topo.getX(), topo.getY());
						if (distance > 0.0 && distance < maxDiff) {
							if (first) {
								logger.debug("-- {} - {} - {}", region, areaName, sectorName);
								first = false;
							}
							logger.debug("UPDATE svg SET path=\"{}\" WHERE id={}; -- {} merged with {} (distance={}, maxDiff={})", topo.getNewPath(t.getX(), t.getY()), topo.getId(), t.getNr(), topo.getNr(), distance, maxDiff);
							break;
						}
					}
				}
				topos.add(topo);
			}
			rst.close();
			ps.close();
			c.setSuccess();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
	
	private double calculateDistanceBetweenPoints(double x1, double y1, double x2, double y2) {       
	    return Math.sqrt((y2 - y1) * (y2 - y1) + (x2 - x1) * (x2 - x1));
	}
}
