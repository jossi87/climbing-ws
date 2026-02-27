package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record TodoSector(int id, String name, boolean lockedAdmin, boolean lockedSuperadmin, List<TodoProblem> problems) {}