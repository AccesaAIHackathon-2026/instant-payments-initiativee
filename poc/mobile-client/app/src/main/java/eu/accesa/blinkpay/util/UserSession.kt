package eu.accesa.blinkpay.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists the registered user's identity across sessions.
 *
 * KYC note: only name, phone alias, and bank-assigned IBAN are stored here.
 * Sensitive identity data (national ID, DOB, address) is never collected by
 * the app — in production those flow through eIDAS 2.0 / EUDI Wallet attestations
 * directly to the PSP, never touching the mobile client.
 */
object UserSession {

    private const val PREFS_NAME = "user_session"
    private const val KEY_IBAN = "iban"
    private const val KEY_HOLDER_NAME = "holderName"
    private const val KEY_PHONE_ALIAS = "phoneAlias"
    private const val KEY_WALLET_ID = "walletId"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val isRegistered: Boolean get() = prefs.contains(KEY_IBAN)

    val iban: String get() = prefs.getString(KEY_IBAN, null)!!
    val holderName: String get() = prefs.getString(KEY_HOLDER_NAME, null)!!
    val phoneAlias: String? get() = prefs.getString(KEY_PHONE_ALIAS, null)
    val walletId: String? get() = prefs.getString(KEY_WALLET_ID, null)

    fun save(iban: String, holderName: String, phoneAlias: String?, walletId: String?) {
        prefs.edit()
            .putString(KEY_IBAN, iban)
            .putString(KEY_HOLDER_NAME, holderName)
            .putString(KEY_PHONE_ALIAS, phoneAlias)
            .putString(KEY_WALLET_ID, walletId)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
