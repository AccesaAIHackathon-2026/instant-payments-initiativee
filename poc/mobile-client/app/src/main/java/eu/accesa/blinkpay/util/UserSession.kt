package eu.accesa.blinkpay.util

import android.content.Context
import android.content.SharedPreferences

object UserSession {

    private const val PREFS_NAME = "user_session"
    private const val KEY_IBAN = "iban"
    private const val KEY_HOLDER_NAME = "holderName"
    private const val KEY_PHONE_ALIAS = "phoneAlias"
    private const val KEY_WALLET_ID = "walletId"
    private const val KEY_BANK_BASE_URL = "bankBaseUrl"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val isRegistered: Boolean get() = prefs.contains(KEY_IBAN)

    val iban: String get() = prefs.getString(KEY_IBAN, "") ?: ""
    val holderName: String get() = prefs.getString(KEY_HOLDER_NAME, "") ?: ""
    val phoneAlias: String? get() = prefs.getString(KEY_PHONE_ALIAS, null)
    val walletId: String? get() = prefs.getString(KEY_WALLET_ID, null)
    val bankBaseUrl: String get() = prefs.getString(KEY_BANK_BASE_URL, "") ?: ""

    fun save(
        iban: String,
        holderName: String,
        phoneAlias: String?,
        walletId: String?,
        bankBaseUrl: String,
    ) {
        prefs.edit()
            .putString(KEY_IBAN, iban)
            .putString(KEY_HOLDER_NAME, holderName)
            .putString(KEY_PHONE_ALIAS, phoneAlias)
            .putString(KEY_WALLET_ID, walletId)
            .putString(KEY_BANK_BASE_URL, bankBaseUrl)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
