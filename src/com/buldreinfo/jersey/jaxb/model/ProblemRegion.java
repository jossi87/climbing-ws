package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record ProblemRegion(int id, String name, List<ProblemRegionArea> areas) {}