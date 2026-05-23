package com.buldreinfo.jersey.jaxb.model;

public record Comment(int id, int idProblem, String comment, boolean danger, boolean resolved, boolean delete) {}