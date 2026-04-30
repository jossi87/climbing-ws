package com.buldreinfo.jersey.jaxb.model;
public record FrontpageActivityAscent(String timeAgo,
			int areaId, String areaName, boolean areaLockedAdmin, boolean areaLockedSuperadmin,
			int sectorId, String sectorName, boolean sectorLockedAdmin, boolean sectorLockedSuperadmin,
			int problemId, boolean problemLockedAdmin, boolean problemLockedSuperadmin, String problemName, String problemSubtype, String grade,
			User u, boolean repeat) {}