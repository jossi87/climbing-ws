package com.buldreinfo.jersey.jaxb.metadata.beans;

import com.google.common.base.Strings;

public class Setup {
	private final int idRegion;
	private final boolean isBouldering;
	private final String title;
	private final String domain;
	private final String description;
	private final boolean showLogoPlay;
	private final boolean showLogoSis;
	private final boolean showLogoBrv;
	
	public Setup(int idRegion, boolean isBouldering, String title, String domain, String description, boolean showLogoPlay, boolean showLogoSis, boolean showLogoBrv) {
		this.idRegion = idRegion;
		this.isBouldering = isBouldering;
		this.title = title;
		this.domain = domain;
		this.description = description;
		this.showLogoPlay = showLogoPlay;
		this.showLogoSis = showLogoSis;
		this.showLogoBrv = showLogoBrv;
	}
	
	public String getDescription() {
		return description;
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
	
	public boolean isShowLogoBrv() {
		return showLogoBrv;
	}
	
	public boolean isShowLogoPlay() {
		return showLogoPlay;
	}
	
	public boolean isShowLogoSis() {
		return showLogoSis;
	}

	@Override
	public String toString() {
		return "Setup [idRegion=" + idRegion + ", isBouldering=" + isBouldering + ", title=" + title + ", domain="
				+ domain + ", description=" + description + ", showLogoPlay=" + showLogoPlay + ", showLogoSis="
				+ showLogoSis + ", showLogoBrv=" + showLogoBrv + "]";
	}
}