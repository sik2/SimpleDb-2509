package com.back.simpleDb;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface SqlFunction<T, R> {
    R apply(T t) throws SQLException;
}