package com.buldreinfo.jersey.jaxb.dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;
import com.buldreinfo.jersey.jaxb.model.ExternalLink;
import com.google.common.base.Stopwatch;

import jakarta.inject.Inject;

public class ExternalLinksRepository extends BaseRepository {
	private static final Logger logger = LogManager.getLogger();
	
	@Inject
	public ExternalLinksRepository(TransactionManager txManager) {
		super(txManager);
	}
	
	protected List<ExternalLink> getExternalLinks(int areaId, int sectorId, int problemId) throws SQLException {
		var stopwatch = Stopwatch.createStarted();
		var c = txManager.getConnection();
		var res = new ArrayList<ExternalLink>();
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
		try (var ps = c.prepareStatement(sql)) {
			ps.setInt(1, areaId);
			ps.setInt(2, sectorId);
			ps.setInt(3, problemId);
			try (var rst = ps.executeQuery()) {
				while (rst.next()) {
					res.add(new ExternalLink(rst.getInt("id"), rst.getString("url"), rst.getString("title"), rst.getBoolean("is_inherited")));
				}
			}
		}
		logger.debug("getExternalLinks(areaId={}, sectorId={}, problemId={}) - res.size()={}, duration={}", areaId, sectorId, problemId, res.size(), stopwatch);
		return res;
	}
	
	protected void upsertExternalLinks(List<ExternalLink> newLinks, int areaId, int sectorId, int problemId) throws SQLException {
		if (areaId <= 0 && sectorId <= 0 && problemId <= 0) {
			throw new UnsupportedOperationException("areaId=0, sectorId=0, problemId=0");
		}
		var c = txManager.getConnection();
		var previousLinks = getExternalLinks(areaId, sectorId, problemId).stream()
				.filter(x -> !x.inherited())
				.toList();
		var toRemove = previousLinks.stream()
				.filter(l -> newLinks == null || newLinks.stream().filter(x -> x.id() == l.id()).findAny().isEmpty())
				.toList();
		if (!toRemove.isEmpty()) {
			try (var ps = c.prepareStatement("DELETE FROM external_link WHERE id=?")) {
				for (var link : toRemove) {
					ps.setInt(1, link.id());
					ps.addBatch();
				}
				ps.executeBatch();
			}
		}
		if (newLinks != null) {
			var newLinksUpdate = newLinks.stream()
					.filter(l -> !l.inherited() && l.id() != 0)
					.toList();
			var newLinksCreate = newLinks.stream()
					.filter(l -> !l.inherited() && l.id() == 0)
					.toList();
			if (!newLinksUpdate.isEmpty()) {
				try (var ps = c.prepareStatement("UPDATE external_link SET url=?, title=? WHERE id=?")) {
					for (var l : newLinksUpdate) {
						ps.setString(1, l.url());
						ps.setString(2, l.title());
						ps.setInt(3, l.id());
						ps.addBatch();
					}
					ps.executeBatch();
				}
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
				try (var ps = c.prepareStatement("INSERT INTO external_link (url, title) VALUES (?, ?)", java.sql.Statement.RETURN_GENERATED_KEYS);
						var psJunction = c.prepareStatement(junctionSql)) {
					for (var l : newLinksCreate) {
						ps.setString(1, l.url());
						ps.setString(2, l.title());
						ps.addBatch();
					}
					ps.executeBatch();
					try (var rst = ps.getGeneratedKeys()) {
						while (rst != null && rst.next()) {
							var externalLinkId = rst.getInt(1);
							psJunction.setInt(1, externalLinkId);
							psJunction.setInt(2, targetId);
							psJunction.addBatch();
						}
					}
					psJunction.executeBatch();
				}
			}
		}
	}
}