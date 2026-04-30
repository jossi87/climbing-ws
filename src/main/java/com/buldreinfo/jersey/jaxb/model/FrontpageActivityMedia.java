package com.buldreinfo.jersey.jaxb.model;
public record FrontpageActivityMedia(MediaIdentity identity,
			int areaId, String areaName, boolean areaLockedAdmin, boolean areaLockedSuperadmin,
			int sectorId, String sectorName, boolean sectorLockedAdmin, boolean sectorLockedSuperadmin,
			int problemId, boolean problemLockedAdmin, boolean problemLockedSuperadmin, String problemName, String grade) {}