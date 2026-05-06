package com.buldreinfo.jersey.jaxb.helpers;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.buldreinfo.jersey.jaxb.model.Grade;

@Deprecated // TODO Remove GradeSystem
public class GradeConverter {
	public static final String NO_PERSONAL_GRADE = "No personal grade";
	public static final int NO_PERSONAL_GRADE_ID = -1;
	private final List<Grade> grades;
	private final Map<Integer, String> idLookup = new LinkedHashMap<>();
	private final Map<String, Integer> gradeLookup = new LinkedHashMap<>();
	private final Map<Integer, String> deprecatedIdLookup = new LinkedHashMap<>();
	private final Map<String, Integer> deprecatedGradeLookup = new LinkedHashMap<>();
	
	public GradeConverter(List<Grade> grades) {
		this.grades = grades;
		for (Grade g : grades) {
			idLookup.put(g.id(), g.grade());
			gradeLookup.put(g.grade(), g.id());
			deprecatedIdLookup.put(g.deprecatedGradeId(), g.grade());
			deprecatedGradeLookup.put(g.grade(), g.deprecatedGradeId());
		}
	}
	
	public String getGradeFromDeprecatedIdGrade(int deprecatedIdGrade) {
		if (deprecatedIdGrade == -1) {
			// No personal grade
			return NO_PERSONAL_GRADE;
		}
		String res = deprecatedIdLookup.get(deprecatedIdGrade);
		int i = deprecatedIdGrade;
		while (res == null && i < Collections.max(deprecatedIdLookup.keySet())) {
			res = deprecatedIdLookup.get(++i);
		}
		return Objects.requireNonNull(res, "Invalid deprecatedIdGrade=" + deprecatedIdGrade + " (grades=" + grades + ")");
	}
	
	public List<Grade> getGrades() {
		return grades;
	}
	
	public int getDeprecatedIdGradeFromGrade(String grade) {
		Objects.requireNonNull(grade, "grade is null");
		if (grade.equals(NO_PERSONAL_GRADE)) {
			// No personal grade
			return NO_PERSONAL_GRADE_ID;
		}
		try {
			return deprecatedGradeLookup.get(grade);
		} catch (NullPointerException e) {
			// Check for first part...
			for (String key : deprecatedGradeLookup.keySet()) {
				if (key.contains(" ") && key.substring(0, key.indexOf(" ")).equals(grade)) {
					return deprecatedGradeLookup.get(key);
				}
			}
			// Check for last part...
			for (String key : deprecatedGradeLookup.keySet()) {
				if (key.endsWith("(" + grade + ")")) {
					return deprecatedGradeLookup.get(key);
				}
			}
			throw new RuntimeException("Invalid grade=" + grade + " (grades=" + grades + ") - message=" + e.getMessage());
		}
	}
	
	public int getIdGradeFromGrade(String grade) {
		Objects.requireNonNull(grade, "grade is null");
		if (grade.equals(NO_PERSONAL_GRADE)) {
			// No personal grade
			return NO_PERSONAL_GRADE_ID;
		}
		try {
			return gradeLookup.get(grade);
		} catch (NullPointerException e) {
			// Check for first part...
			for (String key : gradeLookup.keySet()) {
				if (key.contains(" ") && key.substring(0, key.indexOf(" ")).equals(grade)) {
					return gradeLookup.get(key);
				}
			}
			// Check for last part...
			for (String key : gradeLookup.keySet()) {
				if (key.endsWith("(" + grade + ")")) {
					return gradeLookup.get(key);
				}
			}
			throw new RuntimeException("Invalid grade=" + grade + " (grades=" + grades + ") - message=" + e.getMessage());
		}
	}
}