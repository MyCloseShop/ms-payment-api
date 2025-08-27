package com.etna.gpe.ms_payment_api.exceptions;

/**
 * Exception levée quand un paiement n'est pas trouvé.
 */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String message) {
        super(message);
    }
}
