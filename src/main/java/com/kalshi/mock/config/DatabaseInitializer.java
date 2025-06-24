package com.kalshi.mock.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements ApplicationRunner {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        System.out.println("Initializing SQLite database schema...");
        
        // Create orders table
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS orders (
                id TEXT PRIMARY KEY,
                client_order_id TEXT,
                user_id TEXT NOT NULL,
                side TEXT NOT NULL,
                symbol TEXT NOT NULL,
                order_type TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                filled_quantity INTEGER DEFAULT 0,
                remaining_quantity INTEGER NOT NULL,
                price INTEGER,
                avg_fill_price INTEGER,
                status TEXT NOT NULL,
                time_in_force TEXT,
                created_time BIGINT NOT NULL,
                updated_time BIGINT NOT NULL,
                expiration_time BIGINT,
                action TEXT NOT NULL
            )
        """);
        
        // Create index on user_id for faster queries
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_orders_symbol ON orders(symbol)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status)");
        
        // Create fills table
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS fills (
                id TEXT PRIMARY KEY,
                order_id TEXT NOT NULL,
                user_id TEXT NOT NULL,
                market_id TEXT NOT NULL,
                market_ticker TEXT NOT NULL,
                side TEXT NOT NULL,
                price INTEGER NOT NULL,
                count INTEGER NOT NULL,
                is_taker BOOLEAN NOT NULL,
                created_time BIGINT NOT NULL,
                trade_id TEXT NOT NULL
            )
        """);
        
        // Create indexes for fills
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_fills_user_id ON fills(user_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_fills_order_id ON fills(order_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_fills_market_ticker ON fills(market_ticker)");
        
        // Create positions table
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS positions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT NOT NULL,
                market_id TEXT NOT NULL,
                market_ticker TEXT NOT NULL,
                quantity INTEGER NOT NULL DEFAULT 0,
                avg_price INTEGER NOT NULL DEFAULT 0,
                side TEXT NOT NULL,
                realized_pnl INTEGER NOT NULL DEFAULT 0,
                total_cost INTEGER NOT NULL DEFAULT 0,
                updated_time BIGINT NOT NULL,
                UNIQUE(user_id, market_ticker, side)
            )
        """);
        
        // Create indexes for positions
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_positions_user_id ON positions(user_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_positions_market_ticker ON positions(market_ticker)");
        
        // Create trades table for order book matching
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS trades (
                id TEXT PRIMARY KEY,
                market_ticker TEXT NOT NULL,
                aggressive_order_id TEXT NOT NULL,
                passive_order_id TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                price INTEGER NOT NULL,
                created_time BIGINT NOT NULL
            )
        """);
        
        // Create index for trades
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_trades_market_ticker ON trades(market_ticker)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_trades_created_time ON trades(created_time)");
        
        System.out.println("Database schema initialized successfully.");
    }
}