package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record TocSector(int id, String url, String name, int sorting, Coordinates parking, List<Coordinates> outline, CompassDirection wallDirectionCalculated, CompassDirection wallDirectionManual, boolean lockedAdmin, boolean lockedSuperadmin, int sunFromHour, int sunToHour, List<TocProblem> problems) {}