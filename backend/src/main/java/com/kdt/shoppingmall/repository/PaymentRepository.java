package com.kdt.shoppingmall.repository;

import com.kdt.shoppingmall.domain.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {}
