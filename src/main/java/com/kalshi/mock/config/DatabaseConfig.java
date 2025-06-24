package com.kalshi.mock.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.File;

@Configuration
public class DatabaseConfig {
    
    @Value("${kalshi.database.path:kalshi-mock.db}")
    private String databasePath;
    
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.sqlite.JDBC");
        
        // Create database file in the current directory
        File dbFile = new File(databasePath);
        String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        
        dataSource.setUrl(jdbcUrl);
        
        System.out.println("SQLite database location: " + dbFile.getAbsolutePath());
        
        return dataSource;
    }
    
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}