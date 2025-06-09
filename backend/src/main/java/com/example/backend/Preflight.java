package com.example.backend;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Preflight {
    @RequestMapping(value = "/**", method = RequestMethod.POST)
    public ResponseEntity<?> handle() {
        return ResponseEntity.ok().build();
    }
}

