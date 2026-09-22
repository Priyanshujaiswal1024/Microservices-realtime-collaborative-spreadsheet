package com.collab.spreadsheet.eureka;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Eureka Server
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EurekaServerApplicationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
        // Verify Spring context loads successfully
        assertNotNull(restTemplate);
    }

    @Test
    void eurekaServerIsRunning() {
        String url = "http://localhost:" + port + "/actuator/health";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("UP") || response.getBody().contains("\"status\":\"UP\""));
    }

    @Test
    void eurekaDashboardIsAccessible() {
        String url = "http://localhost:" + port + "/";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // Check for Eureka-specific content
        assertTrue(response.getBody().contains("Eureka") || response.getBody().contains("instances"));
    }

    @Test
    void appsEndpointIsAccessible() {
        String url = "http://localhost:" + port + "/eureka/apps";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Should return 200 even with no apps registered
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
