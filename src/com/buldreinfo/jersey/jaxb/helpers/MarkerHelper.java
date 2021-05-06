package com.buldreinfo.jersey.jaxb.helpers;

import java.util.HashSet;
import java.util.Set;

public class MarkerHelper {
	public class LatLng {
		private final double lat;
		private final double lng;
		public LatLng(double lat, double lng) {
			this.lat = lat;
			this.lng = lng;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			LatLng other = (LatLng) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (Double.doubleToLongBits(lat) != Double.doubleToLongBits(other.lat))
				return false;
			if (Double.doubleToLongBits(lng) != Double.doubleToLongBits(other.lng))
				return false;
			return true;
		}
		public double getLat() {
			return lat;
		}
		public double getLng() {
			return lng;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			long temp;
			temp = Double.doubleToLongBits(lat);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(lng);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			return result;
		}
		private MarkerHelper getOuterType() {
			return MarkerHelper.this;
		}
	}

	private static final double COORDINATE_OFFSET = 0.00002;
	private final Set<LatLng> markers = new HashSet<>();

	public MarkerHelper() {
	}

	public LatLng getLatLng(double latitude, double longitude) {
		LatLng res = new LatLng(latitude, longitude);
		if (res.getLat() == 0 && res.getLng() == 0) {
			return res;
		}
		else if (!markers.contains(res)) {
			markers.add(res);
			return res;
		}
		
		int i = 0;
		while (i < 10000) {
			res = new LatLng((latitude - i * COORDINATE_OFFSET), longitude);
			if (markers.contains(res)) {
				i++;
			}
			else {
				markers.add(res);
				return res;
			}
		}
		throw new RuntimeException("Could not find suitable location for latitude=" + latitude + " and longitude=" + longitude);
	}
}
