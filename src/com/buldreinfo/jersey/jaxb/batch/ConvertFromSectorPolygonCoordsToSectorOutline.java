package com.buldreinfo.jersey.jaxb.batch;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.buldreinfo.jersey.jaxb.db.ConnectionPoolProvider;
import com.buldreinfo.jersey.jaxb.db.DbConnection;
import com.buldreinfo.jersey.jaxb.helpers.GlobalFunctions;
import com.buldreinfo.jersey.jaxb.model.Coordinate;

// TODO Remove when polygon_coords is removed
public class ConvertFromSectorPolygonCoordsToSectorOutline {
	public static void main(String[] args) {
		try (DbConnection c = ConnectionPoolProvider.startTransaction()) {
			c.getConnection().setAutoCommit(true);
			try (PreparedStatement ps = c.getConnection().prepareStatement("SELECT s.id, s.polygon_coords FROM sector s WHERE s.polygon_coords IS NOT NULL AND s.id NOT IN (SELECT sector_id FROM sector_outline)");
					ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int idSector = rst.getInt("id");
					String polygonCoords = rst.getString("polygon_coords");
					List<Coordinate> outline = new ArrayList<>();
					for (String latLng : polygonCoords.split(";")) {
						String[] parts = latLng.split(",");
						double lat = Double.parseDouble(parts[0]);
						double lng = Double.parseDouble(parts[1]);
						if (outline.stream().filter(x -> x.getLatitude() == lat && x.getLongitude() == lng).findAny().isEmpty()) {
							outline.add(new Coordinate(lat, lng));
						}
					}
					c.getBuldreinfoRepo().ensureCoordinatesInDbWithElevation(outline);
					try (PreparedStatement ps2 = c.getConnection().prepareStatement("INSERT INTO sector_outline (sector_id, coordinate_id, sorting) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE coordinate_id=?, sorting=?")) {
						int sorting = 0;
						for (Coordinate coord : outline) {
							sorting++;
							ps2.setInt(1, idSector);
							ps2.setInt(2, coord.getId());
							ps2.setInt(3, sorting);
							ps2.setInt(4, coord.getId());
							ps2.setInt(5, sorting);
							ps2.addBatch();
						}
						ps2.executeBatch();
					}
				}
			}
			c.setSuccess();
		} catch (Exception e) {
			throw GlobalFunctions.getWebApplicationExceptionInternalError(e);
		}
	}
}
