package com.etna.gpe.ms_payment_api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PaymentResponse {

    private UUID paymentId;
    private String checkoutUrl;
    private String stripeCheckoutSessionId;
    private String status;
    private String message;
}
