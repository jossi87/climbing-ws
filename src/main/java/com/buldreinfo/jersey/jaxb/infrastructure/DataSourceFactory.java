package com.buldreinfo.jersey.jaxb.infrastructure;

import javax.sql.DataSource;

import org.glassfish.hk2.api.Factory;

import com.buldreinfo.jersey.jaxb.config.BuldreinfoConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DataSourceFactory implements Factory<DataSource> {
    private final HikariDataSource ds;

    public DataSourceFactory() {
        BuldreinfoConfig config = BuldreinfoConfig.getConfig();
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(String.format("jdbc:mysql://%s/%s", 
                config.getProperty(BuldreinfoConfig.PROPERTY_KEY_DB_HOSTNAME),
                config.getProperty(BuldreinfoConfig.PROPERTY_KEY_DB_DATABASE)));
        hikariConfig.setUsername(config.getProperty(BuldreinfoConfig.PROPERTY_KEY_DB_USERNAME));
        hikariConfig.setPassword(config.getProperty(BuldreinfoConfig.PROPERTY_KEY_DB_PASSWORD));
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setAutoCommit(false);
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.setMaximumPoolSize(25);
        hikariConfig.setMinimumIdle(10);
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.setConnectionInitSql("SET SESSION group_concat_max_len = 1000000");
        this.ds = new HikariDataSource(hikariConfig);
    }

    @Override
    public DataSource provide() {
        return ds;
    }

    @Override
    public void dispose(DataSource instance) {
        if (instance instanceof HikariDataSource hds) {
            hds.close();
        }
    }
}