package com.buldreinfo.jersey.jaxb.model;

public record PublicAscent(int areaId, String areaName, boolean areaLockedAdmin, boolean areaLockedSuperadmin,
		int sectorId, String sectorName, boolean sectorLockedAdmin, boolean sectorLockedSuperadmin,
		int problemId, String problemGrade, String problemName, boolean problemLockedAdmin, boolean problemLockedSuperadmin,
		String date, String name) {}