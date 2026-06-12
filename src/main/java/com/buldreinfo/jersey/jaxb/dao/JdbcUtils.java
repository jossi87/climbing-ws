package com.buldreinfo.jersey.jaxb.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public final class JdbcUtils {
    private JdbcUtils() {
    }

    public static void setNullablePositiveDouble(PreparedStatement ps, int parameterIndex, double value) throws SQLException {
        if (value > 0) {
            ps.setDouble(parameterIndex, value);
        } else {
            ps.setNull(parameterIndex, Types.DOUBLE);
        }
    }

    public static void setNullablePositiveInteger(PreparedStatement ps, int parameterIndex, int value) throws SQLException {
        if (value > 0) {
            ps.setInt(parameterIndex, value);
        } else {
            ps.setNull(parameterIndex, Types.INTEGER);
        }
    }
}