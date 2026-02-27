package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record Toc(int numRegions, int numAreas, int numSectors, int numProblems, List<TocRegion> regions) {}