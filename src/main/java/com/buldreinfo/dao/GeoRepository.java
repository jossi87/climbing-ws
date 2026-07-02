package com.buldreinfo.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.model.Coordinates;

@Repository
public class GeoRepository {
	private final JdbcClient jdbcClient;
	private final JdbcTemplate jdbcTemplate;

	public GeoRepository(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
		this.jdbcClient = jdbcClient;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional
	public List<Coordinates> resolveCoordinates(List<Coordinates> coordinates) {
		if (coordinates == null || coordinates.isEmpty()) return List.of();

		var rounded = coordinates.stream()
				.map(Coordinates::roundTo10Digits)
				.toList();

		jdbcTemplate.batchUpdate(
				"INSERT IGNORE INTO coordinates (latitude, longitude, elevation, elevation_source) VALUES (?, ?, ?, ?)",
				rounded,
				100,
				(ps, coord) -> {
					ps.setDouble(1, coord.latitude());
					ps.setDouble(2, coord.longitude());
					if (coord.elevationSource() != null) {
						ps.setDouble(3, coord.elevation());
						ps.setString(4, coord.elevationSource());
					} else {
						ps.setObject(3, null);
						ps.setObject(4, null);
					}
				});

		String placeholders = String.join(",", Collections.nCopies(rounded.size(), "(?,?)"));
		var sql = "SELECT id, latitude, longitude, elevation, elevation_source FROM coordinates WHERE (latitude, longitude) IN (" + placeholders + ")";

		var params = rounded.stream()
				.flatMap(c -> Stream.of(c.latitude(), c.longitude()))
				.toList();

		Map<String, Coordinates> dbResults = jdbcClient.sql(sql)
				.params(params)
				.query((rs, _) -> new Coordinates(rs.getInt("id"), rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getDouble("elevation"), rs.getString("elevation_source"), 0.0))
				.list()
				.stream()
				.collect(Collectors.toMap(
						c -> c.latitude() + "," + c.longitude(), 
						c -> c
						));

		List<Coordinates> result = new ArrayList<>(coordinates.size());
		for (var c : rounded) {
			var key = c.latitude() + "," + c.longitude();
			var dbCoord = dbResults.get(key);
			if (dbCoord == null) {
				throw new IllegalStateException("Coordinate (" + c.latitude() + ", " + c.longitude() + ") was not persisted after INSERT IGNORE");
			}
			result.add(dbCoord);
		}
		return result;
	}

	@Transactional(readOnly = true)
	public List<Coordinates> getCoordinatesMissingElevation() {
		return jdbcClient.sql("SELECT id, latitude, longitude, elevation, elevation_source FROM coordinates WHERE elevation IS NULL OR elevation_source IS NULL")
				.query((rs, _) -> new Coordinates(
						rs.getInt("id"), 
						rs.getDouble("latitude"), 
						rs.getDouble("longitude"), 
						rs.getDouble("elevation"), 
						rs.getString("elevation_source"),
						0.0
						)).list();
	}

	@Transactional
	public void updateCoordinatesBatch(List<Coordinates> coordinates) {
		jdbcTemplate.batchUpdate(
				"UPDATE coordinates SET elevation=?, elevation_source=? WHERE id=?",
				coordinates,
				100,
				(ps, coord) -> {
					ps.setDouble(1, coord.elevation());
					ps.setString(2, coord.elevationSource());
					ps.setInt(3, coord.id());
				}
				);
	}
}
