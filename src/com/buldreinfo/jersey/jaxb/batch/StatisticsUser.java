package com.buldreinfo.jersey.jaxb.batch;

import java.util.TreeMap;

public class StatisticsUser {
	private final String name;
	private final TreeMap<Integer, Integer> decadeFas = new TreeMap<>();

	public StatisticsUser(String name) {
		this.name = name;
	}
	
	public void addFa(int decade) {
		int num = decadeFas.getOrDefault(decade, 0);
		decadeFas.put(decade, num+1);
	}
	
	public String getName() {
		return name;
	}
	
	public int getNumOnDecade(int decade) {
		return decadeFas.getOrDefault(decade, 0);
	}
	
	public int getTotal() {
		return decadeFas.values().stream().mapToInt(val -> val).sum();
	}
}