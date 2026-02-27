package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record Comment(int id, int idProblem, String comment, boolean danger, boolean resolved, boolean delete, List<NewMedia> newMedia) {}