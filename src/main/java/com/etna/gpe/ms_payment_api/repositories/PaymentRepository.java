package com.etna.gpe.ms_payment_api.repositories;

import com.etna.gpe.ms_payment_api.entity.Payment;
import com.etna.gpe.ms_payment_api.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    Optional<Payment> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

    List<Payment> findByUserIdAndStatus(UUID userId, PaymentStatus status);

    List<Payment> findByShopIdAndStatus(UUID shopId, PaymentStatus status);

    Optional<Payment> findByAppointmentId(UUID appointmentId);

    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.refundEligibleUntil < :now")
    List<Payment> findExpiredRefundEligiblePayments(@Param("status") PaymentStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT p FROM Payment p WHERE p.userId = :userId ORDER BY p.createdAt DESC")
    List<Payment> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}
