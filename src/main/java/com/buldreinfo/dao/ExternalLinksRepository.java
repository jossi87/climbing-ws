package com.buldreinfo.dao;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.buldreinfo.model.ExternalLink;

@Repository
public class ExternalLinksRepository {
	private static final Logger logger = LogManager.getLogger();
	private final JdbcClient jdbcClient;

	public ExternalLinksRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	protected List<ExternalLink> getExternalLinks(int areaId, int sectorId, int problemId) {
		var start = System.nanoTime();
		var sql = """
				WITH req AS (
				    SELECT ? AS req_area_id, ? AS req_sector_id, ? AS req_problem_id
				),
				resolved_hierarchy AS (
				    SELECT 
				        p.id AS problem_id,
				        s.id AS sector_id,
				        a.id AS area_id
				    FROM req
				    LEFT JOIN problem p ON p.id = req.req_problem_id
				    LEFT JOIN sector s ON s.id = CASE WHEN req.req_sector_id > 0 THEN req.req_sector_id ELSE p.sector_id END
				    LEFT JOIN area a ON a.id = CASE WHEN req.req_area_id > 0 THEN req.req_area_id ELSE s.area_id END
				    LIMIT 1
				),
				unified_links AS (
				    SELECT e.id, e.url, e.title, 'area' AS source_type
				    FROM resolved_hierarchy h
				    JOIN external_link_area ea ON h.area_id = ea.area_id
				    JOIN external_link e ON ea.external_link_id = e.id
				    UNION ALL
				    SELECT e.id, e.url, e.title, 'sector'
				    FROM resolved_hierarchy h
				    JOIN external_link_sector es ON h.sector_id = es.sector_id
				    JOIN external_link e ON es.external_link_id = e.id
				    UNION ALL
				    SELECT e.id, e.url, e.title, 'problem'
				    FROM resolved_hierarchy h
				    JOIN external_link_problem ep ON h.problem_id = ep.problem_id
				    JOIN external_link e ON ep.external_link_id = e.id
				)
				SELECT 
				    u.id, u.url, u.title,
				    CASE 
				        WHEN r.req_problem_id > 0 AND u.source_type = 'problem' THEN 0
				        WHEN r.req_problem_id = 0 AND r.req_sector_id > 0 AND u.source_type = 'sector' THEN 0
				        WHEN r.req_problem_id = 0 AND r.req_sector_id = 0 AND r.req_area_id > 0 AND u.source_type = 'area' THEN 0
				        ELSE 1
				    END AS is_inherited
				FROM unified_links u
				CROSS JOIN req r
				ORDER BY u.title
				""";

		var res = jdbcClient.sql(sql)
				.param(1, areaId)
				.param(2, sectorId)
				.param(3, problemId)
				.query((rs, _) -> new ExternalLink(
						rs.getInt("id"),
						rs.getString("url"),
						rs.getString("title"),
						rs.getBoolean("is_inherited")
						)).list();

		logger.debug("getExternalLinks(areaId={}, sectorId={}, problemId={}) - res.size()={}, duration={}ms", areaId, sectorId, problemId, res.size(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
		return res;
	}

	protected void upsertExternalLinks(List<ExternalLink> newLinks, int areaId, int sectorId, int problemId) {
		if (areaId <= 0 && sectorId <= 0 && problemId <= 0) {
			throw new UnsupportedOperationException("areaId=0, sectorId=0, problemId=0");
		}

		var previousLinks = getExternalLinks(areaId, sectorId, problemId).stream()
				.filter(x -> !x.inherited())
				.toList();

		var toRemove = previousLinks.stream()
				.filter(l -> newLinks == null || newLinks.stream().filter(x -> x.id() == l.id()).findAny().isEmpty())
				.toList();

		if (!toRemove.isEmpty()) {
			var idsToRemove = toRemove.stream().map(ExternalLink::id).toList();
			jdbcClient.sql("DELETE FROM external_link WHERE id IN (:ids)")
			.param("ids", idsToRemove)
			.update();
		}

		if (newLinks != null) {
			var newLinksUpdate = newLinks.stream()
					.filter(l -> !l.inherited() && l.id() != 0)
					.toList();

			var newLinksCreate = newLinks.stream()
					.filter(l -> !l.inherited() && l.id() == 0)
					.toList();

			for (var l : newLinksUpdate) {
				jdbcClient.sql("UPDATE external_link SET url=?, title=? WHERE id=?")
				.param(1, l.url())
				.param(2, l.title())
				.param(3, l.id())
				.update();
			}

			if (!newLinksCreate.isEmpty()) {
				String junctionSql;
				int targetId;

				if (areaId > 0) {
					junctionSql = "INSERT INTO external_link_area (external_link_id, area_id) VALUES (?, ?)";
					targetId = areaId;
				} else if (sectorId > 0) {
					junctionSql = "INSERT INTO external_link_sector (external_link_id, sector_id) VALUES (?, ?)";
					targetId = sectorId;
				} else {
					junctionSql = "INSERT INTO external_link_problem (external_link_id, problem_id) VALUES (?, ?)";
					targetId = problemId;
				}

				for (var l : newLinksCreate) {
					var keyHolder = new GeneratedKeyHolder();
					jdbcClient.sql("INSERT INTO external_link (url, title) VALUES (?, ?)")
					.param(1, l.url())
					.param(2, l.title())
					.update(keyHolder, "id");

					if (keyHolder.getKey() != null) {
						jdbcClient.sql(junctionSql)
						.param(1, keyHolder.getKey().intValue())
						.param(2, targetId)
						.update();
					}
				}
			}
		}
	}
}