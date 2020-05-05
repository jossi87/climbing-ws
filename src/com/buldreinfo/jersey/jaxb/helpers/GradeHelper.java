package com.buldreinfo.jersey.jaxb.helpers;

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.buldreinfo.jersey.jaxb.metadata.beans.Setup;
import com.buldreinfo.jersey.jaxb.model.GradeDistribution;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Sets;

public class GradeHelper {
	public static ImmutableBiMap<Integer, String> getGrades(Setup setup) {
		Map<Integer, String> map = new LinkedHashMap<>();
		if (!setup.isBouldering()) {
			map.put(55, "10- (9a)");
			map.put(54, "9+/10- (8c+)");
			map.put(53, "9+ (8c)");
			map.put(52, "9/9+ (8b+)");
			map.put(51, "9 (8b)");
			map.put(50, "9-/9 (8a+)");
			map.put(49, "9- (8a)");
			map.put(48, "8+/9- (7c+/8a)");
			map.put(47, "8+ (7c+)");
			map.put(46, "8/8+ (7c)");
			map.put(45, "8 (7b+)");
			map.put(44, "8-/8 (7b)");
			map.put(43, "8- (7b)");
			map.put(42, "7+/8- (7a+)");
			map.put(41, "7+ (7a)");
			map.put(40, "7/7+ (6c+)");
			map.put(39, "7 (6c)");
			map.put(38, "7-/7 (6b+)");
			map.put(37, "7- (6b+)");
			map.put(36, "6+/7- (6b)");
			map.put(35, "6+ (6b)");
			map.put(34, "6/6+ (6a+)");
			map.put(33, "6 (6a+)");
			map.put(32, "6-/6 (6a)");
			map.put(31, "6- (6a)");
			map.put(30, "5+/6- (5c)");
			map.put(29, "5+ (5c)");
			//			map.put(28, "5/5+");
			map.put(27, "5 (5b)");
			//			map.put(26, "5-/5");
			map.put(25, "5- (5a)");
			//			map.put(24, "4+/5-");
			map.put(23, "4+ (4c)");
			//			map.put(22, "4/4+");
			map.put(21, "4 (4b)");
			//			map.put(20, "4-/4");
			map.put(19, "4- (4a)");
			//			map.put(18, "3+/4-");
			map.put(17, "3+");
			//			map.put(16, "3/3+");
			map.put(15, "3");
			//			map.put(14, "3-/3");
			map.put(13, "3-");
			//			map.put(12, "2+/3-");
			//			map.put(11, "2+");
			//			map.put(10, "2/2+");
			//			map.put(9, "2");
			//			map.put(8, "2-/2");
			//			map.put(7, "2-");
			//			map.put(6, "1+/2-");
			//			map.put(5, "1+");
			//			map.put(4, "1/1+");
			//			map.put(3, "1");
			//			map.put(2, "1-/1");
			//			map.put(1, "1-");
			map.put(0, "n/a");
		}
		else {
			map.put(27, "8C");
			map.put(26, "8B+");
			map.put(25, "8B");
			map.put(24, "8A+");
			map.put(23, "8A");
			map.put(22, "7C+");
			map.put(21, "7C");
			map.put(20, "7B+");
			map.put(19, "7B");
			map.put(18, "7A+");
			map.put(17, "7A");
			map.put(16, "6C+");
			map.put(15, "6C");
			map.put(14, "6B+");
			map.put(13, "6B");
			map.put(12, "6A+");
			map.put(11, "6A");
			map.put(10, "5+");
			map.put(9, "5");
			map.put(8, "4+");
			map.put(7, "4");
			map.put(6, "3");
			map.put(0, "n/a");
		}
		return ImmutableBiMap.copyOf(map);
	}

	public static String intToString(Setup setup, int grade) {
		ImmutableBiMap<Integer, String> grades = getGrades(setup);
		String res = grades.get(grade);
		int i = grade;
		while (res == null && i < Collections.max(grades.keySet())) {
			res = grades.get(++i);
		}
		return Preconditions.checkNotNull(res, "Invalid grade=" + grade + " (isBouldering=" + setup.isBouldering() + ")");
	}
	
	public static Map<String, GradeDistribution> getGradeDistributionBase(Setup setup) {
		Map<String, GradeDistribution> res = new LinkedHashMap<>();
		ImmutableBiMap<Integer, String> grades = getGrades(setup);
		for (int i : Sets.newTreeSet(grades.keySet())) {
			if (i == 0) {
				continue;
			}
			String grade = intToStringBase(setup, i);
			if (!res.containsKey(grade)) {
				res.put(grade, new GradeDistribution(grade));
			}
		}
		return res;
	}
	
	public static String intToStringBase(Setup setup, int grade) {
		String res = intToString(setup, grade);
		int ix = res.indexOf("(");
		if (ix > 0) {
			res = res.substring(ix+1, ix+3);
		}
		else {
			res = res.replaceAll("\\-", "").replaceAll("\\+", "");
		}
		if (res.startsWith("3")) {
			return "3";
		}
		else if (res.startsWith("4")) {
			return "4";
		}
		else if (res.startsWith("5")) {
			return "5";
		}
		return res;
	}

	public static int stringToInt(Setup setup, String grade) throws SQLException {
		Preconditions.checkNotNull(grade, "grade is null");
		ImmutableBiMap<String, Integer> grades = getGrades(setup).inverse();
		try {
			return grades.get(grade);
		} catch (NullPointerException e) {
			// Check for first part...
			for (String key : grades.keySet()) {
				if (key.contains(" ") && key.substring(0, key.indexOf(" ")).equals(grade)) {
					return grades.get(key);
				}
			}
			throw e;
		}
	}
}