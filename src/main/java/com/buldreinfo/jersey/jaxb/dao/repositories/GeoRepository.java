package com.buldreinfo.jersey.jaxb.dao.repositories;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.helpers.GeoHelper;
import com.buldreinfo.jersey.jaxb.model.CompassDirection;
import com.buldreinfo.jersey.jaxb.model.Coordinates;

public record GeoRepository() {
	private static Logger logger = LogManager.getLogger();
	
	public void ensureCoordinatesInDbWithElevationAndId(Connection c, List<Coordinates> coordinates) throws SQLException, InterruptedException {
		if (coordinates != null && !coordinates.isEmpty()) {
			// First round coordinates to 10 digits (to match database type)
			coordinates.forEach(coord -> coord.roundCoordinatesToMaximum10digitsAfterComma());
			// Ensure coordinates exists in db
			try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO coordinates (latitude, longitude, elevation, elevation_source) VALUES (?, ?, ?, ?)")) {
				for (Coordinates coord : coordinates) {
					ps.setDouble(1, coord.getLatitude());
					ps.setDouble(2, coord.getLongitude());
					// Use elevation from GPX/TCX if available, this has better quality compared to Google Elevation API
					if (coord.getElevationSource() != null) {
						ps.setDouble(3, coord.getElevation());
						ps.setString(4, coord.getElevationSource());
					}
					else {
						ps.setNull(3, Types.DOUBLE);
						ps.setNull(4, Types.VARCHAR);
					}
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
		fillMissingElevations(c);
		if (coordinates != null && !coordinates.isEmpty()) {
			// Fetch correct id's and elevation's (id can be wrong in coordinates - user might have changed latitude/longitude on existing id)
			for (Coordinates coord : coordinates) {
				try (PreparedStatement ps = c.prepareStatement("SELECT id, elevation, elevation_source FROM coordinates WHERE latitude=? AND longitude=?")) {
					ps.setDouble(1, coord.getLatitude());
					ps.setDouble(2, coord.getLongitude());
					try (ResultSet rst = ps.executeQuery()) {
						while (rst.next()) {
							int id = rst.getInt("id");
							double elevation = rst.getDouble("elevation");
							String elevationSource = rst.getString("elevation_source");
							coord.setId(id);
							coord.setElevation(elevation, elevationSource);
						}
					}
				}
			}
		}
	}

	private void fillMissingElevations(Connection c) throws SQLException, InterruptedException {
		List<Coordinates> coordinatesMissingElevation = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT id, latitude, longitude, elevation, elevation_source FROM coordinates WHERE elevation IS NULL")) {
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					double latitude = rst.getDouble("latitude");
					double longitude = rst.getDouble("longitude");
					double elevation = rst.getDouble("elevation");
					String elevationSource = rst.getString("elevation_source");
					coordinatesMissingElevation.add(new Coordinates(id, latitude, longitude, elevation, elevationSource));
				}
			}
		}
		if (!coordinatesMissingElevation.isEmpty()) {
			try {
				GeoHelper.fillMissingElevations(coordinatesMissingElevation);
				try (PreparedStatement ps = c.prepareStatement("UPDATE coordinates SET elevation=?, elevation_source=? WHERE id=?")) {
					for (Coordinates coord : coordinatesMissingElevation) {
						ps.setDouble(1, coord.getElevation());
						ps.setString(2, coord.getElevationSource());
						ps.setDouble(3, coord.getId());
						ps.addBatch();
					}
					ps.executeBatch();
				}
			} catch (IOException e) {
				logger.warn(e.getMessage(), e);
			}
		}
	}

	protected CompassDirection getCompassDirection(Setup s, int id) {
		if (id == 0) {
			return null;
		}
		return s.compassDirections()
				.stream()
				.filter(cd -> cd.id() == id)
				.findAny()
				.get();
	}
	
	protected List<CompassDirection> getCompassDirections(Connection c) throws SQLException {
		List<CompassDirection> res = new ArrayList<>();
		try (PreparedStatement ps = c.prepareStatement("SELECT id, direction FROM compass_direction ORDER BY id")) {
			try (ResultSet rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					String direction = rst.getString("direction");
					res.add(new CompassDirection(id, direction));
				}
			}
		}
		return res;
	}
}