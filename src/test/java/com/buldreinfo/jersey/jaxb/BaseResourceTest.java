package com.buldreinfo.jersey.jaxb;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeAll;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.infrastructure.DatabaseContext;
import com.google.common.net.HttpHeaders;

import jakarta.servlet.http.HttpServletRequest;

public abstract class BaseResourceTest {
    protected enum Region { buldreinfo, brattelinjer }
    protected static final Logger logger = LogManager.getLogger();
    protected static final int USER_ID_SUPERADMIN = 1;
    protected static final int USER_ID_NORMAL = 1049;
    protected static final int BULDREINFO_AREA_ID_VISIBLE = 7;
    protected static final int BULDREINFO_SECTOR_ID_VISIBLE = 47;
    protected static final int BULDREINFO_PROBLEM_ID_VISIBLE = 1193;
    protected static final int BULDREINFO_HIDDEN_AREA_ID = 3267;
    protected static final int BULDREINFO_HIDDEN_SECTOR_ID = 2185;
    protected static final int BULDREINFO_HIDDEN_PROBLEM_ID = 2597;
    protected static final int BULDREINFO_MEDIA_ID_WITH_CHAPTERS = 19422;
    protected static final int BRATTELINJER_PROBLEM_ID_PDF = 7745;
    protected static final int BRATTELINJER_DIFFERENT_REGION_AREA_ID = 2887;
    protected static final int BRATTELINJER_DIFFERENT_REGION_SECTOR_ID = 3251;
    protected static final int BRATTELINJER_DIFFERENT_REGION_PROBLEM_ID = 7703;

    @BeforeAll
    public static void warmUp() {
    	DatabaseContext.runSql(_ -> {
            try (var ps = DatabaseContext.getConnection().prepareStatement("SELECT 1")) {
                ps.executeQuery();
            } catch (SQLException e) {
                throw new RuntimeException("Database warmup failed", e);
            }
        });
        logger.debug("Database pool and initial connection warmed up.");
    }

    protected HttpServletRequest getRequest(Region region) {
        String origin = switch (region) {
            case buldreinfo -> "https://buldreinfo.com";
            case brattelinjer -> "https://brattelinjer.no";
        };
        String serverName = switch (region) {
            case buldreinfo -> "buldreinfo.com";
            case brattelinjer -> "brattelinjer.no";
        };
        HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(req.getHeader(HttpHeaders.ORIGIN)).andReturn(origin).anyTimes();
        EasyMock.expect(req.getHeader(HttpHeaders.AUTHORIZATION)).andReturn(null).anyTimes();
        EasyMock.expect(req.getServerName()).andReturn(serverName).anyTimes();
        EasyMock.expect(req.getParameter("access_token")).andReturn(null).anyTimes();
        EasyMock.expect(req.getHeader(DatabaseContext.HEADER_INTERNAL_REQUEST)).andReturn(DatabaseContext.HEADER_INTERNAL_REQUEST_VALUE).anyTimes();
        EasyMock.replay(req);
        return req;
    }

    protected Setup getSetup(Region region) {
        String domain = switch (region) {
            case buldreinfo -> "buldreinfo.com";
            case brattelinjer -> "brattelinjer.no";
        };
        return DatabaseContext.getSetups().stream()
                .filter(s -> s.domain().equalsIgnoreCase(domain))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing setup for domain=" + domain));
    }
}