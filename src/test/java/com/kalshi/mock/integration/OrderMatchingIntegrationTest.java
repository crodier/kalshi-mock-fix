package com.kalshi.mock.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbg.api.rest.FillsResponse;
import com.fbg.api.rest.OrderResponse;
import com.fbg.api.rest.PositionsResponse;
import com.kalshi.mock.dto.KalshiOrderRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderMatchingIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("Order Matching Creates Fills and Updates Positions")
    public void testOrderMatchingFlow() throws Exception {
        // Step 1: Create a buy order
        KalshiOrderRequest buyOrder = new KalshiOrderRequest();
        buyOrder.setMarketTicker("BTCZ-23DEC31-B50000");
        buyOrder.setSide("yes");
        buyOrder.setAction("buy");
        buyOrder.setType("limit");
        buyOrder.setCount(50);
        buyOrder.setPrice(72);
        
        MvcResult buyResult = mockMvc.perform(post("/trade-api/v2/portfolio/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buyOrder)))
                .andExpect(status().isCreated())
                .andReturn();
        
        OrderResponse buyResponse = objectMapper.readValue(
            buyResult.getResponse().getContentAsString(), 
            OrderResponse.class
        );
        assertEquals("open", buyResponse.getOrder().getStatus());
        assertEquals(50, buyResponse.getOrder().getRemaining_quantity());
        
        // Step 2: Create a crossing sell order
        KalshiOrderRequest sellOrder = new KalshiOrderRequest();
        sellOrder.setMarketTicker("BTCZ-23DEC31-B50000");
        sellOrder.setSide("yes");
        sellOrder.setAction("sell");
        sellOrder.setType("limit");
        sellOrder.setCount(30);
        sellOrder.setPrice(72);
        
        MvcResult sellResult = mockMvc.perform(post("/trade-api/v2/portfolio/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sellOrder)))
                .andExpect(status().isCreated())
                .andReturn();
        
        OrderResponse sellResponse = objectMapper.readValue(
            sellResult.getResponse().getContentAsString(), 
            OrderResponse.class
        );
        assertEquals("filled", sellResponse.getOrder().getStatus());
        assertEquals(0, sellResponse.getOrder().getRemaining_quantity());
        assertEquals(30, sellResponse.getOrder().getFilled_quantity());
        
        // Step 3: Check fills were created
        MvcResult fillsResult = mockMvc.perform(get("/trade-api/v2/portfolio/fills"))
                .andExpect(status().isOk())
                .andReturn();
        
        FillsResponse fillsResponse = objectMapper.readValue(
            fillsResult.getResponse().getContentAsString(), 
            FillsResponse.class
        );
        assertFalse(fillsResponse.getFills().isEmpty());
        
        // Step 4: Check positions were updated
        MvcResult positionsResult = mockMvc.perform(get("/trade-api/v2/portfolio/positions"))
                .andExpect(status().isOk())
                .andReturn();
        
        PositionsResponse positionsResponse = objectMapper.readValue(
            positionsResult.getResponse().getContentAsString(), 
            PositionsResponse.class
        );
        
        // Should have at least one position for this market
        assertTrue(positionsResponse.getPositions().stream()
            .anyMatch(p -> p.getMarket_ticker().equals("BTCZ-23DEC31-B50000")));
    }
    
    @Test
    @DisplayName("NO Order Conversion and Matching")
    public void testNOOrderConversion() throws Exception {
        // Step 1: Create a YES bid at 60¢
        KalshiOrderRequest yesBid = new KalshiOrderRequest();
        yesBid.setMarketTicker("TRUMPWIN-24NOV05");
        yesBid.setSide("yes");
        yesBid.setAction("buy");
        yesBid.setType("limit");
        yesBid.setCount(100);
        yesBid.setPrice(60);
        
        mockMvc.perform(post("/trade-api/v2/portfolio/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(yesBid)))
                .andExpect(status().isCreated());
        
        // Step 2: Create a NO bid at 40¢ (converts to Sell YES at 60¢)
        KalshiOrderRequest noBid = new KalshiOrderRequest();
        noBid.setMarketTicker("TRUMPWIN-24NOV05");
        noBid.setSide("no");
        noBid.setAction("buy");
        noBid.setType("limit");
        noBid.setCount(100);
        noBid.setPrice(40);
        
        MvcResult noResult = mockMvc.perform(post("/trade-api/v2/portfolio/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(noBid)))
                .andExpect(status().isCreated())
                .andReturn();
        
        OrderResponse noResponse = objectMapper.readValue(
            noResult.getResponse().getContentAsString(), 
            OrderResponse.class
        );
        
        // The NO buy order should be filled because it crosses with YES bid
        assertEquals("filled", noResponse.getOrder().getStatus());
        
        // Check that fills were created
        MvcResult fillsResult = mockMvc.perform(get("/trade-api/v2/portfolio/fills"))
                .andExpect(status().isOk())
                .andReturn();
        
        FillsResponse fillsResponse = objectMapper.readValue(
            fillsResult.getResponse().getContentAsString(), 
            FillsResponse.class
        );
        
        // Should have fills for both orders
        assertTrue(fillsResponse.getFills().size() >= 2);
    }
    
    @Test
    @DisplayName("Partial Fill Updates Order Status")
    public void testPartialFill() throws Exception {
        // Step 1: Create a large buy order
        KalshiOrderRequest largeBuy = new KalshiOrderRequest();
        largeBuy.setMarketTicker("INXD-23DEC29-B5000");
        largeBuy.setSide("yes");
        largeBuy.setAction("buy");
        largeBuy.setType("limit");
        largeBuy.setCount(1000);
        largeBuy.setPrice(55);
        
        MvcResult buyResult = mockMvc.perform(post("/trade-api/v2/portfolio/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeBuy)))
                .andExpect(status().isCreated())
                .andReturn();
        
        OrderResponse buyResponse = objectMapper.readValue(
            buyResult.getResponse().getContentAsString(), 
            OrderResponse.class
        );
        String buyOrderId = buyResponse.getOrder().getId();
        
        // Step 2: Create a smaller crossing sell order
        KalshiOrderRequest smallSell = new KalshiOrderRequest();
        smallSell.setMarketTicker("INXD-23DEC29-B5000");
        smallSell.setSide("yes");
        smallSell.setAction("sell");
        smallSell.setType("limit");
        smallSell.setCount(300);
        smallSell.setPrice(55);
        
        mockMvc.perform(post("/trade-api/v2/portfolio/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(smallSell)))
                .andExpect(status().isCreated());
        
        // Step 3: Check that the buy order is partially filled
        mockMvc.perform(get("/trade-api/v2/portfolio/orders/" + buyOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order.status").value("partially_filled"))
                .andExpect(jsonPath("$.order.filled_quantity").value(300))
                .andExpect(jsonPath("$.order.remaining_quantity").value(700));
    }
}