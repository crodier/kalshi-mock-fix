package com.kalshi.mock.service;

import com.fbg.api.rest.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MarketService {
    
    private final Map<String, Market> markets = new ConcurrentHashMap<>();
    private final Map<String, Event> events = new ConcurrentHashMap<>();
    
    public MarketService() {
        initializeTestData();
    }
    
    private void initializeTestData() {
        // Create test events
        Event electionEvent = new Event(
            "EVT-ELECTION-2024",
            "ELECTION-2024",
            "Politics",
            "US Elections",
            "2024 US Presidential Election",
            "Market for the 2024 US Presidential Election outcome",
            "active",
            new ArrayList<>()
        );
        events.put(electionEvent.getId(), electionEvent);
        
        Event cryptoEvent = new Event(
            "EVT-BTC-2023",
            "BTC-2023",
            "Crypto",
            "Bitcoin",
            "Bitcoin Price Movements",
            "Markets for Bitcoin price predictions",
            "active",
            new ArrayList<>()
        );
        events.put(cryptoEvent.getId(), cryptoEvent);
        
        // Create test markets
        Market trumpMarket = new Market(
            "MKT-TRUMP-WIN",
            "TRUMPWIN-24NOV05",
            "EVT-ELECTION-2024",
            "active",
            65, // yes_bid
            66, // yes_ask
            34, // no_bid
            35, // no_ask
            65, // last_price
            150000L, // volume
            2500000L, // volume_24h
            75000L, // open_interest
            System.currentTimeMillis() - 86400000L, // open_time (24h ago)
            System.currentTimeMillis() + 86400000L * 300, // close_time (300 days from now)
            null, // expected_expiration_time
            null, // expiration_time
            null, // result
            false, // can_close_early
            null, // cap_strike
            null  // floor_strike
        );
        markets.put(trumpMarket.getTicker(), trumpMarket);
        
        Market btcMarket = new Market(
            "MKT-BTC-50K",
            "BTCZ-23DEC31-B50000",
            "EVT-BTC-2023",
            "active",
            71, // yes_bid
            72, // yes_ask
            28, // no_bid
            29, // no_ask
            71, // last_price
            500000L, // volume
            8000000L, // volume_24h
            200000L, // open_interest
            System.currentTimeMillis() - 86400000L, // open_time
            System.currentTimeMillis() + 86400000L * 7, // close_time (7 days)
            null,
            null,
            null,
            true, // can_close_early
            50000, // cap_strike
            null
        );
        markets.put(btcMarket.getTicker(), btcMarket);
        
        Market indexMarket = new Market(
            "MKT-SP500-5000",
            "INXD-23DEC29-B5000",
            "EVT-MARKET-2023",
            "active",
            45, // yes_bid
            46, // yes_ask
            54, // no_bid
            55, // no_ask
            45, // last_price
            300000L, // volume
            5000000L, // volume_24h
            150000L, // open_interest
            System.currentTimeMillis() - 86400000L,
            System.currentTimeMillis() + 86400000L * 5, // close_time (5 days)
            null,
            null,
            null,
            true,
            5000, // cap_strike
            null
        );
        markets.put(indexMarket.getTicker(), indexMarket);
    }
    
    public List<Market> getAllMarkets() {
        return new ArrayList<>(markets.values());
    }
    
    public Market getMarket(String ticker) {
        Market market = markets.get(ticker);
        if (market == null) {
            throw new NoSuchElementException("Market not found: " + ticker);
        }
        return market;
    }
    
    public List<Event> getAllEvents() {
        return new ArrayList<>(events.values());
    }
    
    public Event getEvent(String eventId) {
        Event event = events.get(eventId);
        if (event == null) {
            throw new NoSuchElementException("Event not found: " + eventId);
        }
        return event;
    }
    
    public void updateMarketPrice(String ticker, Integer yesBid, Integer yesAsk, Integer noBid, Integer noAsk) {
        Market market = markets.get(ticker);
        if (market != null) {
            // Create updated market with new prices
            Market updatedMarket = new Market(
                market.getId(),
                market.getTicker(),
                market.getEvent_id(),
                market.getStatus(),
                yesBid != null ? yesBid : market.getYes_bid(),
                yesAsk != null ? yesAsk : market.getYes_ask(),
                noBid != null ? noBid : market.getNo_bid(),
                noAsk != null ? noAsk : market.getNo_ask(),
                market.getLast_price(),
                market.getVolume(),
                market.getVolume_24h(),
                market.getOpen_interest(),
                market.getOpen_time(),
                market.getClose_time(),
                market.getExpected_expiration_time(),
                market.getExpiration_time(),
                market.getResult(),
                market.getCan_close_early(),
                market.getCap_strike(),
                market.getFloor_strike()
            );
            markets.put(ticker, updatedMarket);
        }
    }
}