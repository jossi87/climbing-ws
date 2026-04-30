package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record FrontpageFirstAscent(String timeAgo,
			int areaId, String areaName, boolean areaLockedAdmin, boolean areaLockedSuperadmin,
			int sectorId, String sectorName, boolean sectorLockedAdmin, boolean sectorLockedSuperadmin,
			int problemId, boolean problemLockedAdmin, boolean problemLockedSuperadmin, String problemName, String problemSubtype, String grade,
			List<User> users) {}