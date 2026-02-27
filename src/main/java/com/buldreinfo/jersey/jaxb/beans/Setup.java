package com.buldreinfo.jersey.jaxb.beans;

import java.util.List;

import com.buldreinfo.jersey.jaxb.helpers.GradeConverter;
import com.buldreinfo.jersey.jaxb.model.CompassDirection;
import com.buldreinfo.jersey.jaxb.model.LatLng;

public record Setup(String domain, String url, int idRegion, GradeSystem gradeSystem, GradeConverter gradeConverter, List<CompassDirection> compassDirections, String title, String description, LatLng defaultCenter, int defaultZoom) {
	public static SetupBuilder newBuilder(String domain, GradeSystem gradeSystem) {
		return new SetupBuilder(domain, gradeSystem);
	}

	public static class SetupBuilder {
		private final String domain;
		private final String url;
		private final GradeSystem gradeSystem;
		private int idRegion;
		private GradeConverter gradeConverter;
		private List<CompassDirection> compassDirections;
		private String title;
		private String description;
		private LatLng defaultCenter;
		private int defaultZoom;

		public SetupBuilder(String domain, GradeSystem gradeSystem) {
			this.domain = domain;
			this.url = "https://" + domain;
			this.gradeSystem = gradeSystem;
		}

		public Setup build() {
			return new Setup(domain, url, idRegion, gradeSystem, gradeConverter, compassDirections, title, description, defaultCenter, defaultZoom);
		}

		public SetupBuilder withCompassDirections(List<CompassDirection> compassDirections) {
			this.compassDirections = compassDirections;
			return this;
		}

		public SetupBuilder withDefaultCenter(LatLng defaultCenter) {
			this.defaultCenter = defaultCenter;
			return this;
		}

		public SetupBuilder withDefaultZoom(int defaultZoom) {
			this.defaultZoom = defaultZoom;
			return this;
		}

		public SetupBuilder withDescription(String description) {
			this.description = description;
			return this;
		}
		
		public SetupBuilder withGradeConverter(GradeConverter gradeConverter) {
			this.gradeConverter = gradeConverter;
			return this;
		}
		
		public SetupBuilder withIdRegion(int idRegion) {
			this.idRegion = idRegion;
			return this;
		}

		public SetupBuilder withTitle(String title) {
			this.title = title;
			return this;
		}
	}
}