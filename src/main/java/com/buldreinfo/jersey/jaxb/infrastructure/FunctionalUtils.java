package com.buldreinfo.jersey.jaxb.infrastructure;

import java.util.function.Supplier;

public class FunctionalUtils {
	@FunctionalInterface
	public interface ThrowingSupplier<T> {
		T get() throws Exception;
	}

	public static <T> Supplier<T> transactional(TransactionManager txManager, ThrowingSupplier<T> supplier) {
		return () -> {
			try {
				return txManager.executeInTransaction(supplier::get);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}
}