package com.buldreinfo.jersey.jaxb.leafletprint.beans;

import java.util.List;

import com.buldreinfo.jersey.jaxb.model.LatLng;

public class Leaflet {
	private final List<Marker> markers;
	private final List<Outline> outlines;
	private final List<Polyline> polylines;
	private final LatLng defaultCenter;
	private final int defaultZoom;
	
	public Leaflet(List<Marker> markers, List<Outline> outlines, List<Polyline> polylines, LatLng defaultCenter, int defaultZoom) {
		this.markers = markers;
		this.outlines = outlines;
		this.polylines = polylines;
		this.defaultCenter = defaultCenter;
		this.defaultZoom = defaultZoom;
	}

	public List<Marker> getMarkers() {
		return markers;
	}

	public List<Outline> getOutlines() {
		return outlines;
	}

	public List<Polyline> getPolylines() {
		return polylines;
	}

	public LatLng getDefaultCenter() {
		return defaultCenter;
	}

	public int getDefaultZoom() {
		return defaultZoom;
	}

	@Override
	public String toString() {
		return "Leaflet [markers=" + markers + ", outlines=" + outlines + ", polylines=" + polylines
				+ ", defaultCenter=" + defaultCenter + ", defaultZoom=" + defaultZoom + "]";
	}
}