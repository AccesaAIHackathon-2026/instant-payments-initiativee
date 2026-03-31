package eu.accesa.blinkpay.bank.model;

/**
 * Verification of Payee result per EU Regulation 2024/886.
 *
 * MATCH      — IBAN and name match exactly (or differ only in case/whitespace)
 * CLOSE_MATCH — name is similar but not exact (UI must warn the payer)
 * NO_MATCH   — IBAN exists but name does not match (UI must block payment)
 * NOT_FOUND  — IBAN is unknown to this PSP
 */
public enum VopResult {
    MATCH, CLOSE_MATCH, NO_MATCH, NOT_FOUND
}
