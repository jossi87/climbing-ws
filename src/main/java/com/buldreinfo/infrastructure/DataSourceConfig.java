package com.buldreinfo.infrastructure;

import com.buldreinfo.config.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(AppConfig appConfig) {
        HikariConfig hikariConfig = new HikariConfig();
        var db = appConfig.db();
        hikariConfig.setJdbcUrl("jdbc:mysql://%s/%s".formatted(db.hostname(), db.database()));
        hikariConfig.setUsername(db.username());
        hikariConfig.setPassword(db.password());
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
        return new HikariDataSource(hikariConfig);
    }
}