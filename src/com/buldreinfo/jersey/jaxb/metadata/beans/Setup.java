package com.buldreinfo.jersey.jaxb.metadata.beans;

import com.buldreinfo.jersey.jaxb.model.LatLng;
import com.google.common.base.Strings;

public class Setup {
	private final int idRegion;
	private boolean isBouldering;
	private String title;
	private String domain;
	private String description;
	private LatLng defaultCenter;
	private int defaultZoom;
	private boolean showLogoPlay;
	private boolean showLogoSis;
	private boolean showLogoBrv;
	
	public Setup(int idRegion) {
		this.idRegion = idRegion;
	}

	public int getDefaultZoom() {
		return defaultZoom;
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

	public LatLng getDefaultCenter() {
		return defaultCenter;
	}

	public String getTitle() {
		return title;
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

	public Setup setBouldering(boolean isBouldering) {
		this.isBouldering = isBouldering;
		return this;
	}

	public Setup setDefaultZoom(int defaultZoom) {
		this.defaultZoom = defaultZoom;
		return this;
	}

	public Setup setDescription(String description) {
		this.description = description;
		return this;
	}

	public Setup setDomain(String domain) {
		this.domain = domain;
		return this;
	}

	public Setup setLatLng(double lat, double lng) {
		this.defaultCenter = new LatLng(lat, lng);
		return this;
	}

	public Setup setShowLogoBrv(boolean showLogoBrv) {
		this.showLogoBrv = showLogoBrv;
		return this;
	}

	public Setup setShowLogoPlay(boolean showLogoPlay) {
		this.showLogoPlay = showLogoPlay;
		return this;
	}

	public Setup setShowLogoSis(boolean showLogoSis) {
		this.showLogoSis = showLogoSis;
		return this;
	}

	public Setup setTitle(String title) {
		this.title = title;
		return this;
	}
}