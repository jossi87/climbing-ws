package com.buldreinfo.helpers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.buldreinfo.exception.ValidationFailedException;
import com.buldreinfo.model.Grade;

/**
 * Turns the grade label a client sends into the id of that grade in one grade system.
 *
 * A client only ever knows labels ({@code 7a+}, {@code 6B+ (6b+)}), never ids, so every write path goes through here.
 * The system is fixed by the caller — the sector the problem belongs to, or the problem a tick is saved on — because
 * weights and labels of different systems are unrelated.
 */
public class GradeConverter {
	public static final String NO_PERSONAL_GRADE = "No personal grade";
	public static final int NO_PERSONAL_GRADE_ID = -1;
	/** Grade label → grade id. A label repeated inside one system maps to its easiest occurrence. */
	private final Map<String, Integer> gradeLookup = new LinkedHashMap<>();

	public GradeConverter(List<Grade> grades) {
		for (Grade g : grades) {
			gradeLookup.putIfAbsent(g.grade(), g.id());
		}
	}

	/**
	 * Grade id of {@code grade} in this converter's system, or {@link #NO_PERSONAL_GRADE_ID} for the
	 * {@link #NO_PERSONAL_GRADE} sentinel (a tick the user did not grade).
	 *
	 * A missing or unknown label is a client error, not a server failure: it names the age of a client rather than a
	 * broken server, so it is reported as a validation error instead of a 500.
	 */
	public int getIdGradeFromGrade(String grade) {
		if (grade == null || grade.isBlank()) {
			throw new ValidationFailedException("Grade is required");
		}
		if (grade.equals(NO_PERSONAL_GRADE)) {
			return NO_PERSONAL_GRADE_ID;
		}
		Integer id = gradeLookup.get(grade);
		if (id == null) {
			id = getIdFromPartialLabel(grade);
		}
		if (id == null) {
			throw new ValidationFailedException("Unknown grade '" + grade + "'");
		}
		return id;
	}

	/**
	 * Label of a grade that also carries a suffix or a bracketed alias — {@code 7a} for {@code 7a+ (7a+)}, and the
	 * long form either way round, which is what older clients send.
	 */
	private Integer getIdFromPartialLabel(String grade) {
		// The first word (`7a` of `7a+ (7a+)`)
		for (var entry : gradeLookup.entrySet()) {
			String label = entry.getKey();
			if (label.contains(" ") && label.substring(0, label.indexOf(" ")).equals(grade)) {
				return entry.getValue();
			}
		}
		// The bracketed alias (`7a+` of `7a+ (7a+)`)
		for (var entry : gradeLookup.entrySet()) {
			if (entry.getKey().endsWith("(" + grade + ")")) {
				return entry.getValue();
			}
		}
		return null;
	}
}