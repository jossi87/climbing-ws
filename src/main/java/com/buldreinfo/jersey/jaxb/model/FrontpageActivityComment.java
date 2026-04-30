package com.buldreinfo.jersey.jaxb.model;
public record FrontpageActivityComment(String timeAgo,
			int areaId, String areaName, boolean areaLockedAdmin, boolean areaLockedSuperadmin,
			int sectorId, String sectorName, boolean sectorLockedAdmin, boolean sectorLockedSuperadmin,
			int problemId, boolean problemLockedAdmin, boolean problemLockedSuperadmin, String problemName,
			User u, String comment) {}