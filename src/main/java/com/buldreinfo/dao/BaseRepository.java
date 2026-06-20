package com.buldreinfo.dao;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;

import com.buldreinfo.infrastructure.ClimbingTransactionManager;

public abstract class BaseRepository {
	protected final ClimbingTransactionManager txManager;

	protected BaseRepository(ClimbingTransactionManager txManager) {
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