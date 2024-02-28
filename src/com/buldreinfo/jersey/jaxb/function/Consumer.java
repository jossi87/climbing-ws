package com.buldreinfo.jersey.jaxb.function;

public interface Consumer<T> {
	public void run(T input) throws Exception;
}
