package com.etna.gpe.ms_payment_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shop_stripe_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopStripeAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "shop_id", columnDefinition = "BINARY(16)", length = 36, nullable = false, unique = true)
    private UUID shopId;

    @Column(name = "stripe_account_id", nullable = false, unique = true)
    private String stripeAccountId;

    @Column(name = "onboarding_completed", nullable = false)
    @Builder.Default
    private Boolean onboardingCompleted = false;

    @Column(name = "charges_enabled", nullable = false)
    @Builder.Default
    private Boolean chargesEnabled = false;

    @Column(name = "payouts_enabled", nullable = false)
    @Builder.Default
    private Boolean payoutsEnabled = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
