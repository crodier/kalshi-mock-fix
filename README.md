# Mock Kalshi FIX Trading Platform

## Overview

A comprehensive mock implementation of Kalshi's trading platform featuring both FIX protocol and REST API support. This project provides a complete catalog system (Series, Events, Markets), order management, portfolio tracking, and market data endpoints compatible with Kalshi's API specification.

## 🚀 Quick Start

### Using Docker (Recommended)

```bash
# Start PostgreSQL and the application
docker-compose up

# Access Swagger UI
open http://localhost:9090/swagger-ui/index.html
```

### Local Development

```bash
# Start PostgreSQL
docker-compose up -d postgres

# Build and run the application
mvn clean package
java -jar target/mock-kalshi-fix-*.jar

# Or use Spring Boot Maven plugin
mvn spring-boot:run
```

## 📋 Features

### REST API
- **Full Kalshi API Compatibility**: Implements Kalshi's trading API v2 specification
- **Swagger UI**: Interactive API documentation at `/swagger-ui/index.html`
- **OpenAPI 3.0**: Complete API specification at `/v3/api-docs`

### Catalog System
- **Series Management**: Create and manage market series (e.g., EURUSD, elections)
- **Events**: Organize markets within series by time periods or categories
- **Markets**: Binary options markets with full order book support

### Trading Engine
- **Concurrent Order Book**: Thread-safe implementation with YES/NO market dynamics
- **NO/YES Conversion**: All NO orders internally converted to YES equivalents
- **FIFO Matching**: Time-priority order matching
- **Cross Detection**: Identifies self-crosses and arbitrage opportunities

### Portfolio Management
- **Orders**: Create, view, and cancel orders
- **Positions**: Track long and short positions with P&L
- **Fills**: Complete trade execution history
- **Balance**: Account balance tracking

### FIX Protocol
- **QuickFIX/J Integration**: FIX server on port 9878
- **FIXT.1.1 Support**: Modern FIX protocol implementation

## 🛠️ Technology Stack

- **Java 17+**
- **Spring Boot 3.5.3**
- **PostgreSQL**: Production database
- **Docker & Docker Compose**: Container orchestration
- **QuickFIX/J**: FIX protocol implementation
- **SpringDoc OpenAPI**: API documentation

## 📁 Project Structure

```
mock-kalshi-fix/
├── src/main/java/com/kalshi/mock/
│   ├── catalog/          # Series, Events, Markets management
│   ├── controller/       # REST API endpoints
│   ├── service/          # Business logic
│   ├── config/           # Spring configuration
│   └── dto/              # Data transfer objects
├── src/main/resources/
│   ├── application.yml   # Spring configuration
│   └── db/schema.sql     # PostgreSQL schema
├── scripts/              # Shell scripts for testing
├── src/test/http/        # IntelliJ HTTP test files
└── docker-compose.yml    # Docker configuration
```

## 🔌 API Endpoints

### Catalog Endpoints
- `GET/POST /trade-api/v2/series` - Manage series
- `GET/POST /trade-api/v2/events` - Manage events  
- `GET/POST /trade-api/v2/markets` - Manage markets

### Trading Endpoints
- `POST /trade-api/v2/portfolio/orders` - Create orders
- `GET /trade-api/v2/portfolio/orders` - View orders
- `DELETE /trade-api/v2/portfolio/orders/{id}` - Cancel orders
- `GET /trade-api/v2/portfolio/positions` - View positions
- `GET /trade-api/v2/portfolio/fills` - View trade history

### Market Data
- `GET /trade-api/v2/markets/{ticker}/orderbook` - Order book snapshot
- `GET /trade-api/v2/markets/trades` - Recent trades
- `GET /trade-api/v2/series/{series}/markets/{ticker}/candlesticks` - OHLC data

## 🧪 Testing

### Using Shell Scripts
```bash
# Get all series
./scripts/get_series.sh

# Create an order
./scripts/create_order.sh TRUMPWIN-24NOV05 yes buy 100 65
```

### Using IntelliJ HTTP Files
Open any `.http` file in `src/test/http/` with IntelliJ IDEA for interactive testing.

### Using cURL
```bash
# Create a market series
curl -X POST http://localhost:9090/trade-api/v2/series \
  -H "Content-Type: application/json" \
  -d '{
    "ticker": "EURUSD",
    "frequency": "daily",
    "title": "EUR/USD Exchange Rate",
    "category": "forex"
  }'
```

## 🔧 Configuration

### Application Properties
Configure in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/kalshi_mock
    username: kalshi
    password: kalshi_dev_password
    
server:
  port: 9090
```

### Environment Variables
- `SPRING_PROFILES_ACTIVE`: Set to `docker` when running in Docker
- `SPRING_DATASOURCE_URL`: Override database connection

## 🐳 Docker Support

### Build and Run
```bash
# Build the application
mvn clean package

# Start everything
docker-compose up

# Stop everything
docker-compose down

# Clean up volumes
docker-compose down -v
```

### Services
- **postgres**: PostgreSQL database on port 5432
- **kalshi-app**: Spring Boot application on port 9090

## 📖 Documentation

- [REST API Guide](REST_API_GUIDE.md) - Detailed API documentation
- [Implementation Summary](IMPLEMENTATION_SUMMARY.md) - Technical implementation details
- [Persistence & Positions](PERSISTENCE_AND_POSITIONS.md) - Database and position tracking
- [cURL Examples](CURL_EXAMPLES.md) - Example API calls

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📝 License

This is a mock implementation for educational and testing purposes only. Not affiliated with Kalshi.