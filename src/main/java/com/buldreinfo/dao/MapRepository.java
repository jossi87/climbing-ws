package com.buldreinfo.dao;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.model.Map;

@Repository
public class MapRepository {
	private final JdbcClient jdbcClient;

	public MapRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Transactional(readOnly = true)
	public List<Map> getMaps(int regionId) {
		return jdbcClient.sql("""
				SELECT m.id, m.name, m.url, m.attribution, m.max_zoom, rm.is_default
				FROM map m
				JOIN region_map rm ON m.id=rm.map_id
				WHERE rm.region_id=?
				ORDER BY m.id
				""")
				.param(regionId)
				.query((rs, _) -> new Map(
						rs.getInt("id"),
						rs.getString("name"),
						rs.getString("url"),
						rs.getString("attribution"),
						rs.getInt("max_zoom"),
						rs.getBoolean("is_default")))
				.list();
	}
}
