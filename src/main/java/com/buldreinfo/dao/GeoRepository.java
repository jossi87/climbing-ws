package com.buldreinfo.dao;

import java.util.Collections;
import java.util.List;
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
	public void insertCoordinates(List<Coordinates> coordinates) {
		if (coordinates == null || coordinates.isEmpty()) return;

		jdbcTemplate.batchUpdate(
				"INSERT IGNORE INTO coordinates (latitude, longitude, elevation, elevation_source) VALUES (?, ?, ?, ?)",
				coordinates,
				100,
				(ps, coord) -> {
					ps.setDouble(1, coord.latitude());
					ps.setDouble(2, coord.longitude());
					ps.setDouble(3, coord.elevation());
					ps.setString(4, coord.elevationSource());
				});
	}

	@Transactional(readOnly = true)
	public List<Coordinates> getCoordinatesByLatLng(List<Coordinates> coordinates) {
		if (coordinates == null || coordinates.isEmpty()) return List.of();
		String placeholders = String.join(",", Collections.nCopies(coordinates.size(), "(?,?)"));
		var sql = "SELECT id, latitude, longitude, elevation, elevation_source FROM coordinates WHERE (latitude, longitude) IN (" + placeholders + ")";
		var params = coordinates.stream()
				.flatMap(c -> Stream.of(c.latitude(), c.longitude()))
				.toList();
		var dbResults = jdbcClient.sql(sql)
				.params(params)
				.query((rs, _) -> new Coordinates(rs.getInt("id"), rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getDouble("elevation"), rs.getString("elevation_source"), 0.0))
				.list();
		var lookup = dbResults.stream()
				.collect(Collectors.toMap(
						c -> c.latitude() + "," + c.longitude(),
						c -> c));
		return coordinates.stream()
				.map(c -> {
					var key = c.latitude() + "," + c.longitude();
					var dbCoord = lookup.get(key);
					if (dbCoord == null) {
						throw new IllegalStateException("Coordinate (" + c.latitude() + ", " + c.longitude() + ") was not persisted after INSERT IGNORE");
					}
					return dbCoord;
				})
				.toList();
	}
}
