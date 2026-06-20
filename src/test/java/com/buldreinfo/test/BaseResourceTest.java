package com.buldreinfo.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;

import com.buldreinfo.beans.Setup;
import com.buldreinfo.dao.RegionRepository;
import com.buldreinfo.infrastructure.ClimbingTransactionManager;

import jakarta.servlet.http.HttpServletRequest;

@SpringBootTest
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseResourceTest {
    protected enum Region { brattelinjer, buldreinfo }
    protected static final int BRATTELINJER_DIFFERENT_REGION_AREA_ID = 2887;
    protected static final int BRATTELINJER_DIFFERENT_REGION_PROBLEM_ID = 7703;
    protected static final int BRATTELINJER_DIFFERENT_REGION_SECTOR_ID = 3251;
    protected static final int BRATTELINJER_PROBLEM_ID_PDF = 7745;
    protected static final int BULDREINFO_AREA_ID_VISIBLE = 7;
    protected static final int BULDREINFO_HIDDEN_AREA_ID = 3267;
    protected static final int BULDREINFO_HIDDEN_PROBLEM_ID = 2597;
    protected static final int BULDREINFO_HIDDEN_SECTOR_ID = 2185;
    protected static final int BULDREINFO_MEDIA_ID_WITH_CHAPTERS = 19422;
    protected static final int BULDREINFO_MEDIA_ID_WITH_CHAPTERS_PROBLEM_ID = 1665;
    protected static final int BULDREINFO_PROBLEM_ID_VISIBLE = 1193;
    protected static final int BULDREINFO_SECTOR_ID_VISIBLE = 47;
    protected static final Logger logger = LogManager.getLogger();
    protected static final int USER_ID_NORMAL = 1049;
    protected static final int USER_ID_SUPERADMIN = 1;
    @Autowired protected ClimbingTransactionManager txManager;
    @Autowired protected RegionRepository regionRepo;

    @BeforeAll
    public void warmUp() throws Exception {
        txManager.executeInTransaction(() -> regionRepo.getSetups());
        logger.debug("Database pool and initial connection warmed up.");
    }

    protected HttpServletRequest getRequest(Region region) {
        String origin = (region == Region.buldreinfo) ? "https://buldreinfo.com" : "https://brattelinjer.no";
        String serverName = (region == Region.buldreinfo) ? "buldreinfo.com" : "brattelinjer.no";

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader(HttpHeaders.ORIGIN)).thenReturn(origin);
        when(req.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(req.getServerName()).thenReturn(serverName);
        when(req.getParameter("access_token")).thenReturn(null);
        when(req.getAttribute("should_update_hits")).thenReturn(false);
        return req;
    }

    protected Setup getSetup(Region region) throws Exception {
        String domain = (region == Region.buldreinfo) ? "buldreinfo.com" : "brattelinjer.no";
        return txManager.executeInTransaction(() -> regionRepo.getSetups()).stream()
                .filter(s -> s.domain().equalsIgnoreCase(domain))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing setup for domain=" + domain));
    }
}