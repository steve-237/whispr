package com.whispr.backend.controller;

import com.whispr.backend.domain.User;
import com.whispr.backend.dto.AuthResponse;
import com.whispr.backend.dto.LoginRequest;
import com.whispr.backend.dto.RegisterRequest;
import com.whispr.backend.security.JwtUtil;
import com.whispr.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        User user = userService.registerUser(request.getEmail(), request.getPseudo(), request.getPassword());
        String token = jwtUtil.generateToken(user.getEmail(), user.getPseudo(), user.getRole().name());
        return ResponseEntity.ok(new AuthResponse(token, user.getPseudo(), user.getRole().name()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        User user = userService.getUserByEmailIgnoreCase(request.getEmail())
                .orElseGet(() -> userService.getUserByPseudoIgnoreCase(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found")));

        if (user.getRole() == com.whispr.backend.domain.Role.BANNED) {
            return ResponseEntity.status(403).build(); // Ou throw une exception plus précise
        }
                
        String token = jwtUtil.generateToken(user.getEmail(), user.getPseudo(), user.getRole().name());
        return ResponseEntity.ok(new AuthResponse(token, user.getPseudo(), user.getRole().name()));
    }
}
