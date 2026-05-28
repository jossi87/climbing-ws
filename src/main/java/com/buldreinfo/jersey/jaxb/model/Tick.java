package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record Tick(boolean delete, int id, int idProblem, String comment, String date, double stars, String grade, List<TickRepeat> repeats) {
	public record TickRepeat(int id, int tickId, String comment, String date) {}
}