package com.etna.gpe.ms_payment_api.services;

import com.etna.gpe.ms_payment_api.entity.ShopStripeAccount;
import com.etna.gpe.ms_payment_api.repositories.ShopStripeAccountRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeConnectService {

    private final ShopStripeAccountRepository shopStripeAccountRepository;

    /**
     * Créer un compte Stripe Connect pour un shop (approche simple - Express Account)
     */
    @Transactional
    public ShopStripeAccount createStripeAccount(UUID shopId) throws StripeException {
        log.info("Création d'un compte Stripe Connect pour le shop: {}", shopId);

        // Vérifier si le shop a déjà un compte Stripe
        Optional<ShopStripeAccount> existingAccount = shopStripeAccountRepository.findByShopId(shopId);
        if (existingAccount.isPresent()) {
            log.warn("Le shop {} a déjà un compte Stripe: {}", shopId, existingAccount.get().getStripeAccountId());
            return existingAccount.get();
        }

        // Créer un compte Express Stripe Connect (le plus simple pour MVP)
        AccountCreateParams params = AccountCreateParams.builder()
                .setType(AccountCreateParams.Type.EXPRESS)
                .setCountry("FR") // France pour EUR
                .setDefaultCurrency("eur")
                .build();

        Account stripeAccount = Account.create(params);

        // Sauvegarder en base
        ShopStripeAccount shopStripeAccount = ShopStripeAccount.builder()
                .shopId(shopId)
                .stripeAccountId(stripeAccount.getId())
                .onboardingCompleted(false)
                .chargesEnabled(false)
                .payoutsEnabled(false)
                .build();

        return shopStripeAccountRepository.save(shopStripeAccount);
    }

    /**
     * Générer un lien d'onboarding pour que le shop configure son compte Stripe
     */
    public String createOnboardingLink(UUID shopId, String returnUrl, String refreshUrl) throws StripeException {
        log.info("Génération du lien d'onboarding pour le shop: {}", shopId);

        ShopStripeAccount shopAccount = shopStripeAccountRepository.findByShopId(shopId)
                .orElseThrow(() -> new RuntimeException("Aucun compte Stripe trouvé pour le shop: " + shopId));

        AccountLinkCreateParams params = AccountLinkCreateParams.builder()
                .setAccount(shopAccount.getStripeAccountId())
                .setRefreshUrl(refreshUrl)
                .setReturnUrl(returnUrl)
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .build();

        AccountLink accountLink = AccountLink.create(params);
        return accountLink.getUrl();
    }

    /**
     * Mettre à jour le statut du compte Stripe après onboarding
     */
    @Transactional
    public void updateAccountStatus(String stripeAccountId) throws StripeException {
        log.info("Mise à jour du statut du compte Stripe: {}", stripeAccountId);

        Account account = Account.retrieve(stripeAccountId);

        ShopStripeAccount shopAccount = shopStripeAccountRepository.findByStripeAccountId(stripeAccountId)
                .orElseThrow(() -> new RuntimeException("Compte Stripe non trouvé: " + stripeAccountId));

        shopAccount.setChargesEnabled(account.getChargesEnabled());
        shopAccount.setPayoutsEnabled(account.getPayoutsEnabled());
        shopAccount.setOnboardingCompleted(account.getDetailsSubmitted());

        shopStripeAccountRepository.save(shopAccount);
    }

    /**
     * Récupérer le compte Stripe d'un shop
     */
    public Optional<ShopStripeAccount> getShopStripeAccount(UUID shopId) {
        return shopStripeAccountRepository.findByShopId(shopId);
    }

    /**
     * Vérifier si un shop peut recevoir des paiements
     */
    public boolean canReceivePayments(UUID shopId) {
        return shopStripeAccountRepository.findByShopId(shopId)
                .map(account -> account.getChargesEnabled() && account.getOnboardingCompleted())
                .orElse(false);
    }
}
