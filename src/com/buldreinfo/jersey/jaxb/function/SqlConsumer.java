package com.buldreinfo.jersey.jaxb.function;

import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * A functional interface that represents an void function that accepts an input and may throw an exception.
 */
public interface SqlConsumer<T> {
	/**
	 * Apply the current operation using the provided input.
	 * @param input the input.
	 * @throws SQLException A generic SQL error.
	 */
	public void apply(T input) throws Exception;
	
	/**
	 * Retrieve a consumer that calls the given {@link SqlConsumer} and returns its value.
	 * <p/>
	 * Any {@link Exception} will be wrapped as a {@link WrappedException}.
	 * @param consumer the consumer to convert.
	 * @return The wrapped consumer.
	 */
	public static <T> Consumer<T> wrap(SqlConsumer<T> consumer) {
		return input -> {
			try {
				consumer.apply(input);
			} catch (Exception e) {
				throw new WrappedException(e);
			}
		};
	}
}
