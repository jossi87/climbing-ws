package com.buldreinfo.model;

import java.util.List;

import com.buldreinfo.helpers.SectorSort;

public record Toc(int numRegions, int numAreas, int numSectors, int numProblems, List<TocRegion> regions) {
	public record TocRegion(int id, String name, List<TocArea> areas) {}
	public record TocArea(int id, String url, String name, Coordinates coordinates, boolean lockedAdmin, boolean lockedSuperadmin, int sunFromHour, int sunToHour, List<TocSector> sectors) {
		public void orderSectors() {
			if (sectors != null) {
				sectors.sort((TocSector o1, TocSector o2) -> SectorSort.sortSector(o1.sorting(), o1.name(), o2.sorting(), o2.name()));
			}
		}
	}
	public record TocSector(int id, String url, String name, int sorting, Coordinates parking, List<Coordinates> outline, CompassDirection wallDirectionCalculated, CompassDirection wallDirectionManual, boolean lockedAdmin, boolean lockedSuperadmin, int sunFromHour, int sunToHour, List<TocProblem> problems) {}
	public record TocProblem(int id, String url, String broken, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String description, int lengthMeter, int startingAltitude, Coordinates coordinates, String grade, String faUser, int faYear, String ffaUser, int ffaYear, int numTicks, double stars, boolean ticked, boolean todo, Type t, int numPitches) {}
	public record TocPitch(String regionName, String url, String areaName, String sectorName, String problemName, int pitch, String grade, String description) {}
}