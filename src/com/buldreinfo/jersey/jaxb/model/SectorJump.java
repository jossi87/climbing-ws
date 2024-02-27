package com.buldreinfo.jersey.jaxb.model;

public record SectorJump(int id, boolean lockedAdmin, boolean lockedSuperadmin, String name, int sorting) {}