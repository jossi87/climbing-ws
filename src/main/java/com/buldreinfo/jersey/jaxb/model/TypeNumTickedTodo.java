package com.buldreinfo.jersey.jaxb.model;

public class TypeNumTickedTodo {
	private final String type;
	private int num;
	private int ticked;
	private int todo;
	
	public TypeNumTickedTodo(String type, int num, int ticked, int todo) {
		this.type = type;
		this.num = num;
		this.ticked = ticked;
		this.todo = todo;
	}
	
	public void addNum(int num) {
		this.num += num;
	}

	public void addTicked(int ticked) {
		this.ticked += ticked;
	}
	
	public void addTodo(int num) {
		this.todo += num;
	}
	
	public int getNum() {
		return num;
	}

	public int getTicked() {
		return ticked;
	}
	
	public int getTodo() {
		return todo;
	}

	public String getType() {
		return type;
	}
}