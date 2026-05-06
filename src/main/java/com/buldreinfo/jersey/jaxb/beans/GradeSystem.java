package com.buldreinfo.jersey.jaxb.beans;

@Deprecated // TODO 2026-05-06 - Remove GradeSystem
public enum GradeSystem {
	CLIMBING, BOULDER, ICE;
	
	public static GradeSystem ofGroup(String group) {
		return switch (group) {
		case "Bouldering" -> GradeSystem.BOULDER;
		case "Climbing" -> GradeSystem.CLIMBING;
		case "Ice" -> GradeSystem.ICE;
		default -> throw new IllegalArgumentException("Invalid group: " + group);
		};
	}
}