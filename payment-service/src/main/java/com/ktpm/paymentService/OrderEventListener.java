package com.ktpm.paymentService;

import com.google.gson.Gson;
import com.ktpm.paymentService.model.OrderEventDTO;
import com.ktpm.paymentService.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private final PaymentService paymentService;

    public OrderEventListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "order-created-topic", groupId = "payment-group")
    public void handleOrderCreated(String orderJson) {
        OrderEventDTO order = new Gson().fromJson(orderJson, OrderEventDTO.class);
        paymentService.processPayment(order);
    }
}