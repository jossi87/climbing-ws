package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record DangerousSector(int id, String name, CompassDirection wallDirectionCalculated, CompassDirection wallDirectionManual, boolean lockedAdmin, boolean lockedSuperadmin, int sunFromHour, int sunToHour, List<DangerousProblem> problems) {}