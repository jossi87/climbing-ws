package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record ProblemSection(int id, int nr, String description, String grade, List<Media> media) {}