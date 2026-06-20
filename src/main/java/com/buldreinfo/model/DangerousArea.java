package com.buldreinfo.model;

import java.util.List;

public record DangerousArea(int id, String name, boolean lockedAdmin, boolean lockedSuperadmin, int sunFromHour, int sunToHour, List<DangerousSector> sectors) {
	public record DangerousSector(int id, String name, CompassDirection wallDirectionCalculated, CompassDirection wallDirectionManual, boolean lockedAdmin, boolean lockedSuperadmin, int sunFromHour, int sunToHour, List<DangerousProblem> problems) {}
	public record DangerousProblem(int id, String broken, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String grade, String postBy, String postWhen, String postTxt) {}
}