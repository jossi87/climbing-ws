package com.buldreinfo.model;

/**
 * One row of the reference table {@code climbing.map} joined with {@code climbing.region_map}: a base map
 * tile provider offered for a region. The providers a region offers, and which one is selected by default,
 * live in the database (the {@code map} / {@code region_map} tables) instead of being hard-coded in the client.
 */
public record Map(int id, String name, String url, String attribution, int maxZoom, boolean isDefault) {}
