package com.buldreinfo.jersey.jaxb.metadata.jsonld;

import com.buldreinfo.jersey.jaxb.metadata.beans.IMetadata;
import com.buldreinfo.jersey.jaxb.metadata.beans.Setup;
import com.buldreinfo.jersey.jaxb.model.Area;
import com.buldreinfo.jersey.jaxb.model.Problem;
import com.buldreinfo.jersey.jaxb.model.Sector;

public class JsonLdCreator {
	public static JsonLd getJsonLd(Setup setup, IMetadata m) {
		if (m == null) {
			return null;
		}
		if (m instanceof Area) {
			Area a = (Area)m;
			JsonLd res = new JsonLd();
			res.getItemListElement().add(new ItemListElement(1, new Item(setup.getUrl("/browse"), "Browse")));
			res.getItemListElement().add(new ItemListElement(2, new Item(setup.getUrl("/area/" + a.getId()), a.getName())));
			return res;
		}
		else if (m instanceof Problem) {
			Problem p = (Problem)m;
			JsonLd res = new JsonLd();
			res.getItemListElement().add(new ItemListElement(1, new Item(setup.getUrl("/browse"), "Browse")));
			res.getItemListElement().add(new ItemListElement(2, new Item(setup.getUrl("/area/" + p.getAreaId()), p.getAreaName())));
			res.getItemListElement().add(new ItemListElement(2, new Item(setup.getUrl("/sector/" + p.getSectorId()), p.getSectorName())));
			res.getItemListElement().add(new ItemListElement(2, new Item(setup.getUrl("/problem/" + p.getId()), p.getName())));
			return res;
		}
		else if (m instanceof Sector) {
			Sector s = (Sector)m;
			JsonLd res = new JsonLd();
			res.getItemListElement().add(new ItemListElement(1, new Item(setup.getUrl("/browse"), "Browse")));
			res.getItemListElement().add(new ItemListElement(2, new Item(setup.getUrl("/area/" + s.getAreaId()), s.getAreaName())));
			res.getItemListElement().add(new ItemListElement(2, new Item(setup.getUrl("/sector/" + s.getId()), s.getName())));
			return res;
		}
		else {
			throw new RuntimeException("Invalid m=" + m);
		}
	}
}