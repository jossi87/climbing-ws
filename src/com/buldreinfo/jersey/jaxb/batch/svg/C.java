package com.buldreinfo.jersey.jaxb.batch.svg;

public class C implements Element {
	private double x1;
	private double y1;
	private double x2;
	private double y2;
	private double x;
	private double y;

	public C(double x1, double y1, double x2, double y2, double x, double y) {
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
		this.x = x;
		this.y = y;
	}

	public double getX1() {
		return x1;
	}

	public double getY1() {
		return y1;
	}

	public double getX2() {
		return x2;
	}

	public double getY2() {
		return y2;
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
		return String.format("C %f %f %f %f %f %f", x1, y1, x2, y2, x, y);
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
		// x,y
		this.x = (x - matrix[0][2]) / matrix[0][0];
		this.y = (y - matrix[1][2]) / matrix[1][1];
		// x1,y1
		this.x1 = (x1 - matrix[0][2]) / matrix[0][0];
		this.y1 = (y1 - matrix[1][2]) / matrix[1][1];
		// x2,y2
		this.x2 = (x2 - matrix[0][2]) / matrix[0][0];
		this.y2 = (y2 - matrix[1][2]) / matrix[1][1];
	}
}