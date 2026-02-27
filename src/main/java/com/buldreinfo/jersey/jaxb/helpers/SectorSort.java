package com.buldreinfo.jersey.jaxb.helpers;

import com.google.common.collect.ComparisonChain;

public class SectorSort {
	public static int sortSector(int sorting1, String name1, int sorting2, String name2) {
		return ComparisonChain.start()
				.compare(sorting1, sorting2)
				.compare(parseName(name1), parseName(name2))
				.result();
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