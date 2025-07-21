package com.etna.gpe.ms_payment_api.controllers;

import com.etna.gpe.ms_payment_api.dto.CheckoutRequestDto;
import com.etna.gpe.ms_payment_api.dto.RefundRequestDto;
import com.etna.gpe.ms_payment_api.entity.Payment;
import com.etna.gpe.ms_payment_api.services.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Contrôleur REST pour gérer les paiements Stripe Connect.
 */
@RestController
@RequestMapping("/payments")
@Slf4j
public class PaymentController implements IPaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Endpoint pour créer une session de paiement Stripe Checkout.
     * @param dto Données de la requête de paiement
     * @return URL de la session Checkout
     */
    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> createCheckout(@RequestBody CheckoutRequestDto dto) {
        log.info("Creating checkout session for user: {}, shop: {}", dto.getUserId(), dto.getShopId());

        String checkoutUrl = paymentService.createCheckoutSession(
            dto.getUserId(),
            dto.getShopId(),
            dto.getAppointmentId(),
            dto.getServiceId(),
            dto.getAmount(),
            dto.getCurrency()
        );

        return ResponseEntity.ok(Map.of("checkout_url", checkoutUrl));
    }

    /**
     * Endpoint optionnel pour initier un remboursement depuis l'application.
     * @param paymentId ID du paiement à rembourser
     * @param dto Données du remboursement
     * @return Le paiement mis à jour
     */
    @PostMapping("/refund/{paymentId}")
    public ResponseEntity<Payment> refundPayment(@PathVariable UUID paymentId, @RequestBody RefundRequestDto dto) {
        log.info("Initiating refund for payment: {}", paymentId);

        Payment refundedPayment = paymentService.refundPayment(paymentId, dto.getAmount());
        return ResponseEntity.ok(refundedPayment);
    }

    /**
     * Endpoint pour récupérer un paiement par son ID.
     * @param paymentId ID du paiement
     * @return Le paiement trouvé
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPayment(@PathVariable UUID paymentId) {
        log.info("Getting payment: {}", paymentId);
        Payment payment = paymentService.findById(paymentId);
        return ResponseEntity.ok(payment);
    }
    
    /**
     * Endpoint pour vérifier le statut de paiement d'un rendez-vous.
     * @param appointmentId ID du rendez-vous
     * @return Le paiement associé au rendez-vous ou 404 si aucun paiement n'est trouvé
     */
    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<Payment> getPaymentByAppointment(@PathVariable UUID appointmentId) {
        log.info("Getting payment for appointment: {}", appointmentId);
        
        Payment payment = paymentService.findByAppointmentId(appointmentId);
        
        if (payment != null) {
            return ResponseEntity.ok(payment);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
