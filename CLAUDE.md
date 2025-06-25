# Mock Kalshi Trading System - Technical Overview

## System Architecture

This is a mock implementation of Kalshi's binary options trading platform, built with Spring Boot and React. The system simulates a real-time prediction market with FIX protocol support and WebSocket-based market data distribution.

### Core Components

1. **Backend (Spring Boot)**
   - REST API for trading operations
   - FIX 4.4 protocol server for institutional trading
   - WebSocket/STOMP for real-time market data
   - PostgreSQL for persistence
   - In-memory order book management

2. **Frontend (React)**
   - Real-time market data display
   - Order placement interface
   - Portfolio management
   - WebSocket client for live updates

## Binary Options Market Dynamics

### Market Structure
- **Binary Markets**: Each market represents a yes/no question about a future event
- **Price Range**: 0-100 cents (representing 0-100% probability)
- **Settlement**: Markets settle at either 0 or 100 based on the outcome

### YES/NO Order Book Mechanics

**Critical Concept**: The system maintains a single unified order book containing only YES orders. NO orders are automatically converted to equivalent YES orders:

```
NO Buy @ 40 → YES Sell @ 60
NO Sell @ 40 → YES Buy @ 60
```

This conversion follows the fundamental relationship: `YES Price + NO Price = 100`

#### Order Conversion Rules
1. **NO Buy Orders**: 
   - Converted to YES Sell orders
   - Price = 100 - NO price
   - Example: Buy NO at 30 → Sell YES at 70

2. **NO Sell Orders**:
   - Converted to YES Buy orders  
   - Price = 100 - NO price
   - Example: Sell NO at 30 → Buy YES at 70

3. **Market Display**:
   - YES side shows actual YES orders
   - NO side shows inverted YES orders
   - Best YES Bid at 45 → Best NO Ask at 55
   - Best YES Ask at 55 → Best NO Bid at 45

## Real-Time Broadcasting Architecture

### Order Book Updates
The system uses an event-driven architecture with broadcast notifications:

1. **OrderBookEventPublisher**: Central event hub
   - Publishes events for all order book changes
   - Supports multiple listeners
   - Events include: OrderPlaced, OrderCanceled, OrderMatched, OrderBookUpdated

2. **WebSocket Broadcasting**:
   - All order book changes are broadcast via STOMP WebSocket
   - Topic structure: `/topic/orderbook/{marketTicker}`
   - Clients receive full order book snapshots on each update
   - No incremental updates - always full book state

3. **Market Data Updates**:
   - Price changes broadcast to `/topic/market/{marketTicker}`
   - Includes best bid/ask and last trade price
   - Updates triggered by order matches

### WebSocket Message Flow
```
Order Placed → OrderBookService → OrderBookEventPublisher → WebSocketPublisher → All Connected Clients
```

## Key Services

### OrderBookService
- Manages in-memory order books using ConcurrentOrderBook
- Handles order placement, cancellation, and matching
- Converts NO orders to YES orders automatically
- Persists order state changes to database

### MarketDataService
- Aggregates order book data for market display
- Calculates best bid/ask prices
- Tracks market statistics (volume, open interest)

### PersistenceService
- Handles all database operations
- Maintains order history
- Tracks user positions and portfolios

## Database Schema

Key tables:
- `markets`: Market definitions and current prices
- `orders`: All orders with status tracking
- `trades`: Executed trades history
- `positions`: User positions by market
- `users`: User accounts and balances

## Business Rules

For detailed business rules including:
- Order matching logic
- Position limits
- Settlement procedures
- Fee structures

See the separate README file.

## Development Notes

### Running the System
1. Start PostgreSQL (via Docker)
2. Run Spring Boot backend: `mvn spring-boot:run`
3. Run React frontend: `npm start`

### Test Data
- SQL scripts in `src/main/resources/data.sql` create test markets
- MockMarketsInitializer loads markets on startup
- Initial order books populated with market maker orders

### Key Configuration
- Backend port: 9090
- FIX port: 9878
- WebSocket endpoint: `/ws`
- Database: PostgreSQL on localhost:5432

## Testing Checklist

When making changes to the order book system:
1. Verify NO→YES order conversion
2. Check WebSocket broadcasts are sent
3. Ensure database persistence
4. Validate price calculations (YES + NO = 100)
5. Test order matching logic
6. Verify market data updates

## Common Issues

1. **Markets not showing**: Check if data.sql has run and markets table is populated
2. **WebSocket disconnections**: Ensure CORS is configured properly
3. **Order book not updating**: Verify OrderBookEventPublisher has registered listeners
4. **Database connection**: Check PostgreSQL credentials in application.properties