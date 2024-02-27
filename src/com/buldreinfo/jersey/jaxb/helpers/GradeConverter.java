package com.buldreinfo.jersey.jaxb.helpers;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.buldreinfo.jersey.jaxb.model.Grade;
import com.google.common.base.Preconditions;

public class GradeConverter {
	public static final String NO_PERSONAL_GRADE = "No personal grade";
	public static final int NO_PERSONAL_GRADE_ID = -1;
	private final List<Grade> grades;
	private final Map<Integer, String> idLookup = new LinkedHashMap<>();
	private final Map<String, Integer> gradeLookup = new LinkedHashMap<>();
	
	public GradeConverter(List<Grade> grades) {
		this.grades = grades;
		for (Grade g : grades) {
			idLookup.put(g.id(), g.grade());
			gradeLookup.put(g.grade(), g.id());
		}
	}
	
	public String getGradeFromIdGrade(int idGrade) {
		if (idGrade == -1) {
			// No personal grade
			return NO_PERSONAL_GRADE;
		}
		String res = idLookup.get(idGrade);
		int i = idGrade;
		while (res == null && i < Collections.max(idLookup.keySet())) {
			res = idLookup.get(++i);
		}
		return Preconditions.checkNotNull(res, "Invalid idGrade=" + idGrade + " (grades=" + grades + ")");
	}
	
	public List<Grade> getGrades() {
		return grades;
	}
	
	public int getIdGradeFromGrade(String grade) {
		Preconditions.checkNotNull(grade, "grade is null");
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
			throw new RuntimeException("Invalid grade=" + grade + " (grades=" + grades + ")");
		}
	}
}