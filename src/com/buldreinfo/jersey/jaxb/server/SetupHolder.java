package com.buldreinfo.jersey.jaxb.server;

import java.util.List;

public class SetupHolder {
	private final static long oneHourInMs = 60*60*1000;
	private final List<Setup> setups;
	private final long created;
	
	protected SetupHolder(List<Setup> setups, long created) {
		this.setups = setups;
		this.created = created;
	}
	
	protected List<Setup> getSetups() {
		return setups;
	}
	
	protected long getValidMilliseconds() {
		return oneHourInMs - (System.currentTimeMillis() - created);
	}
}
