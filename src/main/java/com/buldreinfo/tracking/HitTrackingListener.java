package com.buldreinfo.tracking;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class HitTrackingListener {

	private static final Logger logger = LogManager.getLogger();

	public record AreaHitEvent(int areaId) {}
	public record SectorHitEvent(int sectorId) {}
	public record ProblemHitEvent(int problemId) {}

	private final JdbcClient jdbcClient;

	public HitTrackingListener(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Async("hitTrackingExecutor")
	@EventListener
	@Transactional
	public void handleAreaHit(AreaHitEvent event) {
		try {
			jdbcClient.sql("UPDATE area SET hits=hits+1 WHERE id=?")
			.param(1, event.areaId())
			.update();
			logger.debug("Incremented hits for Area ID={}", event.areaId());
		} catch (Exception e) {
			logger.error("Failed to increment hits for Area ID={}", event.areaId(), e);
		}
	}

	@Async("hitTrackingExecutor")
	@EventListener
	@Transactional
	public void handleSectorHit(SectorHitEvent event) {
		try {
			jdbcClient.sql("UPDATE sector SET hits=hits+1 WHERE id=?")
			.param(1, event.sectorId())
			.update();
			logger.debug("Incremented hits for Sector ID={}", event.sectorId());
		} catch (Exception e) {
			logger.error("Failed to increment hits for Sector ID={}", event.sectorId(), e);
		}
	}

	@Async("hitTrackingExecutor")
	@EventListener
	@Transactional
	public void handleProblemHit(ProblemHitEvent event) {
		try {
			jdbcClient.sql("UPDATE problem SET hits=hits+1 WHERE id=?")
			.param(1, event.problemId())
			.update();
			logger.debug("Incremented hits for Problem ID={}", event.problemId());
		} catch (Exception e) {
			logger.error("Failed to increment hits for Problem ID={}", event.problemId(), e);
		}
	}
}