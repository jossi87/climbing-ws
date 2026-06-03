package com.buldreinfo.jersey.jaxb.leafletprint.beans;

import java.util.List;

import com.buldreinfo.jersey.jaxb.model.Coordinates;
import com.buldreinfo.jersey.jaxb.model.Trail;

public record PrintSlope(Slope slope, String backgroundColor) {
	public record Slope(List<Coordinates> coordinates, double calculatedDurationInMinutes, long distance, long elevationGain, long elevationLoss) {}
	public static PrintSlope of(Trail t) {
		var slope = new Slope(t.path(), t.calculatedDurationInMinutes(), t.distance(), t.elevationGain(), t.elevationLoss());
		return new PrintSlope(slope, t.isDescent() ? "purple" : "lime");
	}
}