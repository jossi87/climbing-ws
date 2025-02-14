package com.buldreinfo.jersey.jaxb.model;

public record TocProblem(int id, String url, String broken, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String description, Coordinates coordinates, String grade, String fa, int faYear, int numTicks, double stars, boolean ticked, boolean todo, Type t, int numPitches) {}