package com.buldreinfo.jersey.jaxb.model;

public record Grade(
		int id,
		String grade,
		String labelMajor,
		String color,
		@Deprecated int deprecatedGradeId // TODO Remove deprecated old id
		) {}