package com.etna.gpe.ms_payment_api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreatePaymentRequest {

    @NotNull(message = "Shop ID est requis")
    @org.hibernate.validator.constraints.UUID
    private UUID shopId;

    @NotNull(message = "Le montant est requis")
    @Positive(message = "Le montant doit être positif")
    private BigDecimal amount;

    @NotNull(message = "Le taux de commission est requis")
    @Positive(message = "Le taux de commission doit être positif")
    private BigDecimal commissionRate;

    private String description;

    private String successUrl;

    private String cancelUrl;
}
