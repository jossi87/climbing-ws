package com.buldreinfo.dao;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.model.CompassDirection;
import com.buldreinfo.model.Coordinates;
import com.buldreinfo.service.ElevationService;

@Repository
public class GeoRepository {
	private final JdbcClient jdbcClient;
	private final ElevationService elevationService;

	public GeoRepository(JdbcClient jdbcClient, ElevationService elevationService) {
		this.jdbcClient = jdbcClient;
		this.elevationService = elevationService;
	}

	private void fillMissingElevations() {
		var coordinatesMissingElevation = jdbcClient.sql("SELECT id, latitude, longitude, elevation, elevation_source FROM coordinates WHERE elevation IS NULL")
				.query((rs, _) -> new Coordinates(
						rs.getInt("id"), 
						rs.getDouble("latitude"), 
						rs.getDouble("longitude"), 
						rs.getDouble("elevation"), 
						rs.getString("elevation_source")
						)).list();

		if (!coordinatesMissingElevation.isEmpty()) {
			elevationService.fillElevations(coordinatesMissingElevation);
			for (var coord : coordinatesMissingElevation) {
				jdbcClient.sql("UPDATE coordinates SET elevation=?, elevation_source=? WHERE id=?")
				.param(1, coord.getElevation())
				.param(2, coord.getElevationSource())
				.param(3, coord.getId())
				.update();
			}
		}
	}

	@Transactional
	protected void ensureCoordinatesInDbWithElevationAndId(List<Coordinates> coordinates) {
		if (coordinates != null && !coordinates.isEmpty()) {
			for (var coord : coordinates) {
				coord.roundCoordinatesToMaximum10digitsAfterComma();
				jdbcClient.sql("INSERT IGNORE INTO coordinates (latitude, longitude, elevation, elevation_source) VALUES (?, ?, ?, ?)")
				.param(1, coord.getLatitude())
				.param(2, coord.getLongitude())
				.param(3, coord.getElevationSource() != null ? coord.getElevation() : null)
				.param(4, coord.getElevationSource() != null ? coord.getElevationSource() : null)
				.update();
			}

			fillMissingElevations();

			for (var coord : coordinates) {
				jdbcClient.sql("SELECT id, elevation, elevation_source FROM coordinates WHERE latitude=? AND longitude=?")
				.param(1, coord.getLatitude())
				.param(2, coord.getLongitude())
				.query(rs -> {
					if (rs.next()) {
						coord.setId(rs.getInt("id"));
						coord.setElevation(rs.getDouble("elevation"), rs.getString("elevation_source"));
					}
					return Void.TYPE;
				});
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
				.orElse(null);
	}
}