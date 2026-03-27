package com.example.demo.order;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDto {
    private Long productId;
    private String productName;
    private BigDecimal price; // Giá tại thời điểm mua
    private Integer quantity;
}