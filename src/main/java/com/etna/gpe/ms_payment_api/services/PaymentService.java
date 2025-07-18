package com.etna.gpe.ms_payment_api.services;

import com.etna.gpe.ms_payment_api.dto.CreatePaymentRequest;
import com.etna.gpe.ms_payment_api.dto.PaymentResponse;
import com.etna.gpe.ms_payment_api.entity.Payment;
import com.etna.gpe.ms_payment_api.entity.ShopStripeAccount;
import com.etna.gpe.ms_payment_api.enums.PaymentStatus;
import com.etna.gpe.ms_payment_api.repositories.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StripeConnectService stripeConnectService;
    private final RabbitMQService rabbitMQService;

    /**
     * Créer un paiement avec Stripe Checkout et commission
     */
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, UUID userId) throws StripeException {
        log.info("Création d'un paiement pour l'utilisateur {} vers le shop {}", userId, request.getShopId());

        // Vérifier que le shop peut recevoir des paiements
        ShopStripeAccount shopAccount = stripeConnectService.getShopStripeAccount(request.getShopId())
                .orElseThrow(() -> new RuntimeException("Shop n'a pas de compte Stripe configuré"));

        if (!stripeConnectService.canReceivePayments(request.getShopId())) {
            throw new RuntimeException("Le shop ne peut pas encore recevoir de paiements. Onboarding Stripe incomplet.");
        }

        // Calculer la commission en centimes
        BigDecimal commissionAmount = request.getAmount()
                .multiply(request.getCommissionRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        Long commissionInCents = commissionAmount.multiply(BigDecimal.valueOf(100)).longValue();
        Long amountInCents = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

        // Créer la session Stripe Checkout avec direct charge sur le compte du shop
        SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(request.getSuccessUrl())
                .setCancelUrl(request.getCancelUrl())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("eur")
                                .setUnitAmount(amountInCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(request.getDescription() != null ? request.getDescription() : "Réservation")
                                        .build())
                                .build())
                        .setQuantity(1L)
                        .build())
                .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                        .setApplicationFeeAmount(commissionInCents) // Commission de la plateforme
                        .build());

        // Utiliser le compte Stripe du shop pour le paiement direct
        Session session = Session.create(sessionBuilder.build(),
                com.stripe.net.RequestOptions.builder()
                        .setStripeAccount(shopAccount.getStripeAccountId())
                        .build());

        // Créer l'entité Payment en base
        Payment payment = Payment.builder()
                .stripePaymentIntentId(session.getPaymentIntent())
                .stripeCheckoutSessionId(session.getId())
                .userId(userId)
                .shopId(request.getShopId())
                .amount(request.getAmount())
                .commissionAmount(commissionAmount)
                .commissionRate(request.getCommissionRate())
                .currency("EUR")
                .status(PaymentStatus.PENDING)
                .description(request.getDescription())
                .build();

        payment = paymentRepository.save(payment);

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .checkoutUrl(session.getUrl())
                .stripeCheckoutSessionId(session.getId())
                .status("PENDING")
                .message("Paiement créé avec succès")
                .build();
    }

    /**
     * Confirmer un paiement après succès Stripe (appelé par webhook)
     */
    @Transactional
    public void confirmPayment(String stripeCheckoutSessionId) {
        log.info("Confirmation du paiement pour la session: {}", stripeCheckoutSessionId);

        Payment payment = paymentRepository.findByStripeCheckoutSessionId(stripeCheckoutSessionId)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé pour la session: " + stripeCheckoutSessionId));

        payment.setStatus(PaymentStatus.COMPLETED);
        paymentRepository.save(payment);

        // Publier un événement RabbitMQ pour notifier les autres microservices
        rabbitMQService.publishPaymentCompleted(payment);

        log.info("Paiement confirmé avec succès: {}", payment.getId());
    }

    /**
     * Vérifier l'éligibilité au remboursement (48h)
     */
    public boolean isRefundEligible(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé: " + paymentId));

        return payment.getStatus().equals(PaymentStatus.COMPLETED) &&
                payment.getRefundEligibleUntil() != null &&
                LocalDateTime.now().isBefore(payment.getRefundEligibleUntil());
    }
}
