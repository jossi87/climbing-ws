package com.buldreinfo.jersey.jaxb.model;

public record PublicAscent(String areaName, boolean areaLockedAdmin, boolean areaLockedSuperadmin, String sectorName,
			boolean sectorLockedAdmin, boolean sectorLockedSuperadmin, int problemId, String problemGrade,
			String problemName, boolean problemLockedAdmin, boolean problemLockedSuperadmin, String date, String name) {}