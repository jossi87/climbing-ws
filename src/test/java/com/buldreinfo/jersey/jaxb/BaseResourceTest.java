package com.buldreinfo.jersey.jaxb;

import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.easymock.EasyMock;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.dao.RegionRepository;
import com.buldreinfo.jersey.jaxb.filters.HitTrackingFilter;
import com.buldreinfo.jersey.jaxb.infrastructure.DependencyBinder;
import com.buldreinfo.jersey.jaxb.infrastructure.GlobalExceptionMapper;
import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;
import com.google.common.net.HttpHeaders;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;

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
	protected final ServiceLocator locator;

	protected BaseResourceTest() {
		this.locator = ServiceLocatorFactory.getInstance().create("test-locator");
		ServiceLocatorUtilities.bind(locator, new DependencyBinder());
	}
	
	@BeforeAll
	public void warmUp() throws Exception {
		TransactionManager txManager = getService(TransactionManager.class);
		RegionRepository regionRepo = getService(RegionRepository.class);
		txManager.executeInTransaction(() -> regionRepo.getSetups());
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
		EasyMock.expect(req.getAttribute(HitTrackingFilter.SHOULD_UPDATE_HITS_KEY)).andReturn(false).anyTimes();
		EasyMock.replay(req);
		return req;
	}

	protected <T> T getService(Class<T> serviceClass) {
		return locator.getService(serviceClass);
	}

	protected Setup getSetup(Region region) throws IllegalStateException, Exception {
		TransactionManager txManager = getService(TransactionManager.class);
		RegionRepository regionRepo = getService(RegionRepository.class);
		String domain = switch (region) {
		case buldreinfo -> "buldreinfo.com";
		case brattelinjer -> "brattelinjer.no";
		};
		return txManager.executeInTransaction(() -> regionRepo.getSetups()).stream()
				.filter(s -> s.domain().equalsIgnoreCase(domain))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Missing setup for domain=" + domain));
	}

	protected Response invoke(Callable<Response> resourceCall) throws Exception {
	    try {
	        return resourceCall.call();
	    } catch (Throwable e) {
	        var mapper = new GlobalExceptionMapper();
	        return mapper.toResponse(e);
	    }
	}
}