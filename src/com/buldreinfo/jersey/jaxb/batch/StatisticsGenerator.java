package com.buldreinfo.jersey.jaxb.batch;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.buldreinfo.jersey.jaxb.model.app.Area;
import com.buldreinfo.jersey.jaxb.model.app.Problem;
import com.buldreinfo.jersey.jaxb.model.app.Region;
import com.buldreinfo.jersey.jaxb.model.app.Sector;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharSource;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class StatisticsGenerator {
	public static void main(String[] args) throws Exception {
		new StatisticsGenerator();
	}

	public StatisticsGenerator() throws Exception {
		TreeMap<Integer, Integer> decadeFaMap = new TreeMap<>();
		Map<String, StatisticsUser> userLookup = new HashMap<>();
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpGet request = new HttpGet("https://brattelinjer.no/com.buldreinfo.jersey.jaxb/v1/regions?uniqueId=419920f881c6cc94&climbingNotBouldering=true");
			try (CloseableHttpResponse response = httpClient.execute(request)) {
				Preconditions.checkArgument(response.getStatusLine().getStatusCode() == 200, response.getStatusLine().getStatusCode() + ": " + response.getStatusLine().getReasonPhrase());
				HttpEntity entity = response.getEntity();
				byte[] buffer = ByteStreams.toByteArray(entity.getContent());
				try (Reader reader = CharSource.wrap(new String(buffer, "UTF-8")).openStream()) {
					Gson gson = new Gson();
					List<Region> regions = gson.fromJson(reader, new TypeToken<ArrayList<Region>>(){}.getType());
					Region r = regions.stream().filter(x -> x.getId() == 4).findAny().get();
					for (Area a : r.getAreas()) {
						for (Sector s : a.getSectors()) {
							for (Problem p : s.getProblems()) {
								if (p.getGrade() > 0) {
									int decade = getFaDecade(p);
									String users = decade != 0? p.getFa().substring(10).trim() : p.getFa();
									if (decade == 0) {
										decade = getAverageFaDecade(s);
									}
									int num = decadeFaMap.getOrDefault(decade, 0);
									decadeFaMap.put(decade, num+1);
									if (users != null) {
										for (String user : users.split(",")) {
											user = user.trim();
											StatisticsUser u = userLookup.get(user);
											if (u == null) {
												u = new StatisticsUser(user);
												userLookup.put(user, u);
											}
											u.addFa(decade);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		// Print decade
		for (int decade : decadeFaMap.keySet()) {
			if (decade != 202) {
				System.err.println(decade*10 + " (" + decadeFaMap.get(decade) + " new routes)");
				List<StatisticsUser> users = userLookup.values()
						.stream()
						.sorted((x1, x2) -> Integer.compare(x2.getNumOnDecade(decade), x1.getNumOnDecade(decade)))
						.collect(Collectors.toList());
				for (int i = 0; i < 5; i++) {
					StatisticsUser u = users.get(i);
					System.err.println("- " + u.getNumOnDecade(decade) + " - " + u.getName());
				}
			}
		}
		System.err.println("####");
		List<StatisticsUser> orderedUsers = userLookup.values()
				.stream()
				.sorted(Comparator.comparingInt(StatisticsUser::getTotal).reversed())
				.collect(Collectors.toList());
		for (int i = 0; i < 20; i++) {
			StatisticsUser u = orderedUsers.get(i);
			System.err.println(u.getTotal() + " - " + u.getName());
		}
	}

	private int getAverageFaDecade(Sector s) {
		List<Integer> decades = new ArrayList<>();
		for (Problem p : s.getProblems()) {
			int decade = getFaDecade(p);
			if (decade > 0) {
				decades.add(decade);
			}
		}
		int avg = (int)Math.round(decades.stream().mapToInt(val -> val).average().orElse(0));
		if (avg == 0) {
			switch (s.getId()) {
			case 2956: return 201; // Dale - Dalsvågen
			case 2873: return 199; // Bersagel - Storesva
			case 2878: return 200; // Ålgård - Ålgård
			case 3536: return 199; // Jøssingfjord - Conanveggen
			case 2909: return 201; // Kalvatødne - Kalvatødne
			case 2910: return 201; // Litla Hetland - Litla Hetland
			case 3009: return 201; // Svihus - Svihusveggen
			case 2984: return 199; // Nordland - Andre etasje
			case 2918: return 200; // Oltedal Steinene - Svenskesteinen
			case 2919: return 199; // Oltedal Steinene - Søppelsteinen
			case 2921: return 200; // Oltedal Steinene - Tørrsteinen
			case 2923: return 199; // Oltedal Steinene - Huginsteinen
			case 2925: return 199; // Oltedal Steinene - Oppvarmingssteinen
			case 2954: return 201; // Skogsveggen - Venstre
			case 2955: return 201; // Skogsveggen - Høyre
			case 2929: return 201; // Krusafjell - Andre etasje
			case 2931: return 200; // Spinneriveggen - Venstresiden
			case 2932: return 200; // Spinneriveggen - Svaet
			case 2934: return 200; // Spinneriveggen - Høyreveggen
			case 2962: return 201; // Bogafjell - Bogafjell
			case 3003: return 200; // Trellskår - Høyreveggen
			case 2966: return 201; // Lauvås - Hammeren (østvendt)
			case 2967: return 201; // Lauvås - Vestveggen
			case 2968: return 201; // Lauvås - Sør-øst-veggen
			case 2958: return 201; // Hesteveggen - Høyreveggen
			case 3261: return 200; // Urdviki - Stein nede ved fossen
			case 3524: return 200; // Lysebotn - Lysebotn
			default: throw new RuntimeException("No decade for " + s.getName() + " (id=" + s.getId() + ")");
			}
		}
		return avg;
	}

	private int getFaDecade(Problem p) {
		if (p.getFa() != null) {
			if (p.getFa().startsWith("19") || p.getFa().startsWith("20")) {
				return Integer.parseInt(p.getFa().substring(0, 3));
			}
		}
		return 0;
	}
}