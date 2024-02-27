package com.buldreinfo.jersey.jaxb.server;

public interface Consumer<T> {
	public void run(T input) throws Exception;
}
