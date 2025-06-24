package com.kalshi.mock.model;

import com.fbg.api.market.Side;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a single order in the order book
 */
public class OrderBookEntry {
    private static final AtomicLong SEQUENCE_GENERATOR = new AtomicLong(0);
    
    private final String orderId;
    private final String userId;
    private final Side side;  // YES or NO
    private final String action; // buy or sell
    private final int price; // in cents
    private int quantity;
    private final int originalQuantity;
    private final long timestamp;
    private final long sequence; // for FIFO ordering
    
    // Normalized values for internal order book representation
    private final int normalizedPrice;
    private final boolean normalizedIsBuy;
    
    public OrderBookEntry(String orderId, String userId, Side side, String action, 
                         int price, int quantity, long timestamp) {
        // Validate price is between 1 and 99 cents
        if (price < 1 || price > 99) {
            throw new IllegalArgumentException("Price must be between 1 and 99 cents, got: " + price);
        }
        
        this.orderId = orderId;
        this.userId = userId;
        this.side = side;
        this.action = action;
        this.price = price;
        this.quantity = quantity;
        this.originalQuantity = quantity;
        this.timestamp = timestamp;
        this.sequence = SEQUENCE_GENERATOR.incrementAndGet();
        
        // Apply NO/YES conversion for order book normalization
        if (side == Side.no) {
            if (action.equals("buy")) {
                // Buy NO @ P → Sell YES @ (100 - P)
                this.normalizedPrice = 100 - price;
                this.normalizedIsBuy = false;
            } else {
                // Sell NO @ P → Buy YES @ (100 - P)
                this.normalizedPrice = 100 - price;
                this.normalizedIsBuy = true;
            }
        } else {
            // YES orders remain as-is
            this.normalizedPrice = price;
            this.normalizedIsBuy = action.equals("buy");
        }
    }
    
    public void reduceQuantity(int amount) {
        this.quantity = Math.max(0, this.quantity - amount);
    }
    
    // Getters
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public Side getSide() { return side; }
    public String getAction() { return action; }
    public int getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public int getOriginalQuantity() { return originalQuantity; }
    public long getTimestamp() { return timestamp; }
    public long getSequence() { return sequence; }
    public int getNormalizedPrice() { return normalizedPrice; }
    public boolean isNormalizedBuy() { return normalizedIsBuy; }
    
    public int getFilledQuantity() {
        return originalQuantity - quantity;
    }
}