package com.buldreinfo.jersey.jaxb.model;

public record SectorProblem(int id, String broken, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String rock, String comment, int gradeWeight, String grade,
			String faUser, int faYear, String ffaUser, int ffaYear,
			int lengthMeter, int numPitches,
			boolean hasImages, boolean hasMovies, boolean hasTopo, Coordinates coordinates, int numTicks, double stars, boolean ticked, boolean todo, Type t,
			boolean danger) {}