package com.kalshi.mock.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class OrderBookEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderBookEventPublisher.class);
    
    private final List<OrderBookEventListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    
    public void addListener(OrderBookEventListener listener) {
        listeners.add(listener);
        logger.info("Added listener: {}", listener.getClass().getSimpleName());
    }
    
    public void removeListener(OrderBookEventListener listener) {
        listeners.remove(listener);
        logger.info("Removed listener: {}", listener.getClass().getSimpleName());
    }
    
    public void publishEvent(OrderBookEvent event) {
        if (listeners.isEmpty()) {
            return;
        }
        
        // Publish asynchronously to avoid blocking
        executor.submit(() -> {
            for (OrderBookEventListener listener : listeners) {
                try {
                    listener.onOrderBookEvent(event);
                } catch (Exception e) {
                    logger.error("Error notifying listener: {}", listener.getClass().getSimpleName(), e);
                }
            }
        });
    }
    
    public void shutdown() {
        executor.shutdown();
    }
}