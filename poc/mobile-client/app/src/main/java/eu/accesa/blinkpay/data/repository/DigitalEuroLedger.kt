package eu.accesa.blinkpay.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal

data class OfflineTransfer(
    val counterpartyName: String,
    val counterpartyIban: String,
    val amount: BigDecimal,
    val isSend: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)

object DigitalEuroLedger {

    private val _balance = MutableStateFlow(BigDecimal.ZERO)
    val balance: StateFlow<BigDecimal> = _balance

    private val _transfers = MutableStateFlow<List<OfflineTransfer>>(emptyList())
    val transfers: StateFlow<List<OfflineTransfer>> = _transfers

    fun initialize(balance: BigDecimal) {
        _balance.value = balance
    }

    fun canDebit(amount: BigDecimal): Boolean = _balance.value >= amount

    fun debit(amount: BigDecimal, counterpartyName: String, counterpartyIban: String): Boolean {
        val current = _balance.value
        if (current < amount) return false
        _balance.value = current - amount
        _transfers.value = _transfers.value + OfflineTransfer(
            counterpartyName = counterpartyName,
            counterpartyIban = counterpartyIban,
            amount = amount,
            isSend = true,
        )
        return true
    }

    fun credit(amount: BigDecimal, counterpartyName: String, counterpartyIban: String) {
        _balance.value = _balance.value + amount
        _transfers.value = _transfers.value + OfflineTransfer(
            counterpartyName = counterpartyName,
            counterpartyIban = counterpartyIban,
            amount = amount,
            isSend = false,
        )
    }
}
