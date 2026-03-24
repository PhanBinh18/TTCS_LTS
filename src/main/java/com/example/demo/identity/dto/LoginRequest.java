package com.example.demo.identity.dto;

import lombok.Data;

/**
 * Object hứng dữ liệu khi người dùng đăng nhập.
 */
@Data
public class LoginRequest {
    private String email;
    private String password;
}