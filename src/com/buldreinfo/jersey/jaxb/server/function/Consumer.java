package com.buldreinfo.jersey.jaxb.server.function;

public interface Consumer<T> {
	public void run(T input) throws Exception;
}
