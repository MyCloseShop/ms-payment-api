package com.etna.gpe.ms_payment_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration pour les frais de plateforme Stripe.
 * Cette classe gère les paramètres liés aux frais prélevés sur chaque transaction.
 */
@Configuration
public class StripeFeeConfig {

    @Value("${stripe.platform.fee_percentage:5.0}")
    private double platformFeePercentage;

    /**
     * Retourne le pourcentage de frais de plateforme.
     * @return Le pourcentage de frais (ex: 5.0 pour 5%)
     */
    public double getPlatformFeePercentage() {
        return platformFeePercentage;
    }

    /**
     * Calcule le montant des frais de plateforme pour un montant donné.
     * @param amount Le montant de la transaction en centimes
     * @return Le montant des frais en centimes
     */
    public long calculateFeeAmount(long amount) {
        return Math.round(amount * (platformFeePercentage / 100.0));
    }
}