package com.buldreinfo.jersey.jaxb.leafletprint.beans;

import java.util.List;

import com.buldreinfo.jersey.jaxb.model.LatLng;

public record Leaflet(List<Marker> markers, List<Outline> outlines, List<PrintSlope> slopes, List<String> legends, LatLng defaultCenter, int defaultZoom, boolean showPhotoNotMap) {}