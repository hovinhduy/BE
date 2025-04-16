package com.ktpm.paymentService.model;
import lombok.Data;

@Data
public class OrderEventDTO {
    private String id;
    private String customerId;
    private String productId;
    private Integer quantity;
    private Double totalAmount;
    private String status;
}