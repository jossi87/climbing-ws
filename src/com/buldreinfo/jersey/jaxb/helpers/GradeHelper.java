package com.buldreinfo.jersey.jaxb.helpers;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;

public class GradeHelper {
	public static ImmutableBiMap<Integer, String> getGrades(int regionId) {
		Map<Integer, String> map = new LinkedHashMap<>();
		if (regionId == 4) {
			map.put(19, "10");
			map.put(18, "9+");
			map.put(17, "9");
			map.put(16, "8+");
			map.put(15, "8");
			map.put(14, "7+");
			map.put(13, "7");
			map.put(12, "6+");
			map.put(11, "6");
			map.put(10, "5+");
			map.put(9, "5");
			map.put(8, "4+");
			map.put(7, "4");
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
	
	public static String intToString(int regionId, int grade) throws SQLException {
		return Preconditions.checkNotNull(getGrades(regionId).get(grade), "Invalid grade=" + grade);
	}
	
	public static int stringToInt(int regionId, String grade) throws SQLException {
		Preconditions.checkNotNull(grade, "grade is null");
		return getGrades(regionId).inverse().get(grade);
	}
}