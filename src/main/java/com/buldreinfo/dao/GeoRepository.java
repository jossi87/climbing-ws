package com.buldreinfo.dao;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.helpers.GeoHelper;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;
import com.buldreinfo.model.CompassDirection;
import com.buldreinfo.model.Coordinates;

@Repository
public class GeoRepository extends BaseRepository {
	private static final Logger logger = LogManager.getLogger();
	
	public GeoRepository(ClimbingTransactionManager txManager) {
		super(txManager);
	}
	
	private void fillMissingElevations() throws SQLException, InterruptedException {
		var c = txManager.getConnection();
		var coordinatesMissingElevation = new ArrayList<Coordinates>();
		try (var ps = c.prepareStatement("SELECT id, latitude, longitude, elevation, elevation_source FROM coordinates WHERE elevation IS NULL")) {
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					int id = rst.getInt("id");
					double latitude = rst.getDouble("latitude");
					double longitude = rst.getDouble("longitude");
					double elevation = rst.getDouble("elevation");
					var elevationSource = rst.getString("elevation_source");
					coordinatesMissingElevation.add(new Coordinates(id, latitude, longitude, elevation, elevationSource));
				}
			}
		}
		if (!coordinatesMissingElevation.isEmpty()) {
			try {
				GeoHelper.fillMissingElevations(coordinatesMissingElevation);
				try (var ps = c.prepareStatement("UPDATE coordinates SET elevation=?, elevation_source=? WHERE id=?")) {
					for (var coord : coordinatesMissingElevation) {
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

	protected void ensureCoordinatesInDbWithElevationAndId(List<Coordinates> coordinates) throws SQLException, InterruptedException {
		var c = txManager.getConnection();
		if (coordinates != null && !coordinates.isEmpty()) {
			coordinates.forEach(coord -> coord.roundCoordinatesToMaximum10digitsAfterComma());
			try (var ps = c.prepareStatement("INSERT IGNORE INTO coordinates (latitude, longitude, elevation, elevation_source) VALUES (?, ?, ?, ?)")) {
				for (var coord : coordinates) {
					ps.setDouble(1, coord.getLatitude());
					ps.setDouble(2, coord.getLongitude());
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
		fillMissingElevations();
		if (coordinates != null && !coordinates.isEmpty()) {
			for (var coord : coordinates) {
				try (var ps = c.prepareStatement("SELECT id, elevation, elevation_source FROM coordinates WHERE latitude=? AND longitude=?")) {
					ps.setDouble(1, coord.getLatitude());
					ps.setDouble(2, coord.getLongitude());
					try (var rst = ps.executeQuery()) {
						while (rst.next()) {
							int id = rst.getInt("id");
							double elevation = rst.getDouble("elevation");
							var elevationSource = rst.getString("elevation_source");
							coord.setId(id);
							coord.setElevation(elevation, elevationSource);
						}
					}
				}
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
}