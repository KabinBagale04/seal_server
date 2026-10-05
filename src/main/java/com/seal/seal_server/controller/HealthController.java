package com.seal.seal_server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController//the annotation means that this class handles http api request
@RequestMapping("/api")// request mapping le chai /api lai base path vanerw note garxa
public class HealthController {
    @GetMapping("/health")// yo getmapping le chai request mapping le quote gareko base paxi health add garxa
    public String health(){
        return "SEAL server is running";
    }
}