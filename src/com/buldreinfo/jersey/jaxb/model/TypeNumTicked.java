package com.buldreinfo.jersey.jaxb.model;

public class TypeNumTicked {
	private final String type;
	private int num;
	private int ticked;
	
	public TypeNumTicked(String type, int num, int ticked) {
		this.type = type;
		this.num = num;
		this.ticked = ticked;
	}

	public String getType() {
		return type;
	}
	
	public int getNum() {
		return num;
	}
	
	public int getTicked() {
		return ticked;
	}

	public void addNum(int num) {
		this.num += num;
	}

	public void addTicked(int ticked) {
		this.ticked += ticked;
	}
}