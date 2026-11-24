package com.buldreinfo.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.model.Coordinates;

@Repository
public class GeoRepository {
	private final JdbcClient jdbcClient;

	public GeoRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Transactional
	public void insertCoordinates(List<Coordinates> coordinates) {
		if (coordinates == null || coordinates.isEmpty()) return;

		var batch = jdbcClient.sql("INSERT IGNORE INTO coordinates (latitude, longitude, elevation, elevation_source) VALUES (?, ?, ?, ?)")
				.batch();
		coordinates.forEach(coord -> batch.entry(
				e -> e.param(coord.latitude()).param(coord.longitude()).param(coord.elevation()).param(coord.elevationSource())));
		batch.update();
	}

	@Transactional(readOnly = true)
	public List<Coordinates> getCoordinatesByLatLng(List<Coordinates> coordinates) {
		if (coordinates == null || coordinates.isEmpty()) return List.of();

		Map<String, Object> params = new HashMap<>();
		String inSql = IntStream.range(0, coordinates.size())
				.mapToObj(i -> {
					params.put("lat" + i, coordinates.get(i).latitude());
					params.put("lon" + i, coordinates.get(i).longitude());
					return "(:lat" + i + ", :lon" + i + ")";
				})
				.collect(Collectors.joining(", "));

		String sql = """
				SELECT id, latitude, longitude, elevation, elevation_source
				FROM coordinates
				WHERE (latitude, longitude) IN (%s)
				""".formatted(inSql);

		var dbResults = jdbcClient.sql(sql)
				.params(params)
				.query((rs, _) -> new Coordinates(
						rs.getInt("id"), 
						rs.getDouble("latitude"), 
						rs.getDouble("longitude"), 
						rs.getDouble("elevation"), 
						rs.getString("elevation_source"), 
						0.0))
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
						throw new IllegalStateException("Coordinate (" + c.latitude() + ", " + c.longitude() + ") was not persisted");
					}
					return dbCoord;
				})
				.toList();
	}
}
