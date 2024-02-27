package com.buldreinfo.jersey.jaxb.model.v1;

import java.util.List;

public record V1Sector(int areaId, int id, String name, String comment, double lat, double lng, List<V1Media> media, List<V1Problem> problems) {}