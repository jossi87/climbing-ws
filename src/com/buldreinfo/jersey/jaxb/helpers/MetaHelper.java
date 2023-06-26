package com.buldreinfo.jersey.jaxb.helpers;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Preconditions;

public class MetaHelper {
	private List<Setup> setups = new ArrayList<>();

	public MetaHelper() {
		setups.add(new Setup("buldreinfo.com", Setup.GRADE_SYSTEM.BOULDER)
				.setIdRegion(1)
				.setTitle("Buldreinfo")
				.setDescription("Bouldering in Rogaland (Stavanger, Western Norway)")
				.setLatLng(58.72, 6.62).setDefaultZoom(8));
		setups.add(new Setup("buldring.bergen-klatreklubb.no", Setup.GRADE_SYSTEM.BOULDER)
				.setIdRegion(2)
				.setTitle("Buldring i Hordaland")
				.setDescription("Bouldering in Hordaland (Bergen, Western Norway)")
				.setLatLng(60.89, 5.84).setDefaultZoom(8));
		setups.add(new Setup("buldre.forer.no", Setup.GRADE_SYSTEM.BOULDER)
				.setIdRegion(3)
				.setTitle("Buldring i Fredrikstad")
				.setDescription("Bouldering in Fredrikstad (Eastern Norway)")
				.setLatLng(59.15, 10.92).setDefaultZoom(11));
		setups.add(new Setup("brattelinjer.no", Setup.GRADE_SYSTEM.CLIMBING)
				.setIdRegion(4)
				.setTitle("Bratte Linjer")
				.setDescription("Climbing in Rogaland (Stavanger, Western Norway)")
				.setLatLng(58.72, 6.62).setDefaultZoom(8));
		setups.add(new Setup("buldring.jotunheimenfjellsport.com", Setup.GRADE_SYSTEM.BOULDER)
				.setIdRegion(5)
				.setTitle("Buldring i Jotunheimen")
				.setDescription("Bouldering in Jotunheimen (Norway)")
				.setLatLng(61.875, 9.086).setDefaultZoom(9));
		setups.add(new Setup("klatring.jotunheimenfjellsport.com", Setup.GRADE_SYSTEM.CLIMBING)
				.setIdRegion(6)
				.setTitle("Klatring i Jotunheimen")
				.setDescription("Climbing in Jotunheimen (Norway)")
				.setLatLng(61.875, 9.086).setDefaultZoom(9));
		setups.add(new Setup("buldreforer.tromsoklatring.no", Setup.GRADE_SYSTEM.BOULDER)
				.setIdRegion(7)
				.setTitle("Buldring i Tromsø")
				.setDescription("Bouldering in Tromsø (Norway)")
				.setLatLng(69.73, 18.49).setDefaultZoom(11));
		setups.add(new Setup("klatreforer.tromsoklatring.no", Setup.GRADE_SYSTEM.CLIMBING)
				.setIdRegion(8)
				.setTitle("Klatring i Tromsø")
				.setDescription("Climbing in Tromsø (Norway)")
				.setLatLng(69.77, 18.78).setDefaultZoom(9));
		setups.add(new Setup("klatreforer.narvikklatreklubb.no", Setup.GRADE_SYSTEM.CLIMBING)
				.setIdRegion(9)
				.setTitle("Klatring i Narvik")
				.setDescription("Climbing in Narvik (Norway)")
				.setLatLng(68.41312, 17.54277).setDefaultZoom(9));
		setups.add(new Setup("tau.forer.no", Setup.GRADE_SYSTEM.CLIMBING)
				.setIdRegion(10)
				.setTitle("Klatring i Fredrikstad")
				.setDescription("Climbing in Fredrikstad (Eastern Norway)")
				.setLatLng(59.15, 10.92).setDefaultZoom(11));
		setups.add(new Setup("hkl.brattelinjer.no", Setup.GRADE_SYSTEM.CLIMBING)
				.setIdRegion(11)
				.setTitle("Klatring på Haugalandet")
				.setDescription("Climbing in Haugaland (Western Norway)")
				.setLatLng(59.51196, 5.76736).setDefaultZoom(9));
		setups.add(new Setup("hkl.buldreinfo.com", Setup.GRADE_SYSTEM.BOULDER)
				.setIdRegion(12)
				.setTitle("Buldring på Haugalandet")
				.setDescription("Bouldering in Haugaland (Western Norway)")
				.setLatLng(59.67, 5.38).setDefaultZoom(8));
		setups.add(new Setup("buldring.flatangeradventure.no", Setup.GRADE_SYSTEM.BOULDER)
				.setIdRegion(13)
				.setTitle("Buldring i Trøndelag")
				.setDescription("Bouldering in Trøndelag (Norway)")
				.setLatLng(64.06897, 10.50973).setDefaultZoom(8));
		setups.add(new Setup("klatring.flatangeradventure.no", Setup.GRADE_SYSTEM.CLIMBING)
				.setIdRegion(14)
				.setTitle("Klatring i Trøndelag")
				.setDescription("Climbing in Trøndelag (Norway)")
				.setLatLng(64.06897, 10.50973).setDefaultZoom(8));
		setups.add(new Setup("buldring.narvikklatreklubb.no", Setup.GRADE_SYSTEM.BOULDER)
				.setIdRegion(15)
				.setTitle("Buldring i Narvik")
				.setDescription("Bouldering in Narvik (Norway)")
				.setLatLng(68.41312, 17.54277).setDefaultZoom(9));
		setups.add(new Setup("is.brattelinjer.no", Setup.GRADE_SYSTEM.ICE)
				.setIdRegion(16)
				.setTitle("Bratte Linjer (isklatring)")
				.setDescription("Ice climbing in Rogaland (Stavanger, Western Norway)")
				.setLatLng(58.72, 6.62).setDefaultZoom(8));
		setups.add(new Setup("is.forer.no", Setup.GRADE_SYSTEM.ICE)
				.setIdRegion(17)
				.setTitle("Is-klatring i Fredrikstad")
				.setDescription("Ice climbing in Fredrikstad (Eastern Norway)")
				.setLatLng(59.15, 10.92).setDefaultZoom(11));
		setups.add(new Setup("is.tromsoklatring.no", Setup.GRADE_SYSTEM.ICE)
				.setIdRegion(18)
				.setTitle("Is-klatring i Tromsø")
				.setDescription("Ice climbing in Tromsø (Northern Norway)")
				.setLatLng(69.73, 18.49).setDefaultZoom(11));
		setups.add(new Setup("sorlandet.brattelinjer.no", Setup.GRADE_SYSTEM.CLIMBING)
				.setIdRegion(19)
				.setTitle("Klatring på Sørlandet")
				.setDescription("Climbing in Southern Norway")
				.setLatLng(58.34991, 7.51464).setDefaultZoom(9));
		setups.add(new Setup("sorlandet.buldreinfo.com", Setup.GRADE_SYSTEM.BOULDER)
				.setIdRegion(20)
				.setTitle("Buldring på Sørlandet")
				.setDescription("Bouldering in Southern Norway")
				.setLatLng(58.34991, 7.51464).setDefaultZoom(8));
		setups.add(new Setup("salten.brattelinjer.no", Setup.GRADE_SYSTEM.CLIMBING)
				.setIdRegion(21)
				.setTitle("Klatring på Salten")
				.setDescription("Climbing in Salten (Northern Norway)")
				.setLatLng(67.38370, 15.20014).setDefaultZoom(9));
	}

	public Setup getSetup(HttpServletRequest request) {
		Preconditions.checkNotNull(request);
		Preconditions.checkNotNull(request.getServerName(), "Invalid request=" + request);
		final String serverName = request.getServerName().toLowerCase().replace("www.", "");
		return setups
				.stream()
				.filter(x -> serverName.equalsIgnoreCase(x.getDomain()))
				.findAny()
				.orElseThrow(() -> new RuntimeException("Invalid serverName=" + serverName));
	}

	public Setup getSetup(int regionId) {
		return setups
				.stream()
				.filter(x -> x.getIdRegion() == regionId)
				.findAny()
				.orElseThrow(() -> new RuntimeException("Invalid regionId=" + regionId));
	}

	public List<Setup> getSetups() {
		return setups;
	}
}