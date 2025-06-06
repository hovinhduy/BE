package com.ktpm.paymentService.repository;

import com.ktpm.paymentService.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderCode(int orderCode);
    Optional<Payment> findByOrderId(Long orderId);
}
