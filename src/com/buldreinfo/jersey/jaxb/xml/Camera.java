package com.buldreinfo.jersey.jaxb.xml;

public class Camera {
	private String id;
	private String lastUpdated;
	private String name;
	private String urlStillImage;
	private String urlYr;
	private double lat;
	private double lng;
	
	public Camera() {
	}
	
	public Camera(String id, String lastUpdated, String name, String urlStillImage, String urlYr, double lat, double lng) {
		this.id = id;
		this.lastUpdated = lastUpdated;
		this.name = name;
		this.urlStillImage = urlStillImage;
		this.urlYr = urlYr;
		this.lat = lat;
		this.lng = lng;
	}

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getLastUpdated() {
		return lastUpdated;
	}
	
	public void setLastUpdated(String lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getUrlStillImage() {
		return urlStillImage;
	}
	
	public void setUrlStillImage(String urlStillImage) {
		this.urlStillImage = urlStillImage;
	}
	
	public String getUrlYr() {
		return urlYr;
	}
	
	public void setUrlYr(String urlYr) {
		this.urlYr = urlYr;
	}
	
	public double getLat() {
		return lat;
	}
	
	public void setLat(double lat) {
		this.lat = lat;
	}
	
	public double getLng() {
		return lng;
	}
	
	public void setLng(double lng) {
		this.lng = lng;
	}

	@Override
	public String toString() {
		return "Camera [id=" + id + ", lastUpdated=" + lastUpdated + ", name=" + name + ", urlStillImage="
				+ urlStillImage + ", urlYr=" + urlYr + ", lat=" + lat + ", lng=" + lng + "]";
	}
}