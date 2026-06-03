package com.foodscanner.api.controller;

import com.foodscanner.api.dto.PingResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Слой: api
 * GET /api/v1/ping — heartbeat для клиента.
 */
@RestController
@RequestMapping("/api/v1")
public class PingController {

    @GetMapping("/ping")
    public PingResponse ping() {
        return PingResponse.now();
    }
}
