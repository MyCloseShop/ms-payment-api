package com.etna.gpe.ms_payment_api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO pour les requêtes de remboursement.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestDto {
    private long amount; // montant à rembourser en cents
}
