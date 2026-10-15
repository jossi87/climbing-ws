package com.buldreinfo.beans;

import java.util.List;

import com.buldreinfo.model.CompassDirection;
import com.buldreinfo.model.GradeSystem;
import com.buldreinfo.model.LatLng;

public record Setup(String domain, String url, int idRegion, boolean isBouldering, boolean isClimbing, boolean isIce, List<GradeSystem> gradeSystems, List<CompassDirection> compassDirections, String title, String description, LatLng defaultCenter, int defaultZoom, String defaultLanguage) {
	public static SetupBuilder newBuilder(String domain, boolean isBouldering, boolean isClimbing, boolean isIce) {
		return new SetupBuilder(domain, isBouldering, isClimbing, isIce);
	}

	public static class SetupBuilder {
		private List<CompassDirection> compassDirections;
		private LatLng defaultCenter;
		private String defaultLanguage;
		private int defaultZoom;
		private String description;
		private final String domain;
		private List<GradeSystem> gradeSystems;
		private int idRegion;
		private final boolean isBouldering;
		private final boolean isClimbing;
		private final boolean isIce;
		private String title;
		private final String url;

		public SetupBuilder(String domain, boolean isBouldering, boolean isClimbing, boolean isIce) {
			this.domain = domain;
			this.url = "https://" + domain;
			this.isBouldering = isBouldering;
			this.isClimbing = isClimbing;
			this.isIce = isIce;
		}

		public Setup build() {
			return new Setup(domain, url, idRegion, isBouldering, isClimbing, isIce, gradeSystems, compassDirections, title, description, defaultCenter, defaultZoom, defaultLanguage);
		}

		public SetupBuilder withCompassDirections(List<CompassDirection> compassDirections) {
			this.compassDirections = compassDirections;
			return this;
		}

		public SetupBuilder withDefaultCenter(LatLng defaultCenter) {
			this.defaultCenter = defaultCenter;
			return this;
		}

		public SetupBuilder withDefaultLanguage(String defaultLanguage) {
			this.defaultLanguage = defaultLanguage;
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

		public SetupBuilder withGradeSystems(List<GradeSystem> gradeSystems) {
			this.gradeSystems = gradeSystems;
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

	public CompassDirection getCompassDirection(int id) {
		if (id == 0) {
			return null;
		}
		return compassDirections()
				.stream()
				.filter(cd -> cd.id() == id)
				.findAny()
				.orElse(null);
	}
}