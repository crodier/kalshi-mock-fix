# Persistence and Position Tracking Implementation

## Overview
We've successfully implemented a complete persistence layer using SQLite and a concurrent position tracking system for the mock Kalshi trading platform.

## Key Components

### 1. SQLite Persistence Layer
- **Database**: SQLite with file-based storage (configurable via `kalshi.database.path`)
- **Tables**:
  - `orders`: Stores all order details with full history
  - `fills`: Records all trade executions
  - `positions`: Tracks user positions by market and side
  - `trades`: Public trade tape

### 2. PersistenceService
- Thread-safe database operations using Spring's `@Transactional`
- Handles all CRUD operations for orders, fills, positions, and trades
- Implements proper NULL handling for optional fields
- Efficient querying with indexes on key fields

### 3. PositionsService
- Concurrent position management with `ReadWriteLock` for thread safety
- Tracks both long and short positions
- Handles position lifecycle:
  - Opening new positions (long or short)
  - Increasing existing positions
  - Reducing positions (partial close)
  - Flipping positions (long to short or vice versa)
  - Closing positions completely
- Calculates unrealized P&L and portfolio value

### 4. Position Tracking Logic
- **Buy orders**: Increase position (positive quantity)
- **Sell orders**: Decrease position (negative quantity)
- **Short positions**: Represented as negative quantities
- **Average price calculation**:
  - When increasing: Weighted average of old and new
  - When reducing: Maintains existing average price
  - When flipping: New position at current price

## Database Schema

### Orders Table
```sql
CREATE TABLE orders (
    id TEXT PRIMARY KEY,
    client_order_id TEXT,
    user_id TEXT NOT NULL,
    side TEXT NOT NULL,          -- 'yes' or 'no'
    symbol TEXT NOT NULL,
    order_type TEXT NOT NULL,
    quantity INTEGER NOT NULL,
    filled_quantity INTEGER DEFAULT 0,
    remaining_quantity INTEGER NOT NULL,
    price INTEGER,
    avg_fill_price INTEGER,
    status TEXT NOT NULL,         -- 'open', 'filled', 'partially_filled', 'canceled'
    time_in_force TEXT,
    created_time BIGINT NOT NULL,
    updated_time BIGINT NOT NULL,
    expiration_time BIGINT,
    action TEXT NOT NULL          -- 'buy' or 'sell'
)
```

### Positions Table
```sql
CREATE TABLE positions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id TEXT NOT NULL,
    market_id TEXT NOT NULL,
    market_ticker TEXT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,  -- Negative for short positions
    avg_price INTEGER NOT NULL DEFAULT 0,
    side TEXT NOT NULL,                   -- 'yes' or 'no'
    realized_pnl INTEGER NOT NULL DEFAULT 0,
    total_cost INTEGER NOT NULL DEFAULT 0,
    updated_time BIGINT NOT NULL,
    UNIQUE(user_id, market_ticker, side)
)
```

## Test Coverage

### Unit Tests
1. **PositionsServiceTest** (9 tests)
   - Buy/sell fill processing
   - Position calculations
   - P&L calculations
   - Portfolio value calculations

2. **PersistenceServicePositionTest** (10 tests)
   - Creating long/short positions
   - Increasing/reducing positions
   - Flipping positions
   - Multiple markets and sides

### Integration Tests
1. **OrderControllerIntegrationTest** (8 tests)
   - Order creation and cancellation
   - Order retrieval
   - Batch orders
   - Fill generation

2. **PositionTrackingIntegrationTest** (5 tests)
   - Position creation from matched orders
   - Short position tracking
   - Position aggregation
   - NO order conversion effects

## Example Usage

### Creating Orders and Tracking Positions
```bash
# Create a buy order
curl -X POST http://localhost:9090/trade-api/v2/portfolio/orders \
  -H "Content-Type: application/json" \
  -d '{
    "market_ticker": "TRUMPWIN-24NOV05",
    "side": "yes",
    "action": "buy",
    "type": "limit",
    "count": 100,
    "price": 65
  }'

# Get current positions
curl http://localhost:9090/trade-api/v2/portfolio/positions

# Response shows position after trade:
{
  "positions": [{
    "market_id": "TRUMPWIN-24NOV05",
    "market_ticker": "TRUMPWIN-24NOV05",
    "quantity": 100,        // Long position
    "avg_price": 65,
    "side": "yes",
    "realized_pnl": 0,
    "total_cost": 6500
  }]
}
```

### Short Position Example
```bash
# Sell without owning (going short)
curl -X POST http://localhost:9090/trade-api/v2/portfolio/orders \
  -H "Content-Type: application/json" \
  -d '{
    "market_ticker": "BTCZ-23DEC31-B50000",
    "side": "yes",
    "action": "sell",
    "type": "limit",
    "count": 50,
    "price": 80
  }'

# Position will show negative quantity:
{
  "positions": [{
    "market_ticker": "BTCZ-23DEC31-B50000",
    "quantity": -50,        // Short position
    "avg_price": 80,
    "side": "yes",
    "realized_pnl": 0,
    "total_cost": 4000
  }]
}
```

## Key Features

1. **Persistent Storage**: All data survives server restarts
2. **Thread-Safe**: Concurrent access handled properly
3. **Position Netting**: Automatically nets long/short positions
4. **Zero Position Filtering**: Zero quantity positions excluded from API responses
5. **FIFO Matching**: Orders matched in time priority order
6. **Real-time Updates**: Positions updated immediately after trades

## Configuration

Set database location in `application.properties`:
```properties
# SQLite Database Configuration
kalshi.database.path=kalshi-mock.db  # Default: creates file in current directory

# Use in-memory database for testing
kalshi.database.path=:memory:
```

## Next Steps

1. **Add WebSocket support** for real-time position updates
2. **Implement realized P&L tracking** when positions are closed
3. **Add position history** for audit trail
4. **Implement margin/collateral tracking**
5. **Add position limits and risk controls**