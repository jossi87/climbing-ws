package com.buldreinfo.infrastructure;

import com.buldreinfo.config.BuldreinfoConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
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
        
        return new HikariDataSource(hikariConfig);
    }
}