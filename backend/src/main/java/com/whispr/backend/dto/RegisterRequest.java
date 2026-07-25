package com.whispr.backend.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String pseudo;
    private String password;
}
