package eu.accesa.blinkpay.bank.model;

public enum AccountType {
    /** Individual consumer — has a phone alias, bank + Digital Euro balance. */
    CONSUMER,

    /** Merchant / retailer — no phone alias, receives payments only, starts at €0. */
    MERCHANT
}
