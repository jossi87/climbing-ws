package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record DangerousArea(int id, String url, String name, boolean lockedAdmin, boolean lockedSuperadmin, int sunFromHour, int sunToHour, List<DangerousSector> sectors) {}