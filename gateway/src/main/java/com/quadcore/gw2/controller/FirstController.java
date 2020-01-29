package com.quadcore.gw2.controller;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@CrossOrigin
@RestController
public class FirstController {

    @GetMapping(path="/test")
    public String hi() {
        return "hi";
    }

    @RequestMapping("/fallback")
    public Mono<String> fallback() {
        return Mono.just("fallback!");
    }

}
