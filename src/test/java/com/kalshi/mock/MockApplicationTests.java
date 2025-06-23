package com.kalshi.mock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class MockApplicationTests {

    @Autowired
    private MockFixClient mockFixClient;


    @Test
    void contextLoads() {
        assertNotNull(mockFixClient);
    }

    @Test
    void testSending77PriceIOCMarketOrderIsFilled() {
        
    }
    
}
