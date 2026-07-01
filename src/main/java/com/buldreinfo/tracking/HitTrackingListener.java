package com.buldreinfo.tracking;

import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class HitTrackingListener {

    public record AreaHitEvent(int areaId) {}
    public record SectorHitEvent(int sectorId) {}
    public record ProblemHitEvent(int problemId) {}

    private final JdbcClient jdbcClient;

    public HitTrackingListener(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Async
    @EventListener
    public void handleAreaHit(AreaHitEvent event) {
        jdbcClient.sql("UPDATE area SET hits=hits+1 WHERE id=?")
                  .param(1, event.areaId())
                  .update();
    }

    @Async
    @EventListener
    public void handleSectorHit(SectorHitEvent event) {
        jdbcClient.sql("UPDATE sector SET hits=hits+1 WHERE id=?")
                  .param(1, event.sectorId())
                  .update();
    }

    @Async
    @EventListener
    public void handleProblemHit(ProblemHitEvent event) {
        jdbcClient.sql("UPDATE problem SET hits=hits+1 WHERE id=?")
                  .param(1, event.problemId())
                  .update();
    }
}