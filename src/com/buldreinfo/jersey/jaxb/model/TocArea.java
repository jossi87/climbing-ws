package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

import com.buldreinfo.jersey.jaxb.helpers.SectorSort;

public record TocArea(int id, String url, String name, Coordinates coordinates, boolean lockedAdmin, boolean lockedSuperadmin, int sunFromHour, int sunToHour, List<TocSector> sectors) {
	public void orderSectors() {
		if (sectors != null) {
			sectors.sort((TocSector o1, TocSector o2) -> SectorSort.sortSector(o1.sorting(), o1.name(), o2.sorting(), o2.name()));
		}
	}
}