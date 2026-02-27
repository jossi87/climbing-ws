package com.buldreinfo.jersey.jaxb.model;

public record Svg(boolean delete, int id, int problemId, String problemName, String problemGrade, int problemGradeGroup, String problemSubtype, int nr,
		int pitch, String path, boolean hasAnchor, String texts, String anchors, String tradBelayStations,
		boolean primary, boolean ticked, boolean todo, boolean dangerous) {}