package com.example.demo.cart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "cart_items")
@Data
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cart_id")
    @JsonIgnore // Ngắt vòng lặp JSON
    private Cart cart;

    private Long productId; // Chỉ lưu ID (Liên kết lỏng)
    private Integer quantity;
}