package com.buldreinfo.model;

public record AreaBasic(String regionName, int id, boolean lockedAdmin, boolean lockedSuperadmin, boolean forDevelopers, String name, Coordinates coordinates, String pageViews) {
}