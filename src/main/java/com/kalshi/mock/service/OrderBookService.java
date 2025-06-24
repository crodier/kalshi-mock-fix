package com.kalshi.mock.service;

import com.fbg.api.rest.*;
import com.fbg.api.market.Side;
import com.kalshi.mock.model.ConcurrentOrderBook;
import com.kalshi.mock.model.OrderBookEntry;
import com.kalshi.mock.service.MatchingEngine;
import com.kalshi.mock.service.MatchingEngine.Execution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OrderBookService implements ConcurrentOrderBook.OrderBookListener {
    
    private final Map<String, ConcurrentOrderBook> orderBooks = new ConcurrentHashMap<>();
    private final AtomicLong orderIdGenerator = new AtomicLong(1000);
    private final MatchingEngine matchingEngine = new MatchingEngine();
    
    @Autowired
    private PersistenceService persistenceService;
    
    @Autowired
    private PositionsService positionsService;
    
    @PostConstruct
    private void initializeTestMarkets() {
        // Create a few test markets
        createOrderBook("INXD-23DEC29-B5000");
        createOrderBook("BTCZ-23DEC31-B50000");
        createOrderBook("TRUMPWIN-24NOV05");
    }
    
    private void createOrderBook(String marketTicker) {
        ConcurrentOrderBook orderBook = new ConcurrentOrderBook(marketTicker);
        orderBook.addListener(this);
        orderBooks.put(marketTicker, orderBook);
        
        // Load existing open orders from database
        loadOpenOrdersForMarket(marketTicker);
    }
    
    private void loadOpenOrdersForMarket(String marketTicker) {
        // This would load existing open orders from the database
        // For now, we'll start with empty order books
    }
    
    public Order createOrder(String marketTicker, OrderRequest request, String action, String userId) {
        ConcurrentOrderBook orderBook = orderBooks.get(marketTicker);
        if (orderBook == null) {
            throw new IllegalArgumentException("Market not found: " + marketTicker);
        }
        
        // Validate order request
        validateOrderRequest(request);
        
        // Generate order ID
        String orderId = "ORD-" + orderIdGenerator.incrementAndGet();
        long timestamp = System.currentTimeMillis();
        
        // Create order book entry
        OrderBookEntry bookEntry = new OrderBookEntry(
            orderId,
            userId,
            request.getSide(),
            action, // Use the provided action
            request.getPrice() != null ? request.getPrice() : 0,
            request.getQuantity(),
            timestamp
        );
        
        // First, attempt to match the order
        List<Execution> executions = matchingEngine.matchOrder(bookEntry, orderBook);
        
        // Process executions
        if (!executions.isEmpty()) {
            // Convert to trades and fills
            List<Trade> trades = matchingEngine.executionsToTrades(executions, marketTicker);
            List<Fill> fills = matchingEngine.executionsToFills(executions, marketTicker);
            
            // Store trades in database using execution data
            for (int i = 0; i < trades.size() && i < executions.size(); i++) {
                Trade trade = trades.get(i);
                Execution exec = executions.get(i);
                persistenceService.saveTrade(
                    trade.getTrade_id(),
                    marketTicker,
                    exec.getAggressor().getOrderId(),
                    exec.getPassive().getOrderId(),
                    trade.getCount(), // Use getCount() instead of getQuantity()
                    trade.getPrice()
                );
            }
            
            // Store fills and update positions
            for (Fill fill : fills) {
                OrderBookEntry orderEntry = orderBook.getOrder(fill.getOrder_id());
                if (orderEntry != null) {
                    persistenceService.saveFill(fill, orderEntry.getUserId());
                    // Pass whether this was a buy or sell order
                    boolean isBuy = orderEntry.getAction().equals("buy");
                    positionsService.updatePositionFromFill(fill, orderEntry.getUserId(), isBuy);
                } else if (fill.getOrder_id().equals(orderId)) {
                    // This is the incoming order
                    persistenceService.saveFill(fill, userId);
                    boolean isBuy = action.equals("buy");
                    positionsService.updatePositionFromFill(fill, userId, isBuy);
                }
            }
        }
        
        // If order has remaining quantity, add to order book
        if (bookEntry.getQuantity() > 0) {
            boolean added = orderBook.addOrder(bookEntry);
            if (!added) {
                throw new IllegalStateException("Failed to add order to book");
            }
        }
        
        // Calculate filled quantity and status
        int filledQuantity = request.getQuantity() - bookEntry.getQuantity();
        String status = filledQuantity == 0 ? "open" : 
                       (bookEntry.getQuantity() == 0 ? "filled" : "partially_filled");
        
        // Calculate average fill price
        Integer avgFillPrice = null;
        if (!executions.isEmpty()) {
            int totalValue = 0;
            int totalQty = 0;
            for (Execution exec : executions) {
                totalValue += exec.getExecutionPrice() * exec.getQuantity();
                totalQty += exec.getQuantity();
            }
            avgFillPrice = totalQty > 0 ? totalValue / totalQty : null;
        }
        
        // Create and store Order object
        Order order = new Order(
            orderId,
            request.getClient_order_id(),
            userId,
            request.getSide(),
            marketTicker,
            request.getOrder_type(),
            request.getQuantity(),
            filledQuantity,
            bookEntry.getQuantity(), // remaining_quantity
            request.getPrice(),
            avgFillPrice,
            status,
            request.getTime_in_force(),
            timestamp,
            timestamp,
            null // expiration_time
        );
        
        // Persist order to database
        persistenceService.saveOrder(order, action);
        
        return order;
    }
    
    public Order cancelOrder(String orderId) {
        Order order = persistenceService.getOrder(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        
        ConcurrentOrderBook orderBook = orderBooks.get(order.getSymbol());
        if (orderBook == null) {
            throw new IllegalArgumentException("Market not found for order");
        }
        
        boolean canceled = orderBook.cancelOrder(orderId);
        if (!canceled) {
            throw new IllegalStateException("Failed to cancel order");
        }
        
        // Update order status in database
        persistenceService.updateOrderStatus(
            orderId,
            "canceled",
            order.getFilled_quantity(),
            0, // remaining_quantity
            order.getAvg_fill_price()
        );
        
        // Return updated order
        return persistenceService.getOrder(orderId);
    }
    
    public Orderbook getOrderbook(String marketTicker) {
        ConcurrentOrderBook orderBook = orderBooks.get(marketTicker);
        if (orderBook == null) {
            return new Orderbook(new ArrayList<>(), new ArrayList<>());
        }
        
        // Get orderbook snapshot with depth of 10
        return orderBook.getOrderbookSnapshot(10);
    }
    
    public List<Order> getUserOrders(String userId) {
        return persistenceService.getUserOrders(userId);
    }
    
    public Order getOrder(String orderId) {
        Order order = persistenceService.getOrder(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        return order;
    }
    
    public List<Trade> getMarketTrades(String marketTicker) {
        // TODO: Implement trade retrieval from database
        return new ArrayList<>();
    }
    
    public List<Fill> getUserFills(String userId) {
        return persistenceService.getUserFills(userId);
    }
    
    // ConcurrentOrderBook.OrderBookListener implementation
    @Override
    public void onOrderAdded(String marketTicker, OrderBookEntry order) {
        // Log or process order addition
        System.out.println("Order added: " + order.getOrderId() + " to market " + marketTicker);
    }
    
    @Override
    public void onOrderCanceled(String marketTicker, OrderBookEntry order) {
        // Log or process order cancellation
        System.out.println("Order canceled: " + order.getOrderId() + " from market " + marketTicker);
    }
    
    @Override
    public void onOrderExecuted(String marketTicker, OrderBookEntry order, int executedQuantity) {
        // Process execution - update order status, create fills and trades
        System.out.println("Order executed: " + order.getOrderId() + " quantity: " + executedQuantity);
        
        // Update order in database
        Order storedOrder = persistenceService.getOrder(order.getOrderId());
        if (storedOrder != null) {
            int newFilledQuantity = storedOrder.getFilled_quantity() + executedQuantity;
            int newRemainingQuantity = storedOrder.getQuantity() - newFilledQuantity;
            String newStatus = newRemainingQuantity == 0 ? "filled" : "partially_filled";
            
            persistenceService.updateOrderStatus(
                order.getOrderId(),
                newStatus,
                newFilledQuantity,
                newRemainingQuantity,
                storedOrder.getAvg_fill_price() // Will be recalculated if needed
            );
        }
    }
    
    @Override
    public void onCrossDetected(String marketTicker, OrderBookEntry order) {
        // Log cross detection - in real implementation would trigger matching
        System.out.println("Cross detected in market " + marketTicker + " for order " + order.getOrderId());
    }
    
    // Helper methods
    private void validateOrderRequest(OrderRequest request) {
        if (request.getSymbol() == null || request.getSymbol().isEmpty()) {
            throw new IllegalArgumentException("Market ticker is required");
        }
        if (request.getSide() == null) {
            throw new IllegalArgumentException("Side is required");
        }
        if (request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if ("limit".equals(request.getOrder_type()) && 
            (request.getPrice() == null || request.getPrice() < 1 || request.getPrice() > 99)) {
            throw new IllegalArgumentException("Limit orders require price between 1 and 99");
        }
    }
    
}