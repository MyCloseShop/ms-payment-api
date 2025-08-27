package com.etna.gpe.ms_payment_api.exceptions;

/**
 * Exception levée lors d'erreurs avec l'API Stripe.
 */
public class StripePaymentException extends RuntimeException {

    public StripePaymentException(String message) {
        super(message);
    }

    public StripePaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
