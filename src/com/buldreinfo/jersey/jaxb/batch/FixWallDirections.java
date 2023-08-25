package com.buldreinfo.jersey.jaxb.batch;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GeoHelper;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;

public class FixWallDirections {
	private static Logger logger = LogManager.getLogger();

	public static void main(String[] args) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.name area_name, s.name sector_name, s.id sector_id, s.polygon_coords FROM type t, region_type rt, area a, sector s WHERE t.group='Climbing' AND t.id=rt.type_id AND rt.region_id=a.region_id AND a.id=s.area_id AND s.polygon_coords IS NOT NULL AND s.wall_direction IS NULL GROUP BY a.name, s.name, s.id, s.polygon_coords ORDER BY a.name, s.name");
					ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					String areaName = rst.getString("area_name");
					int sectorId = rst.getInt("sector_id");
					String sectorName = rst.getString("sector_name");
					String polygonCoords = rst.getString("polygon_coords");
					String wallDirection = null;
					try {
						GeoHelper calc = new GeoHelper();
						wallDirection = calc.getWallDirection(polygonCoords);
						calc.debug();
					} catch (Exception e) {
						logger.warn(e.getMessage(), e);
					}
					logger.debug("{} - {}: {}", areaName, sectorName, wallDirection);
					if (wallDirection != null) {
						try (PreparedStatement ps2 = c.getConnection().prepareStatement("UPDATE sector SET wall_direction=? WHERE id=?")) {
							ps2.setString(1, wallDirection);
							ps2.setInt(2, sectorId);
							ps2.execute();
						}
					}
				}
			}
			c.setSuccess();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
}
