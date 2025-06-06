package com.ktpm.paymentService.service;

import com.ktpm.paymentService.dto.PaymentRequest;
import com.ktpm.paymentService.model.Payment;
import com.ktpm.paymentService.model.PaymentStatus;
import com.ktpm.paymentService.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PayOSClient payOSClient;

    public PaymentService(PaymentRepository paymentRepository, PayOSClient payOSClient) {
        this.paymentRepository = paymentRepository;
        this.payOSClient = payOSClient;
    }

    public String createPaymentAndGetUrl(PaymentRequest request) {
        Optional<Payment> existing = paymentRepository.findByOrderId(request.getOrderId());
        if (existing.isPresent()) {
            return existing.get().getPaymentUrl();
        }

        int generatedOrderCode = (int) (Math.random() * 10000 + 1000);
        String paymentUrl = payOSClient.createPaymentUrl(request.getOrderId(), generatedOrderCode, request.getAmount());

        if (paymentUrl == null) {
            throw new RuntimeException("Failed to create payment URL");
        }

        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setPaymentUrl(paymentUrl);
        payment.setOrderCode(generatedOrderCode);

        paymentRepository.save(payment);

        return paymentUrl;
    }

    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId).orElse(null);
    }

    public Payment updateStatus(int orderCode, String status) {
        Payment payment = paymentRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (status.equals("PAID")) {
            payment.setStatus(PaymentStatus.PAID);
        } else if (status.equals("CANCELLED")) {
            payment.setStatus(PaymentStatus.CANCELLED);
        } else {
            throw new RuntimeException("Invalid status");
        }

        return paymentRepository.save(payment);
    }

    public String checkStatus(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        String status = payOSClient.getPaymentStatus(payment.getOrderCode());

        // Cập nhật trạng thái thanh toán nếu là CANCELLED
        if (status.equals("CANCELLED")) {
            payment.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);
        }
        if (status.equals("PAID")) {
            payment.setStatus(PaymentStatus.PAID);
            paymentRepository.save(payment);
        }
        if (status.equals("PENDING")) {
            payment.setStatus(PaymentStatus.PENDING);
            paymentRepository.save(payment);
        }

        return status;
    }

    public Long getOrderIdByOrderCode(int orderCode){
        Payment payment = paymentRepository.findByOrderCode(orderCode).orElse(null);
        return payment.getOrderId();
    }
}
