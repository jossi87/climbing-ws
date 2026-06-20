package com.buldreinfo.leafletprint.beans;

public record Marker(double lat, double lng, IconType iconType, String label) {}