package net.bytes_brains.ssl_tlsmobilesecuirty.data.remote

import android.content.Context
import android.util.Log
import net.bytes_brains.ssl_tlsmobilesecuirty.data.utils.Const
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


class RetrofitProvider {
    private var retrofit: Retrofit? = null

    private fun getInstance(context: Context): Retrofit {
        return if (retrofit != null)
            retrofit!!
        else {
            retrofit = Retrofit.Builder()
                .baseUrl(Const.BASE_URL)
                .client(buildOkHttp(context))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            retrofit!!
        }
    }

    private fun buildOkHttp(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(createLoggingInterceptor())
//            .addSSLSocketFactoryForPreInit(context,true)
//            .certificatePinner(certificatePinner)

//            .certificatePinner(addingCertificatePinning())
            .build()
    }

    private fun OkHttpClient.Builder.addSSLSocketFactory(
        context: Context,
        isSSlPinningEnabled: Boolean
    ) = apply {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
        }

        context.assets.open("ca_bundle.txt").use { inputStream ->
            // Split the bundle into individual certificates based on the PEM delimiters
            val certificates = inputStream.bufferedReader().use { it.readText() }
                .split("-----END CERTIFICATE-----")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { it + "\n-----END CERTIFICATE-----" } // Re-add the end line for parsing

            certificates.forEachIndexed { index, certificatePEM ->
                val certInputStream = certificatePEM.byteInputStream()
                try {
                    val certificate = certificateFactory.generateCertificate(certInputStream)
                    val alias = "ca_cert_$index"
                    keyStore.setCertificateEntry(alias, certificate)
                } catch (e: Exception) {
                    // Handle parsing error, log or skip
                    println("Error parsing certificate at index $index: ${e.message}")
                }
            }
        }
        // Initialize TrustManager with the loaded certificates
        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        ).apply {
            if (isSSlPinningEnabled) {
                init(keyStore)
            } else {
                init(null as KeyStore?)
            }
        }
        val trustManager = trustManagerFactory.trustManagers[0] as X509TrustManager

        // Create an SSLContext with the TrustManager
        val sslContext = SSLContext.getInstance("TLSv1.3").apply {
            init(null, arrayOf(trustManager), SecureRandom())
        }
        sslSocketFactory(sslContext.socketFactory, trustManager)
    }

    private val certificatePinner = CertificatePinner.Builder()
        .add("alphabill.org", "sha256/g8XL/6X37eu0/q09TCQovj9kFaHebuoGYLsAi8Xurjw=") // Replace with actual SHA256 fingerprint
        .build()

    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Log the request and response body
        }
    }

    fun initApiService(context: Context): TLSApiService {
        return getInstance(context).create(TLSApiService::class.java)
    }


    companion object {
        fun toLog(message: String? = null) {
            Log.e(RetrofitProvider.javaClass.name, "toLog: message= $message")
        }
    }

}