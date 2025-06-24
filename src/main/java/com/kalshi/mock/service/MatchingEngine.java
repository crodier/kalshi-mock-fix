package com.kalshi.mock.service;

import com.kalshi.mock.model.ConcurrentOrderBook;
import com.kalshi.mock.model.OrderBookEntry;
import com.fbg.api.rest.*;
import com.fbg.api.market.Side;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Matching engine that executes trades when orders cross in the order book.
 * All matching is done in the normalized YES order book space.
 */
public class MatchingEngine {
    
    private final AtomicLong tradeIdGenerator = new AtomicLong(1);
    private final AtomicLong fillIdGenerator = new AtomicLong(1);
    
    /**
     * Attempt to match a new order against the order book
     * @return List of executions that occurred
     */
    public List<Execution> matchOrder(OrderBookEntry incomingOrder, ConcurrentOrderBook orderBook) {
        List<Execution> executions = new ArrayList<>();
        
        // Skip market orders with no price (for now)
        if (incomingOrder.getNormalizedPrice() == 0) {
            return executions;
        }
        
        // Determine which side of the book to match against
        boolean incomingIsBuy = incomingOrder.isNormalizedBuy();
        
        while (incomingOrder.getQuantity() > 0) {
            Map.Entry<Integer, Queue<OrderBookEntry>> bestLevel = null;
            
            if (incomingIsBuy) {
                // Buy order matches against asks (sells)
                bestLevel = orderBook.getBestAsk();
                
                // Check if we can match (buy price >= ask price)
                if (bestLevel == null || incomingOrder.getNormalizedPrice() < bestLevel.getKey()) {
                    break; // No match possible
                }
            } else {
                // Sell order matches against bids (buys)
                bestLevel = orderBook.getBestBid();
                
                // Check if we can match (sell price <= bid price)
                if (bestLevel == null || incomingOrder.getNormalizedPrice() > bestLevel.getKey()) {
                    break; // No match possible
                }
            }
            
            // Match against orders at the best level
            Queue<OrderBookEntry> ordersAtLevel = bestLevel.getValue();
            Iterator<OrderBookEntry> iterator = ordersAtLevel.iterator();
            
            while (iterator.hasNext() && incomingOrder.getQuantity() > 0) {
                OrderBookEntry restingOrder = iterator.next();
                
                if (restingOrder.getQuantity() == 0) {
                    continue; // Skip depleted orders
                }
                
                // Calculate execution quantity
                int executionQty = Math.min(incomingOrder.getQuantity(), restingOrder.getQuantity());
                
                // Determine execution price (price of resting order)
                int executionPrice = bestLevel.getKey();
                
                // Create execution record
                Execution execution = new Execution(
                    generateTradeId(),
                    incomingOrder,
                    restingOrder,
                    executionQty,
                    executionPrice,
                    System.currentTimeMillis()
                );
                
                executions.add(execution);
                
                // Update quantities
                incomingOrder.reduceQuantity(executionQty);
                restingOrder.reduceQuantity(executionQty);
                
                // Notify order book of execution
                orderBook.notifyOrderExecuted(restingOrder, executionQty);
                
                // Remove resting order if fully filled
                if (restingOrder.getQuantity() == 0) {
                    iterator.remove();
                    orderBook.removeFilledOrder(restingOrder.getOrderId());
                }
            }
            
            // Clean up empty price level
            if (ordersAtLevel.isEmpty()) {
                if (incomingIsBuy) {
                    orderBook.removeEmptyAskLevel(bestLevel.getKey());
                } else {
                    orderBook.removeEmptyBidLevel(bestLevel.getKey());
                }
            }
        }
        
        return executions;
    }
    
    /**
     * Convert executions to trades (public trade tape)
     */
    public List<Trade> executionsToTrades(List<Execution> executions, String marketTicker) {
        List<Trade> trades = new ArrayList<>();
        
        for (Execution exec : executions) {
            // Determine the original side that initiated the trade
            OrderBookEntry aggressor = exec.getAggressor();
            
            // Convert back to original YES/NO representation for public trade
            Side tradeSide = aggressor.getSide();
            int tradePrice = aggressor.getPrice();
            
            Trade trade = new Trade(
                exec.getTradeId(),
                marketTicker,
                marketTicker,
                tradePrice,
                exec.getQuantity(),
                tradeSide,
                exec.getTimestamp(),
                null, // yes_price - can be calculated if needed
                null  // no_price - can be calculated if needed
            );
            
            trades.add(trade);
        }
        
        return trades;
    }
    
    /**
     * Convert executions to fills (private fill records)
     */
    public List<Fill> executionsToFills(List<Execution> executions, String marketTicker) {
        List<Fill> fills = new ArrayList<>();
        
        for (Execution exec : executions) {
            // Create fill for aggressor
            fills.add(createFill(exec, exec.getAggressor(), marketTicker, true));
            
            // Create fill for passive order
            fills.add(createFill(exec, exec.getPassive(), marketTicker, false));
        }
        
        return fills;
    }
    
    private Fill createFill(Execution exec, OrderBookEntry order, String marketTicker, boolean isTaker) {
        return new Fill(
            "FILL-" + fillIdGenerator.incrementAndGet(),
            order.getOrderId(),
            marketTicker,
            marketTicker,
            order.getSide(),
            order.getPrice(), // Original price, not execution price
            exec.getQuantity(),
            isTaker,
            exec.getTimestamp(),
            exec.getTradeId()
        );
    }
    
    private String generateTradeId() {
        return "TRD-" + tradeIdGenerator.incrementAndGet();
    }
    
    /**
     * Execution record containing details of a matched trade
     */
    public static class Execution {
        private final String tradeId;
        private final OrderBookEntry aggressor;
        private final OrderBookEntry passive;
        private final int quantity;
        private final int executionPrice; // In normalized YES space
        private final long timestamp;
        
        public Execution(String tradeId, OrderBookEntry aggressor, OrderBookEntry passive, 
                        int quantity, int executionPrice, long timestamp) {
            this.tradeId = tradeId;
            this.aggressor = aggressor;
            this.passive = passive;
            this.quantity = quantity;
            this.executionPrice = executionPrice;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getTradeId() { return tradeId; }
        public OrderBookEntry getAggressor() { return aggressor; }
        public OrderBookEntry getPassive() { return passive; }
        public int getQuantity() { return quantity; }
        public int getExecutionPrice() { return executionPrice; }
        public long getTimestamp() { return timestamp; }
    }
}