package com.etna.gpe.ms_payment_api.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "stripe")
@Data
@Slf4j
public class StripeConfig {

    private String mode;
    private String publicKey;
    private String secretKey;
    private String webhookSecret;
    private String successUrl;
    private String cancelUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;

        // Validation du mode
        if (mode != null && mode.equals("test")) {
            if (!secretKey.startsWith("sk_test_")) {
                throw new IllegalArgumentException("Mode TEST activé mais clé secrète de PRODUCTION détectée ! Vérifiez vos variables d'environnement.");
            }
            log.warn("🧪 MODE TEST STRIPE ACTIVÉ - Aucune vraie transaction ne sera effectuée");
        } else if (mode != null && mode.equals("live")) {
            if (!secretKey.startsWith("sk_live_")) {
                throw new IllegalArgumentException("Mode LIVE activé mais clé secrète de TEST détectée ! Vérifiez vos variables d'environnement.");
            }
            log.warn("⚡ MODE LIVE STRIPE ACTIVÉ - VRAIES TRANSACTIONS ACTIVÉES !");
        }
    }

    public boolean isTestMode() {
        return "test".equals(mode);
    }
}
