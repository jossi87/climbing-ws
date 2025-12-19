package com.buldreinfo.jersey.jaxb.function;

import java.util.Optional;

import com.buldreinfo.jersey.jaxb.beans.Setup;
import com.buldreinfo.jersey.jaxb.db.Dao;

@FunctionalInterface
public interface FunctionDsUser<T, R> {
    R get(Dao dao, T dataSource, Setup setup, Optional<Integer> authUserId) throws Exception;
}