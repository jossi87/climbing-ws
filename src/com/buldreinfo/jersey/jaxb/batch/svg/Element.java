package com.buldreinfo.jersey.jaxb.batch.svg;

public interface Element {
	public double getX();
	public double getY();
	public void applyMatrix(double[][] matrix);
}
