package com.example.demo.cart;

import com.example.demo.product.Product;
import lombok.Data;

@Data
public class CartItemDto {
    private Long id;
    private Long productId;
    private Integer quantity;
    // Đây là nơi chứa cục Product xịn, không bị Hibernate quản lý
    private Product product;
}