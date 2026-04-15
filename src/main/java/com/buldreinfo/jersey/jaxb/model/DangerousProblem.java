package com.buldreinfo.jersey.jaxb.model;

public record DangerousProblem(int id, String broken, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String grade, String postBy, String postWhen, String postTxt) {}