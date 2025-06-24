package com.kalshi.mock.controller;

import com.fbg.api.rest.*;
import com.kalshi.mock.service.MarketService;
import com.kalshi.mock.service.OrderBookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trade-api/v2")
@Tag(name = "Markets", description = "Market data and information endpoints")
public class MarketController {
    
    @Autowired
    private MarketService marketService;
    
    @Autowired
    private OrderBookService orderBookService;
    
    @GetMapping("/markets")
    @Operation(summary = "Get all markets", description = "Returns a list of all available markets")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved markets"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<MarketsResponse> getMarkets(
            @Parameter(description = "Filter by event ID") @RequestParam(required = false) String event_id,
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status) {
        
        List<Market> markets = marketService.getAllMarkets();
        
        // Apply filters if provided
        if (event_id != null) {
            markets = markets.stream()
                .filter(m -> m.getEvent_id().equals(event_id))
                .toList();
        }
        
        if (status != null) {
            markets = markets.stream()
                .filter(m -> m.getStatus().equals(status))
                .toList();
        }
        
        return ResponseEntity.ok(new MarketsResponse(markets));
    }
    
    @GetMapping("/markets/{ticker}")
    @Operation(summary = "Get market by ticker", description = "Returns detailed information about a specific market")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Market found"),
        @ApiResponse(responseCode = "404", description = "Market not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<MarketResponse> getMarket(
            @Parameter(description = "Market ticker", required = true) @PathVariable String ticker) {
        
        try {
            Market market = marketService.getMarket(ticker);
            return ResponseEntity.ok(new MarketResponse(market));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/markets/{ticker}/orderbook")
    @Operation(summary = "Get market orderbook", description = "Returns the current orderbook for a specific market")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Orderbook retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Market not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<OrderbookResponse> getOrderbook(
            @Parameter(description = "Market ticker", required = true) @PathVariable String ticker,
            @Parameter(description = "Depth of orderbook") @RequestParam(required = false, defaultValue = "5") Integer depth) {
        
        try {
            // Verify market exists
            marketService.getMarket(ticker);
            
            // Get orderbook from order book service
            Orderbook orderbook = orderBookService.getOrderbook(ticker);
            
            return ResponseEntity.ok(new OrderbookResponse(orderbook));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/events")
    @Operation(summary = "Get all events", description = "Returns a list of all available events")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved events"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<EventsResponse> getEvents(
            @Parameter(description = "Filter by category") @RequestParam(required = false) String category,
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status) {
        
        List<Event> events = marketService.getAllEvents();
        
        // Apply filters if provided
        if (category != null) {
            events = events.stream()
                .filter(e -> e.getCategory().equalsIgnoreCase(category))
                .toList();
        }
        
        if (status != null) {
            events = events.stream()
                .filter(e -> e.getStatus().equals(status))
                .toList();
        }
        
        return ResponseEntity.ok(new EventsResponse(events));
    }
    
    @GetMapping("/events/{event_id}")
    @Operation(summary = "Get event by ID", description = "Returns detailed information about a specific event")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event found"),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Event> getEvent(
            @Parameter(description = "Event ID", required = true) @PathVariable String event_id) {
        
        try {
            Event event = marketService.getEvent(event_id);
            return ResponseEntity.ok(event);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/markets/{ticker}/trades")
    @Operation(summary = "Get market trades", description = "Returns recent trades for a specific market")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trades retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Market not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<TradesResponse> getMarketTrades(
            @Parameter(description = "Market ticker", required = true) @PathVariable String ticker,
            @Parameter(description = "Maximum number of trades to return") @RequestParam(required = false, defaultValue = "100") Integer limit,
            @Parameter(description = "Cursor for pagination") @RequestParam(required = false) String cursor) {
        
        try {
            // Verify market exists
            marketService.getMarket(ticker);
            
            // Get trades from order book service
            List<Trade> trades = orderBookService.getMarketTrades(ticker);
            
            // Apply limit
            if (limit != null && trades.size() > limit) {
                trades = trades.subList(0, limit);
            }
            
            return ResponseEntity.ok(new TradesResponse(trades, null));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}