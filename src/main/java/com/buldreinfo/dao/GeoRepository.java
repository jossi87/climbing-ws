package com.buldreinfo.dao;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.model.Coordinates;
import com.buldreinfo.service.ElevationService;

@Repository
public class GeoRepository {
	private final JdbcClient jdbcClient;
	private final JdbcTemplate jdbcTemplate;
	private final ElevationService elevationService;

	public GeoRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate, ElevationService elevationService) {
		this.jdbcClient = jdbcClient;
		this.jdbcTemplate = jdbcTemplate;
		this.elevationService = elevationService;
	}

	private void fillMissingElevations() {
		var coordinatesMissingElevation = jdbcClient.sql("SELECT id, latitude, longitude, elevation, elevation_source FROM coordinates WHERE elevation IS NULL OR elevation_source IS NULL")
				.query((rs, _) -> new Coordinates(
						rs.getInt("id"), 
						rs.getDouble("latitude"), 
						rs.getDouble("longitude"), 
						rs.getDouble("elevation"), 
						rs.getString("elevation_source")
						)).list();

		if (!coordinatesMissingElevation.isEmpty()) {
			elevationService.fillElevations(coordinatesMissingElevation);
			jdbcTemplate.batchUpdate(
					"UPDATE coordinates SET elevation=?, elevation_source=? WHERE id=?",
					coordinatesMissingElevation,
					100,
					(ps, coord) -> {
						ps.setDouble(1, coord.getElevation());
						ps.setString(2, coord.getElevationSource());
						ps.setInt(3, coord.getId());
					}
					);
		}
	}

	@Transactional
	public void ensureCoordinatesInDbWithElevationAndId(List<Coordinates> coordinates) {
		if (coordinates == null || coordinates.isEmpty()) return;

		for (var coord : coordinates) {
			coord.roundCoordinatesToMaximum10digitsAfterComma();
		}

		jdbcTemplate.batchUpdate(
				"INSERT IGNORE INTO coordinates (latitude, longitude, elevation, elevation_source) VALUES (?, ?, ?, ?)",
				coordinates,
				100,
				(ps, coord) -> {
					ps.setDouble(1, coord.getLatitude());
					ps.setDouble(2, coord.getLongitude());
					if (coord.getElevationSource() != null) {
						ps.setDouble(3, coord.getElevation());
						ps.setString(4, coord.getElevationSource());
					} else {
						ps.setObject(3, null);
						ps.setObject(4, null);
					}
				});

		fillMissingElevations();

		String placeholders = String.join(",", Collections.nCopies(coordinates.size(), "(?,?)"));
		var sql = "SELECT id, latitude, longitude, elevation, elevation_source FROM coordinates WHERE (latitude, longitude) IN (" + placeholders + ")";

		var params = coordinates.stream()
				.flatMap(c -> Stream.of(c.getLatitude(), c.getLongitude()))
				.toList();

		jdbcClient.sql(sql)
		.params(params)
		.query(rs -> {
			double lat = rs.getDouble("latitude");
			double lon = rs.getDouble("longitude");
			int id = rs.getInt("id");
			double elev = rs.getDouble("elevation");
			String src = rs.getString("elevation_source");

			for (Coordinates c : coordinates) {
				if (Math.abs(c.getLatitude() - lat) < 1e-10 && Math.abs(c.getLongitude() - lon) < 1e-10) {
					c.setId(id);
					c.setElevation(elev, src);
				}
			}
		});
	}
}