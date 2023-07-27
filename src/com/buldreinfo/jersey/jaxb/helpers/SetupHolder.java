package com.buldreinfo.jersey.jaxb.helpers;

import java.util.List;

public class SetupHolder {
	private final static long fiftyNineMinutesInMs = 59*60*1000;
	private final List<Setup> setups;
	private final long created;
	
	public SetupHolder(List<Setup> setups, long created) {
		this.setups = setups;
		this.created = created;
	}
	
	public List<Setup> getSetups() {
		return setups;
	}
	
	public long getValidMilliseconds() {
		return fiftyNineMinutesInMs - (System.currentTimeMillis() - created);
	}
}
