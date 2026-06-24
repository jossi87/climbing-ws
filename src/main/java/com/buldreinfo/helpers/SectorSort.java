package com.buldreinfo.helpers;


public class SectorSort {
	public static int sortSector(int sorting1, String name1, int sorting2, String name2) {
		int cmp = Integer.compare(sorting1, sorting2);
		if (cmp != 0) return cmp;
		return parseName(name1).compareTo(parseName(name2));
	}

	public static String parseName(String name) {
		return name.toLowerCase()
				.replace("første", "1første")
				.replace("sør", "1sør")
				.replace("vest", "1vest")
				.replace("venstre", "1venstre")
				.replace("andre", "2andre")
				.replace("midt", "2midt")
				.replace("tredje", "3tredje")
				.replace("hoved", "3hoved")
				.replace("fjerde", "4fjerde")
				.replace("høyre", "4høyre")
				.replace("øst", "5øst")
				.replace("nord", "6nord");
	}
}