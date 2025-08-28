package com.etna.gpe.ms_payment_api.services;

import com.etna.gpe.ms_payment_api.config.StripeFeeConfig;
import com.etna.gpe.ms_payment_api.dto.ServiceDto;
import com.etna.gpe.ms_payment_api.entity.Payment;
import com.etna.gpe.ms_payment_api.enums.PaymentStatus;
import com.etna.gpe.ms_payment_api.exceptions.PaymentNotFoundException;
import com.etna.gpe.ms_payment_api.exceptions.StripePaymentException;
import com.etna.gpe.ms_payment_api.repositories.PaymentRepository;
import com.etna.gpe.mycloseshop.security_api.config.JwtTokenUtil;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implémentation du service de paiement utilisant Stripe Connect.
 */
@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";
    public static final String CALLING_MS_SHOP_API_AT = "Calling ms-shop-api at: {}";
    public static final String USING_TOKEN_FOR_MS_SHOP_API = "Using token for ms-shop-api: {}";
    public static final String MS_PAYMENT_API = "ms-payment-api";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${microservices.shop-api.url}")
    private String shopApiUrl;

    @Value("${rabbitmq.exchange.payment}")
    private String paymentExchange;

    @Value("${rabbitmq.routing-keys.payment-completed}")
    private String paymentCompletedRoutingKey;

    @Value("${rabbitmq.routing-keys.payment-refunded}")
    private String paymentRefundedRoutingKey;

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate;
    private final StripeFeeConfig stripeFeeConfig;
    private final JwtTokenUtil jwtTokenUtil;

    /**
     * Récupère les détails d'un service par son ID.
     *
     * @param serviceId ID du service
     * @return Les détails du service ou null si non trouvé
     */
    private ServiceDto getServiceDetailsById(UUID serviceId) {
        log.info("Retrieving service details for service: {}", serviceId);

        try {
            String url = shopApiUrl + "/service/" + serviceId;
            log.debug(CALLING_MS_SHOP_API_AT, url);

            // Ajouter le token JWT pour l'authentification
            String tokenMs = jwtTokenUtil.generateTokenForMsWith(MS_PAYMENT_API, UUID.randomUUID(), List.of(ROLE_ADMIN));

            log.debug(USING_TOKEN_FOR_MS_SHOP_API, tokenMs);

            // Ajouter le token dans les headers de la requête
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set(AUTHORIZATION, BEARER + tokenMs);
            org.springframework.http.HttpEntity<Void> requestEntity = new org.springframework.http.HttpEntity<>(headers);

            ResponseEntity<ServiceDto> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, requestEntity, ServiceDto.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Service details retrieved successfully for service: {}", serviceId);
                return response.getBody();
            } else {
                log.warn("Failed to retrieve service details for service: {}", serviceId);
                return null;
            }
        } catch (Exception e) {
            log.error("Error retrieving service details for service: {}", serviceId, e);
            return null;
        }
    }

    /**
     * Met à jour le statut d'un rendez-vous à "confirmé" après paiement.
     *
     * @param appointmentId ID du rendez-vous
     * @return true si la mise à jour a réussi, false sinon
     */
    private boolean paidAppointment(UUID appointmentId) {
        if (appointmentId == null) {
            log.warn("Cannot set paid appointment: appointmentId is null");
            return false;
        }

        log.info("Set paid appointment: {}", appointmentId);

        try {
            String url = shopApiUrl + "/appointment/paid/" + appointmentId;
            log.debug(CALLING_MS_SHOP_API_AT, url);

            // Ajouter le token JWT pour l'authentification
            String tokenMs = jwtTokenUtil.generateTokenForMsWith(MS_PAYMENT_API, UUID.randomUUID(), List.of(ROLE_ADMIN));
            log.debug(USING_TOKEN_FOR_MS_SHOP_API, tokenMs);


            // Ajouter le token dans les headers de la requête
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set(AUTHORIZATION, BEARER + tokenMs);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            org.springframework.http.HttpEntity<Map<String, Object>> requestEntity = new org.springframework.http.HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, Object.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Appointment confirmed successfully: {}", appointmentId);
                return true;
            } else {
                log.warn("Failed to confirm appointment: {}", appointmentId);
                return false;
            }
        } catch (Exception e) {
            log.error("Error confirming appointment: {}", appointmentId, e);
            return false;
        }
    }

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository, RabbitTemplate rabbitTemplate, 
                             RestTemplate restTemplate, StripeFeeConfig stripeFeeConfig, 
                             JwtTokenUtil jwtTokenUtil) {
        this.paymentRepository = paymentRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.restTemplate = restTemplate;
        this.stripeFeeConfig = stripeFeeConfig;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @PostConstruct
    public void init() {
        if (stripeApiKey != null && !stripeApiKey.isEmpty()) {
            Stripe.apiKey = stripeApiKey;
            log.info("Stripe API initialized");
        } else {
            log.warn("Stripe API key not configured");
        }
    }

    @Override
    public String createCheckoutSession(UUID userId, UUID shopId, UUID appointmentId, UUID serviceId, long amount, String currency) {
        log.info("Creating Stripe checkout session for user: {}, shop: {}, service: {}, amount: {}", userId, shopId, serviceId, amount);

        // (1) Créer Payment en base, statut PENDING
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setUserId(userId);
        payment.setShopId(shopId);
        payment.setAppointmentId(appointmentId);
        payment.setServiceId(serviceId);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // (2) Récupérer les détails du service
        String productName = "Paiement boutique - " + shopId.toString().substring(0, 8);

        // Récupérer les détails du service si disponible
        if (serviceId != null) {
            ServiceDto serviceDetails = getServiceDetailsById(serviceId);
            if (serviceDetails != null) {
                productName = "Service: " + serviceDetails.getName();
                if (serviceDetails.getDescription() != null && !serviceDetails.getDescription().isEmpty()) {
                    productName += " - " + serviceDetails.getDescription();
                }
            }
        }

        // (3) Créer la session Checkout avec les détails du service
        SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(baseUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(baseUrl + "/payment/cancel")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(currency)
                                                .setUnitAmount(amount)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(productName)
                                                                .build()
                                                ).build()
                                ).build()
                );

        // Récupérer l'ID du compte Stripe du shop
        String stripeAccountId = getStripeAccountIdForShop(shopId);
        if (stripeAccountId == null || stripeAccountId.isEmpty() || stripeAccountId.startsWith("acct_placeholder")) {
            log.info("No valid Stripe account found for shop: {} - Payment will go to platform account", shopId);
        } else {
            log.info("Found Stripe account for shop: {}, account ID: {}", shopId, stripeAccountId);
        }

        try {
            Session session;

            // Essayer d'abord une charge directe si un compte Stripe valide est disponible
            if (stripeAccountId != null && !stripeAccountId.isEmpty() && !stripeAccountId.startsWith("acct_placeholder")) {
                try {
                    log.info("Attempting direct charge to connected account: {}", stripeAccountId);
                    
                    // Calculer les frais d'application (commission de la plateforme)
                    long applicationFeeAmount = stripeFeeConfig.calculateFeeAmount(amount);
                    log.info("Calculated application fee: {} ({}% of {})", 
                            applicationFeeAmount, stripeFeeConfig.getPlatformFeePercentage(), amount);
                    
                    // Ajouter les frais d'application aux paramètres de la session
                    paramsBuilder.setPaymentIntentData(
                            SessionCreateParams.PaymentIntentData.builder()
                                    .setApplicationFeeAmount(applicationFeeAmount)
                                    .build()
                    );

                    // Créer des options de requête avec le paramètre stripeAccount pour une charge directe
                    RequestOptions requestOptions = RequestOptions.builder()
                            .setStripeAccount(stripeAccountId)
                            .build();

                    // Créer la session avec les options de requête pour une charge directe
                    session = Session.create(paramsBuilder.build(), requestOptions);
                    log.info("Direct charge with application fee successful to connected account: {}", stripeAccountId);
                } catch (StripeException e) {
                    log.warn("Direct charge failed, falling back to platform payment: {}", e.getMessage());
                    log.debug("Direct charge failure details: {}", e.toString());
                    
                    // Si la charge directe échoue, utiliser le compte de la plateforme
                    log.info("Creating checkout session using platform account");
                    session = Session.create(paramsBuilder.build());
                }
            } else {
                // Utiliser le compte de la plateforme pour le paiement
                log.info("Creating checkout session using platform account");
                session = Session.create(paramsBuilder.build());
            }

            // (4) Mettre à jour la session et PaymentIntent en BD
            payment.setStripeSessionId(session.getId());
            payment.setStripePaymentIntentId(session.getPaymentIntent());
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            log.info("Stripe checkout session created: {}", session.getId());
            return session.getUrl();
        } catch (StripeException e) {
            log.error("Error creating Stripe session", e);
            throw new StripePaymentException("Erreur lors de la création de la session Stripe", e);
        }
    }

    @Override
    public Payment handleCheckoutCompleted(String sessionId) {
        log.info("Handling checkout completed for session: {}", sessionId);

        Payment payment = paymentRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment non trouvé pour la session " + sessionId));

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Confirmer le rendez-vous si présent
        if (payment.getAppointmentId() != null) {
            boolean appointmentConfirmed = paidAppointment(payment.getAppointmentId());
            if (appointmentConfirmed) {
                log.info("Appointment confirmed for payment: {}, appointment: {}",
                        payment.getId(), payment.getAppointmentId());
            } else {
                log.warn("Failed to confirm appointment for payment: {}, appointment: {}",
                        payment.getId(), payment.getAppointmentId());
            }
        }

        // Publier événement RabbitMQ payment.completed
        rabbitTemplate.convertAndSend(paymentExchange, paymentCompletedRoutingKey, payment);
        log.info("Payment completed and event published for payment: {}", payment.getId());

        return payment;
    }

    @Override
    public Payment handlePaymentRefunded(String paymentIntentId) {
        log.info("Handling payment refunded for PaymentIntent: {}", paymentIntentId);

        Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment non trouvé pour PaymentIntent " + paymentIntentId));

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Publier événement RabbitMQ payment.refunded
        rabbitTemplate.convertAndSend(paymentExchange, paymentRefundedRoutingKey, payment);
        log.info("Payment refunded and event published for payment: {}", payment.getId());

        return payment;
    }

    @Override
    public Payment refundPayment(UUID paymentId, long amount) {
        log.info("Initiating refund for payment: {}, amount: {}", paymentId, amount);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment non trouvé"));

        try {
            RefundCreateParams refundParams = RefundCreateParams.builder()
                    .setPaymentIntent(payment.getStripePaymentIntentId())
                    .setAmount(amount)
                    .build();

            Refund refund = Refund.create(refundParams);
            log.info("Stripe refund created: {}", refund.getId());

            // Le webhook Stripe générera ensuite l'événement pour mettre à jour le statut
            return payment;
        } catch (StripeException e) {
            log.error("Error creating Stripe refund", e);
            throw new StripePaymentException("Impossible d'effectuer le remboursement Stripe", e);
        }
    }

    @Override
    public String getStripeAccountIdForShop(UUID shopId) {
        log.info("Retrieving Stripe account ID for shop: {}", shopId);

        try {
            // Appel HTTP vers ms-shop-api pour récupérer l'account ID Stripe du shop
            String url = shopApiUrl + "/shop/" + shopId + "/stripe-account";
            log.debug(CALLING_MS_SHOP_API_AT, url);

            String tokenMs = jwtTokenUtil.generateTokenForMsWith(MS_PAYMENT_API, UUID.randomUUID(), List.of(ROLE_ADMIN));

            // Add authorization header with bearer token
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set(AUTHORIZATION, BEARER + tokenMs);
            org.springframework.http.HttpEntity<Void> requestEntity = new org.springframework.http.HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, requestEntity, String.class);
            String stripeAccountId = response.getBody();

            if (stripeAccountId != null && !stripeAccountId.isEmpty()) {
                log.info("Retrieved Stripe account ID: {} for shop: {}", stripeAccountId, shopId);
                return stripeAccountId;
            } else {
                log.info("No Stripe account configured for shop: {}", shopId);
                return null;
            }

        } catch (Exception e) {
            log.warn("Could not retrieve Stripe account ID for shop: {} - Error: {}", shopId, e.getMessage());
            // Si ms-shop-api n'est pas disponible ou le shop n'a pas de compte Stripe,
            // on continue avec un paiement simple (vers le compte principal)
            return null;
        }
    }

    public Payment findById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment non trouvé avec l'ID: " + paymentId));
    }

    @Override
    public Payment findByAppointmentId(UUID appointmentId) {
        log.info("Finding payment by appointment ID: {}", appointmentId);
        return paymentRepository.findByAppointmentId(appointmentId)
                .orElse(null);
    }
}
