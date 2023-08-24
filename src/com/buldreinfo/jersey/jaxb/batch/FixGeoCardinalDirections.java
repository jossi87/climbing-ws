package com.buldreinfo.jersey.jaxb.batch;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.helpers.geocardinaldirection.GeoCardinalDirectionCalculator;

public class FixGeoCardinalDirections {
	private static Logger logger = LogManager.getLogger();
	
	public static void main(String[] args) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT a.name area_name, s.name sector_name, s.polygon_coords FROM area a, sector s WHERE a.region_id=4 AND a.id=s.area_id AND s.polygon_coords IS NOT NULL AND a.name='Gloppedalen' ORDER BY a.name, s.name");
					ResultSet rst = ps.executeQuery();) {
				while (rst.next()) {
					String areaName = rst.getString("area_name");
					String sectorName = rst.getString("sector_name");
					String polygonCoords = rst.getString("polygon_coords");
					GeoCardinalDirectionCalculator calc = new GeoCardinalDirectionCalculator(polygonCoords);
					logger.debug("{} {}: {} - {}", areaName, sectorName, calc.getCardinalDirection(), calc.getVector());
				}
			}
			c.setSuccess();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
}
