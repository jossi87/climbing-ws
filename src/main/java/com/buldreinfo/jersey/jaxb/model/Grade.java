package com.buldreinfo.jersey.jaxb.model;

public record Grade(
		int id,
		String grade,
		@Deprecated int deprecatedGradeId // TODO Remove deprecated old id
		) {}