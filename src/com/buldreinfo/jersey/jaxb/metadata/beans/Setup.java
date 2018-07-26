package com.buldreinfo.jersey.jaxb.metadata.beans;

import com.google.common.base.Strings;

public class Setup {
	private final int idRegion;
	private final boolean isBouldering;
	private final String title;
	private final String domain;
	
	public Setup(int idRegion, boolean isBouldering, String title, String domain) {
		this.idRegion = idRegion;
		this.isBouldering = isBouldering;
		this.title = title;
		this.domain = domain;
	}
	
	public String getDomain() {
		return domain;
	}
	
	public int getIdRegion() {
		return idRegion;
	}
	
	public String getTitle(String prefix) {
		if (Strings.isNullOrEmpty(prefix)) {
			return title;
		}
		return prefix + " | " + title;
	}
	
	public String getUrl(String suffix) {
		return "https://" + domain + suffix;
	}
	
	public boolean isBouldering() {
		return isBouldering;
	}
}