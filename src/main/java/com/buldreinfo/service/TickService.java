package com.buldreinfo.service;

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
    public void setTick(Setup setup, Optional<Integer> authUserId, Tick t) {
        tickRepo.setTick(setup, authUserId, t);
        activityRepo.fillActivity(t.idProblem());
        problemRepo.updateProblemConsensusGrade(t.idProblem());
    }
}