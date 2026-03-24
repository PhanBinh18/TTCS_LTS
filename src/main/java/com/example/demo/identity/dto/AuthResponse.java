package com.example.demo.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Object trả về cho Frontend sau khi đăng nhập/đăng ký thành công, chứa thẻ JWT.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
}