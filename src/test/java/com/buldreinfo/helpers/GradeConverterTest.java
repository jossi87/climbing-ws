package com.buldreinfo.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.buldreinfo.exception.ValidationFailedException;
import com.buldreinfo.model.Grade;

public class GradeConverterTest {
	private static Grade grade(int id, String label, int weight) {
		return new Grade(id, label, label, label, weight, "#CCCCCC");
	}

	/** The system the assertions below use: labels that carry an alias in brackets, and one plain label. */
	private static final List<Grade> GRADES = List.of(
			grade(1, "n/a", 0),
			grade(2, "7a+ (7a+)", 10),
			grade(3, "7b (7b)", 20),
			grade(4, "10- (6b+)", 30));

	@Test
	public void exactLabelResolves() {
		var converter = new GradeConverter(GRADES);
		assertEquals(2, converter.getIdGradeFromGrade("7a+ (7a+)"));
		assertEquals(3, converter.getIdGradeFromGrade("7b (7b)"));
	}

	@Test
	public void noPersonalGradeSentinelResolvesToMinusOne() {
		assertEquals(GradeConverter.NO_PERSONAL_GRADE_ID,
				new GradeConverter(GRADES).getIdGradeFromGrade(GradeConverter.NO_PERSONAL_GRADE));
	}

	/** A client that sends only the first word (`10-` of `10- (6b+)`) still names one grade. */
	@Test
	public void firstWordOfTheLabelResolves() {
		assertEquals(4, new GradeConverter(GRADES).getIdGradeFromGrade("10-"));
	}

	/** …and so does one that sends only the alias in brackets (`6b+` of `10- (6b+)`). */
	@Test
	public void bracketedAliasOfTheLabelResolves() {
		assertEquals(4, new GradeConverter(GRADES).getIdGradeFromGrade("6b+"));
	}

	@Test
	public void unknownLabelIsAValidationError() {
		assertThrows(ValidationFailedException.class, () -> new GradeConverter(GRADES).getIdGradeFromGrade("9c"));
	}

	@Test
	public void blankLabelIsAValidationError() {
		var converter = new GradeConverter(GRADES);
		assertThrows(ValidationFailedException.class, () -> converter.getIdGradeFromGrade(null));
		assertThrows(ValidationFailedException.class, () -> converter.getIdGradeFromGrade(""));
		assertThrows(ValidationFailedException.class, () -> converter.getIdGradeFromGrade("   "));
	}

	/** A label repeated in one system keeps a single position, and it is the easiest one. */
	@Test
	public void repeatedLabelMapsToItsEasiestGrade() {
		var converter = new GradeConverter(List.of(grade(5, "7", 40), grade(6, "7", 50)));
		assertEquals(5, converter.getIdGradeFromGrade("7"));
	}
}
