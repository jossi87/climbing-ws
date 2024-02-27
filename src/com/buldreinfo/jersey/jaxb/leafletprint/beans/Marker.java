package com.buldreinfo.jersey.jaxb.leafletprint.beans;

public record Marker(double lat, double lng, IconType iconType, String label) {}