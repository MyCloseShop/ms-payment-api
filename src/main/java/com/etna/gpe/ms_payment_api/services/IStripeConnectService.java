package com.etna.gpe.ms_payment_api.services;

import java.util.UUID;

/**
 * Interface pour gérer l'onboarding des shops avec Stripe Connect.
 */
public interface IStripeConnectService {

    /**
     * Crée un compte Stripe Connect Standard pour un shop.
     * @param shopId ID du shop
     * @return L'ID du compte Stripe connecté créé
     */
    String createConnectedAccount(UUID shopId);

    /**
     * Génère un Account Link pour permettre au shop de compléter son onboarding Stripe.
     * @param accountId ID du compte Stripe connecté
     * @return URL du lien d'onboarding à transmettre au shop
     */
    String createAccountLink(String accountId);

    /**
     * Crée un compte connecté et génère immédiatement le lien d'onboarding.
     * @param shopId ID du shop
     * @return URL d'onboarding à transmettre au shop
     */
    String onboardShop(UUID shopId);
}