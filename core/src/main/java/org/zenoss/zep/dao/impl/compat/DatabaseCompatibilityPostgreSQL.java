/*
 * Copyright (C) 2011, Zenoss Inc.  All Rights Reserved.
 */
package org.zenoss.zep.dao.impl.compat;

import org.springframework.jdbc.core.SqlParameterValue;
import org.zenoss.utils.dao.RangePartitioner;
import org.zenoss.utils.dao.impl.PostgreSqlRangePartitioner;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Database compatibility interface for PostgreSQL support.
 */
public class DatabaseCompatibilityPostgreSQL implements DatabaseCompatibility {

    private final TypeConverter<String> uuidConverter = new UUIDConverterPostgreSQL();

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.POSTGRESQL;
    }

    @Override
    public TypeConverter<Long> getTimestampConverter() {
        return new TypeConverter<Long>() {
            @Override
            public Long fromDatabaseType(ResultSet rs, String columnName) throws SQLException {
                Timestamp ts = rs.getTimestamp(columnName);
                return (ts != null) ? ts.getTime() : null;
            }

            @Override
            public Object toDatabaseType(Long timestampInMillis) {
                if (timestampInMillis == null) {
                    return null;
                }
                return new Timestamp(timestampInMillis);
            }
        };
    }

    @Override
    public TypeConverter<String> getUUIDConverter() {
        return this.uuidConverter;
    }

    @Override
    public RangePartitioner getRangePartitioner(DataSource ds,
            String databaseName, String tableName, String columnName,
            long duration, TimeUnit unit) {
        return new PostgreSqlRangePartitioner(ds, databaseName, tableName,
                columnName, duration, unit);
    }
}