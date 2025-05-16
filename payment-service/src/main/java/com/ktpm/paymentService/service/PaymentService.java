package com.ktpm.paymentService.service;

import com.ktpm.paymentService.dto.PaymentRequest;
import com.ktpm.paymentService.model.Payment;
import com.ktpm.paymentService.model.PaymentStatus;
import com.ktpm.paymentService.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PayOSClient payOSClient;

    public PaymentService(PaymentRepository paymentRepository, PayOSClient payOSClient) {
        this.paymentRepository = paymentRepository;
        this.payOSClient = payOSClient;
    }

    public String createPaymentAndGetUrl(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        return payOSClient.createPaymentUrl(request.getOrderId(), request.getAmount());
    }

    public Payment updateStatus(Long orderId, String status) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (status.equals("SUCCESS")) {
            payment.setStatus(PaymentStatus.SUCCESS);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        return paymentRepository.save(payment);
    }
}
