package com.ktpm.productService.service;

import com.ktpm.productService.config.KafkaTopicConfig;
import com.ktpm.productService.dto.event.ProductCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendProductCreatedEvent(ProductCreatedEvent event) {
        kafkaTemplate.send(KafkaTopicConfig.PRODUCT_CREATED_TOPIC, event);
    }
}