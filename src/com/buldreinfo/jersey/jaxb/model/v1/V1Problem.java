package com.buldreinfo.jersey.jaxb.model.v1;

import java.util.List;

public record V1Problem(int sectorId, int id, int nr, String name, String comment, int grade, String fa, double lat, double lng, List<V1Media> media) {}