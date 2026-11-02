package com.buldreinfo.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.ActivityRepository;
import com.buldreinfo.dao.ProblemRepository;
import com.buldreinfo.dao.TickRepository;
import com.buldreinfo.model.Tick;
import com.buldreinfo.model.Ticks;

@Service
public class TickService {
    private final TickRepository tickRepo;
    private final ProblemRepository problemRepo;
    private final ActivityRepository activityRepo;

    public TickService(TickRepository tickRepo, ProblemRepository problemRepo, ActivityRepository activityRepo) {
        this.tickRepo = tickRepo;
        this.problemRepo = problemRepo;
        this.activityRepo = activityRepo;
    }
    
    @Transactional(readOnly = true)
    public Ticks getTicks(Optional<Integer> authUserId, Setup setup, int page) {
        return tickRepo.getTicks(authUserId, setup, page);
    }

    @Transactional
    public void setTick(Optional<Integer> authUserId, Tick t) {
        tickRepo.setTick(authUserId, t);
        activityRepo.fillActivity(List.of(t.idProblem()));
        problemRepo.updateProblemConsensusGrade(t.idProblem());
    }

    @Transactional
    public void setTicks(int userId, List<Tick> ticks) {
        if (ticks == null || ticks.isEmpty()) {
            return;
        }
        var problemIds = new LinkedHashSet<Integer>();
        for (var t : ticks) {
            tickRepo.setTick(Optional.of(userId), t);
            problemIds.add(t.idProblem());
        }
        activityRepo.fillActivity(List.copyOf(problemIds));
        for (var idProblem : problemIds) {
            problemRepo.updateProblemConsensusGrade(idProblem);
        }
    }
}