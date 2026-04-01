package eu.accesa.blinkpay.data.api

import eu.accesa.blinkpay.BuildConfig
import eu.accesa.blinkpay.util.UserSession
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("X-Api-Key", BuildConfig.BANK_API_KEY)
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private var _bankApi: BankApi? = null
    private var _currentBaseUrl: String? = null

    val bankApi: BankApi
        get() {
            val url = UserSession.bankBaseUrl.ifBlank { BuildConfig.BANK_API_BASE_URL }
            if (_bankApi == null || _currentBaseUrl != url) {
                _currentBaseUrl = url
                _bankApi = Retrofit.Builder()
                    .baseUrl(url)
                    .client(okHttp)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(BankApi::class.java)
            }
            return _bankApi!!
        }

    /** Call after a new PIN login so the next API call uses the new bank URL. */
    fun onSessionChanged() {
        _bankApi = null
        _currentBaseUrl = null
    }
}
