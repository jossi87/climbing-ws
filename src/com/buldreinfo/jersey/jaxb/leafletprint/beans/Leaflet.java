package com.buldreinfo.jersey.jaxb.leafletprint.beans;

import java.util.List;

import com.buldreinfo.jersey.jaxb.model.LatLng;

public class Leaflet {
	private final List<Marker> markers;
	private final List<Outline> outlines;
	private final List<String> polylines;
	private final List<String> legends;
	private final LatLng defaultCenter;
	private final int defaultZoom;
	
	public Leaflet(List<Marker> markers, List<Outline> outlines, List<String> polylines, List<String> legends, LatLng defaultCenter, int defaultZoom) {
		this.markers = markers;
		this.outlines = outlines;
		this.polylines = polylines;
		this.legends = legends;
		this.defaultCenter = defaultCenter;
		this.defaultZoom = defaultZoom;
	}
	
	public LatLng getDefaultCenter() {
		return defaultCenter;
	}

	public int getDefaultZoom() {
		return defaultZoom;
	}

	public List<String> getLegends() {
		return legends;
	}

	public List<Marker> getMarkers() {
		return markers;
	}

	public List<Outline> getOutlines() {
		return outlines;
	}

	public List<String> getPolylines() {
		return polylines;
	}

	@Override
	public String toString() {
		return "Leaflet [markers=" + markers + ", outlines=" + outlines + ", polylines=" + polylines + ", legends="
				+ legends + ", defaultCenter=" + defaultCenter + ", defaultZoom=" + defaultZoom + "]";
	}
}