package com.example.backend.controller;

import com.example.backend.JwtUtil;
import com.example.backend.RegisterRequest;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepo;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (userRepo.findByUsername(req.getUsername()) != null)
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User exists");

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(encoder.encode(req.getPassword()));
        user.setRole(req.getRole()); // "student" or "teacher"
        userRepo.save(user);

        return ResponseEntity.ok("Registered");
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        System.out.println(user.toString());
        User found = userRepo.findByUsername(user.getUsername());
        if (found != null && encoder.matches(user.getPassword(), found.getPassword())) {
            String token = jwtUtil.generateToken(found.getUsername());
            return ResponseEntity.ok(Map.of("token", token, "role",
                    found.getRole()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
