package com.buldreinfo.jersey.jaxb.batch;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.buldreinfo.jersey.jaxb.model.app.Area;
import com.buldreinfo.jersey.jaxb.model.app.Region;
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
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpGet request = new HttpGet("https://brattelinjer.no/com.buldreinfo.jersey.jaxb/v1/regions?uniqueId=419920f881c6cc94&climbingNotBouldering=true");
			try (CloseableHttpResponse response = httpClient.execute(request)) {
				Preconditions.checkArgument(response.getStatusLine().getStatusCode() == 200, response.getStatusLine().getStatusCode() + ": " + response.getStatusLine().getReasonPhrase());
				HttpEntity entity = response.getEntity();
				byte[] buffer = ByteStreams.toByteArray(entity.getContent());
				try (Reader reader = CharSource.wrap(new String(buffer)).openStream()) {
					Gson gson = new Gson();
					List<Region> regions = gson.fromJson(reader, new TypeToken<ArrayList<Region>>(){}.getType());
					Region r = regions.stream().filter(x -> x.getId() == 4).findAny().get();
					System.err.println(r.getName());
					System.err.println(r.getAreas());
					for (Area a : r.getAreas()) {
						System.err.println(a);
					}
				}
			}
		}
	}

}
