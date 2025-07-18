package com.etna.gpe.ms_payment_api.repositories;

import com.etna.gpe.ms_payment_api.entity.ShopStripeAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShopStripeAccountRepository extends JpaRepository<ShopStripeAccount, UUID> {

    Optional<ShopStripeAccount> findByShopId(UUID shopId);

    Optional<ShopStripeAccount> findByStripeAccountId(String stripeAccountId);

    boolean existsByShopId(UUID shopId);
}
