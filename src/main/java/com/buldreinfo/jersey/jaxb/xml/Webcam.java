package com.buldreinfo.jersey.jaxb.xml;

public class Webcam {
	private String id;
	private String lastUpdated;
	private String name;
	private String urlStillImage;
	private String urlYr;
	private String urlOther;
	private double lat;
	private double lng;
	
	public Webcam() {
	}
	
	public Webcam(String id, String lastUpdated, String name, String urlStillImage, String urlYr, String urlOther, double lat, double lng) {
		this.id = id;
		this.lastUpdated = lastUpdated;
		this.name = name;
		this.urlStillImage = urlStillImage;
		this.urlYr = urlYr;
		this.urlOther = urlOther;
		this.lat = lat;
		this.lng = lng;
	}

	public String getId() {
		return id;
	}
	
	public String getLastUpdated() {
		return lastUpdated;
	}
	
	public double getLat() {
		return lat;
	}
	
	public double getLng() {
		return lng;
	}
	
	public String getName() {
		return name;
	}
	
	public String getUrlOther() {
		return urlOther;
	}
	
	public String getUrlStillImage() {
		return urlStillImage;
	}
	
	public String getUrlYr() {
		return urlYr;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public void setLastUpdated(String lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
	
	public void setLat(double lat) {
		this.lat = lat;
	}
	
	public void setLng(double lng) {
		this.lng = lng;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setUrlOther(String urlOther) {
		this.urlOther = urlOther;
	}
	
	public void setUrlStillImage(String urlStillImage) {
		this.urlStillImage = urlStillImage;
	}
	
	public void setUrlYr(String urlYr) {
		this.urlYr = urlYr;
	}

	@Override
	public String toString() {
		return "Camera [id=" + id + ", lastUpdated=" + lastUpdated + ", name=" + name + ", urlStillImage="
				+ urlStillImage + ", urlYr=" + urlYr + ", lat=" + lat + ", lng=" + lng + "]";
	}
}