package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record DangerousSector(int id, String url, String name, CompassDirection wallDirectionCalculated, CompassDirection wallDirectionManual, boolean lockedAdmin, boolean lockedSuperadmin, List<DangerousProblem> problems) {}