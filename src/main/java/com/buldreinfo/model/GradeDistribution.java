package com.buldreinfo.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Grade distribution for one grade: how many problems of each type sit in the sectors/regions the chart shows.
 * The per-type columns are keyed by {@code climbing.type.id} and labelled with the DB subtype, so the API never has to
 * know type names.
 */
public class GradeDistribution {
	/** Problems of one type within a chart row. */
	public record GradeDistributionType(Type type, int num) {}

	public static class GradeDistributionRow {
		private final int id;
		private final String name;
		private final List<GradeDistributionType> types = new ArrayList<>();
		public GradeDistributionRow(int id, String name) {
			this.id = id;
			this.name = name;
		}
		/** Adds the problems of one type, merging repeated visits (a row arrives once per type in the query). */
		public void addType(Type type, int num) {
			for (int i = 0; i < types.size(); i++) {
				var existing = types.get(i);
				if (existing.type().id() == type.id()) {
					types.set(i, new GradeDistributionType(existing.type(), existing.num() + num));
					return;
				}
			}
			types.add(new GradeDistributionType(type, num));
		}
		public int getId() {
			return id;
		}
		public String getName() {
			return name;
		}
		public int getNum() {
			return types.stream().mapToInt(GradeDistributionType::num).sum();
		}
		public List<GradeDistributionType> getTypes() {
			return types;
		}
	}
	private final String grade;
	private final String color;
	private int num = 0;
	private int prim = 0;
	private int sec = 0;
	private final List<GradeDistributionRow> rows = new ArrayList<>();
	
	public GradeDistribution(String grade, String color) {
		this.grade = grade;
		this.color = color;
	}
	
	/**
	 * Adds the problems of one type in one sector/region. {@code primary} is the two-tone bar shading the chart draws
	 * (bouldering, ice climbing and bolted routes use the bright "primary" shade) and is computed from the type and
	 * discipline flags in SQL — never from the type or subtype name.
	 */
	public void addSector(int id, String name, Type type, boolean primary, int num) {
		GradeDistributionRow row = rows.stream().filter(x -> x.getId() == id).findAny().orElseGet(() -> {
			GradeDistributionRow added = new GradeDistributionRow(id, name);
			rows.add(added);
			return added;
		});
		row.addType(type, num);
		if (primary) {
			this.prim += num;
		} else {
			this.sec += num;
		}
		this.num += num;
	}
	
	public String getColor() {
		return color;
	}

	public String getGrade() {
		return grade;
	}
	
	public int getNum() {
		return num;
	}

	public int getPrim() {
		return prim;
	}

	public List<GradeDistributionRow> getRows() {
		return rows;
	}

	public int getSec() {
		return sec;
	}
}