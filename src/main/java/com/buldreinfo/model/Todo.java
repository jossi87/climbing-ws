package com.buldreinfo.model;

import java.util.List;

public record Todo (List<TodoSector> sectors) {
	public record TodoSector(int id, String name, boolean lockedAdmin, boolean lockedSuperadmin, List<TodoProblem> problems) {}
	public record TodoProblem(int id, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String grade, List<User> partners) {}
}