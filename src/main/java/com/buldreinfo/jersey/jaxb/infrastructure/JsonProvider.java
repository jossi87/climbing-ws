package com.buldreinfo.jersey.jaxb.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public final class JsonProvider implements MessageBodyWriter<Object>, MessageBodyReader<Object> {
	private final Gson gson = new GsonBuilder().create();

	@SuppressWarnings("unused")
	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return mediaType == null || mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
	}

	@SuppressWarnings("unused")
	@Override
	public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
		InputStreamReader streamReader = new InputStreamReader(entityStream, StandardCharsets.UTF_8);
		Type jsonType = type.equals(genericType) ? type : genericType;
		return gson.fromJson(streamReader, jsonType);
	}

	@SuppressWarnings("unused")
	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return mediaType == null || mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
	}

	@SuppressWarnings("unused")
	@Override
	public long getSize(Object object, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@SuppressWarnings("unused")
	@Override
	public void writeTo(Object object, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
		OutputStreamWriter writer = new OutputStreamWriter(entityStream, StandardCharsets.UTF_8);
		Type jsonType = type.equals(genericType) ? type : genericType;
		gson.toJson(object, jsonType, writer);
		writer.flush();
	}
}