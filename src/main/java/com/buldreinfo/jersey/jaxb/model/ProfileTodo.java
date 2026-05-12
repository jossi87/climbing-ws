package com.buldreinfo.jersey.jaxb.model;

import java.util.List;

public record ProfileTodo(List<ProfileTodoArea> areas) {
	public record ProfileTodoArea(int id, String name, boolean lockedAdmin, boolean lockedSuperadmin, List<ProfileTodoSector> sectors) {}
	public record ProfileTodoSector(int id, String name, boolean lockedAdmin, boolean lockedSuperadmin, List<ProfileTodoProblem> problems) {}
	public record ProfileTodoProblem(int todoId, int id, boolean lockedAdmin, boolean lockedSuperadmin, int nr, String name, String grade, Coordinates coordinates, List<User> partners) {}
}