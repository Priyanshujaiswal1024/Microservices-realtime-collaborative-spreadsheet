package com.collab.spreadsheet.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestMethod;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping(value = "/user-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public Mono<ResponseEntity<Map<String, Object>>> userServiceFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", 503,
                "code", "SERVICE_UNAVAILABLE",
                "message", "User Service is temporarily unavailable. Please try again in a few moments.",
                "timestamp", Instant.now().toString()
        )));
    }

    @RequestMapping(value = "/sheet-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public Mono<ResponseEntity<Map<String, Object>>> sheetServiceFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", 503,
                "code", "SERVICE_UNAVAILABLE",
                "message", "Sheet Service is temporarily unavailable. Please try again in a few moments.",
                "timestamp", Instant.now().toString()
        )));
    }

    @RequestMapping(value = "/comment-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public Mono<ResponseEntity<Map<String, Object>>> commentServiceFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", 503,
                "code", "SERVICE_UNAVAILABLE",
                "message", "Comment Service is temporarily unavailable. Please try again in a few moments.",
                "timestamp", Instant.now().toString()
        )));
    }
}
