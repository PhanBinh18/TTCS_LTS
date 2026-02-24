package com.example.demo.order;

import com.example.demo.product.Product;
import com.example.demo.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService; // Chỉ gọi Service của module khác

    @Transactional
    public Order createOrder(OrderRequest request) {
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setStatus("CREATED");

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            // Gọi ProductService để trừ kho
            Product product = productService.reduceStock(itemRequest.getProductId(), itemRequest.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setOrder(order);

            orderItems.add(orderItem);

            BigDecimal subTotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(subTotal);
        }

        order.setItems(orderItems);
        order.setTotalPrice(totalAmount);

        return orderRepository.save(order);
    }
}