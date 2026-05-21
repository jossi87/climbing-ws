package com.buldreinfo.jersey.jaxb.model;

public record ProblemSearchResult(int id, String areaName, String sectorName, String problemName, String grade, int numPitches) {}