package com.etna.gpe.ms_payment_api.services;

import com.etna.gpe.mycloseshop.security_api.config.JwtTokenUtil;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service pour gérer l'onboarding des shops avec Stripe Connect (comptes Standard).
 */
@Service
@Slf4j
public class StripeConnectService implements IStripeConnectService {

    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";

    @Value("${app.base-url}")
    private String baseUrl;
    
    @Value("${microservices.shop-api.url}")
    private String shopApiUrl;

    private final JwtTokenUtil jwtTokenUtil;
    
    private final RestTemplate restTemplate;
    
    @Autowired
    public StripeConnectService(RestTemplate restTemplate, JwtTokenUtil jwtTokenUtil) {
        this.restTemplate = restTemplate;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * Crée un compte Stripe Connect Standard pour un shop.
     * @param shopId ID du shop
     * @return L'ID du compte Stripe connecté créé
     */
    public String createConnectedAccount(UUID shopId) {
        log.info("Creating Stripe connected account for shop: {}", shopId);

        try {
            AccountCreateParams params = AccountCreateParams.builder()
                .setType(AccountCreateParams.Type.STANDARD)
                .build();

            Account account = Account.create(params);
            String accountId = account.getId();

            log.info("Stripe connected account created: {} for shop: {}", accountId, shopId);
            // TODO: Stocker accountId associé à ce shop en BD ou via un service Shop

            return accountId;
        } catch (StripeException e) {
            log.error("Error creating Stripe connected account for shop: {}", shopId, e);
            throw new RuntimeException("Erreur lors de la création du compte Stripe connecté", e);
        }
    }

    /**
     * Génère un Account Link pour permettre au shop de compléter son onboarding Stripe.
     * @param accountId ID du compte Stripe connecté
     * @return URL du lien d'onboarding à transmettre au shop
     */
    public String createAccountLink(String accountId) {
        log.info("Creating account link for Stripe account: {}", accountId);

        try {
            AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                .setAccount(accountId)
                .setRefreshUrl(baseUrl + "/stripe/reauth")
                .setReturnUrl(baseUrl + "/stripe/return")
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .build();

            AccountLink link = AccountLink.create(linkParams);
            String onboardingUrl = link.getUrl();

            log.info("Account link created for account: {}", accountId);
            return onboardingUrl;
        } catch (StripeException e) {
            log.error("Error creating account link for account: {}", accountId, e);
            throw new RuntimeException("Erreur lors de la création du lien d'onboarding Stripe", e);
        }
    }

    /**
     * Met à jour l'ID du compte Stripe connecté pour un shop.
     * @param shopId ID du shop
     * @param stripeAccountId ID du compte Stripe connecté
     */
    private void updateShopStripeAccount(UUID shopId, String stripeAccountId) {
        log.info("Updating Stripe account ID for shop: {}", shopId);
        
        try {
            String url = shopApiUrl + "/shop/" + shopId + "/stripe-account";
            log.debug("Calling ms-shop-api at: {}", url);
            
            // Générer un token JWT pour l'authentification
            String tokenMs = jwtTokenUtil.generateTokenForMsWith("ms-payment-api", UUID.randomUUID(), List.of("ROLE_ADMIN"));
            log.debug("Using token for ms-shop-api: {}", tokenMs);
            
            // Préparer les headers avec le token d'authentification
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(AUTHORIZATION, BEARER + tokenMs);
            
            // Préparer le corps de la requête
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("stripeAccountId", stripeAccountId);
            log.debug("Updating Stripe account ID for shop: {} with Stripe account ID: {}", shopId, stripeAccountId);
            
            // Créer l'entité de la requête
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            // Faire l'appel API avec exchange au lieu de postForObject
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                requestEntity,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Shop Stripe account ID updated successfully: {}", response.getBody());
            } else {
                log.warn("Failed to update Stripe account ID for shop: {}. Status code: {}", shopId, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error updating Stripe account ID for shop: {}", shopId, e);
            // On continue même en cas d'erreur pour permettre l'onboarding
        }
    }

    /**
     * Crée un compte connecté et génère immédiatement le lien d'onboarding.
     * @param shopId ID du shop
     * @return URL d'onboarding à transmettre au shop
     */
    public String onboardShop(UUID shopId) {
        String accountId = createConnectedAccount(shopId);
        
        // Mettre à jour l'ID du compte Stripe dans le shop
        updateShopStripeAccount(shopId, accountId);
        
        return createAccountLink(accountId);
    }
}
