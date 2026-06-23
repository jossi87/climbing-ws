package com.buldreinfo.dao;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.buldreinfo.infrastructure.ClimbingTransactionManager;

public abstract class BaseRepository {
	protected final ClimbingTransactionManager txManager;
	public static final Executor executor = Executors.newThreadPerTaskExecutor(java.lang.Thread.ofVirtual().name("climbing-repo-", 0).factory());

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