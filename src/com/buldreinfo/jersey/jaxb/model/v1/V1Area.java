package com.buldreinfo.jersey.jaxb.model.v1;

import java.util.List;

public record V1Area(int regionId, int id, String name, String comment, double lat, double lng, List<V1Sector> sectors) {}