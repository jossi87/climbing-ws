package com.buldreinfo.jersey.jaxb.model;

public class OpenGraph {
	private final String url;
	private final String image;
	private final int imageWidth;
	private final int imageHeight;
	private final String video;
	private final String fbAppId = "275320366630912";
	
	public OpenGraph(String url, String image, int imageWidth, int imageHeight, String video) {
		this.url = url;
		this.image = image;
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		this.video = video;
	}
	
	public String getImage() {
		return image;
	}
	
	public int getImageHeight() {
		return imageHeight;
	}
	
	public int getImageWidth() {
		return imageWidth;
	}
	
	public String getUrl() {
		return url;
	}
	
	public String getFbAppId() {
		return fbAppId;
	}
	
	public String getVideo() {
		return video;
	}
}