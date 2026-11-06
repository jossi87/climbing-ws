package com.buldreinfo.model;

import java.util.ArrayList;
import java.util.List;

public class GradeGraph {
	private final int gradeSystemId;
	private final String gradeSystem;
	private final List<GradeDistribution> gradeDistribution = new ArrayList<>();

	public GradeGraph(int gradeSystemId, String gradeSystem) {
		this.gradeSystemId = gradeSystemId;
		this.gradeSystem = gradeSystem;
	}

	public GradeDistribution addGrade(String grade, String color) {
		for (var existing : gradeDistribution) {
			if (existing.getGrade().equals(grade)) {
				return existing;
			}
		}
		var added = new GradeDistribution(grade, color);
		gradeDistribution.add(added);
		return added;
	}

	public int getGradeSystemId() {
		return gradeSystemId;
	}

	public String getGradeSystem() {
		return gradeSystem;
	}

	public int getNum() {
		return gradeDistribution.stream().mapToInt(GradeDistribution::getNum).sum();
	}

	public List<GradeDistribution> getGradeDistribution() {
		return gradeDistribution;
	}
}
