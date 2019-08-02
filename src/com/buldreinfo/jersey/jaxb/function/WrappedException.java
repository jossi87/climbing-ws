package com.buldreinfo.jersey.jaxb.function;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

/**
 * Represents a wrapped exception.
 */
public class WrappedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * Construct a new wrapped exception.
	 * @param cause the cause.
	 */
	public WrappedException(Throwable cause) {
		super(Preconditions.checkNotNull(cause, "cause cannot be NULL"));
	}
	
	/**
	 * Re-throw the current cause, either as the given checked exception, or as a wrapped RuntimeException.
	 * @param checkedException the checked exception.
	 * @return Will never return.
	 * @throws T The checked exception type.
	 */
	public <T extends Exception> T rethrow(Class<T> checkedException) throws T {
		Throwable cause = getCause();
		Throwables.throwIfInstanceOf(cause, checkedException);
		throw new RuntimeException(cause);
	}
}
