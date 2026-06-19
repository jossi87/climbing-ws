package com.buldreinfo.jersey.jaxb.dao;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;

import com.buldreinfo.jersey.jaxb.infrastructure.TransactionManager;

public abstract class BaseRepository {
	protected final TransactionManager txManager;

	protected BaseRepository(TransactionManager txManager) {
		this.txManager = txManager;
	}

	protected <T> T executeConcurrentTask(Callable<T> task) {
		try {
			return txManager.executeInTransaction(task);
		} catch (Exception e) {
			throw new CompletionException(e);
		}
	}
}