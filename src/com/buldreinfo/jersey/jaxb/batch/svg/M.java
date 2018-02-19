package com.buldreinfo.jersey.jaxb.batch.svg;

public class M implements Element {
	private double x;
	private double y;
	
	public M(double x, double y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public double getX() {
		return x;
	}

	@Override
	public double getY() {
		return y;
	}
	
	@Override
	public String toString() {
		return String.format("M %s %s", x, y);
	}
	
	/**
	 * Matrix:
	 * [a c e
	 *  b d f]
	 * x = ax + cy + e
	 * y = bx + dy + f
	 * --> Inverse!
	 */
	@Override
	public void applyMatrix(double[][] matrix) {
		this.x = (x - matrix[0][2]) / matrix[0][0];
		this.y = (y - matrix[1][2]) / matrix[1][1];
	}
}