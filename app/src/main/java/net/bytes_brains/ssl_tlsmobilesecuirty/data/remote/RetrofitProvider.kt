package net.bytes_brains.ssl_tlsmobilesecuirty.data.remote

import android.content.Context
import android.util.Base64
import android.util.Log
import net.bytes_brains.ssl_tlsmobilesecuirty.R
import net.bytes_brains.ssl_tlsmobilesecuirty.data.utils.Const
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
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
                .client(jjj(context))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            retrofit!!
        }
    }

    private fun loadPemCertificate(context: Context, certResId: Int): Certificate? {
        val certificateFactory: CertificateFactory = CertificateFactory.getInstance("X.509")
            // Open the PEM file from resources
            val pemInputStream: InputStream = context.resources.openRawResource(R.raw.my_cert)
            val pemContent = pemInputStream.bufferedReader().readText()

            // Remove the PEM headers/footers
            val cleanedPem = pemContent.replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replace("\n", "")
                .replace("\r", "")

            // Decode the base64 encoded content
            val decodedBytes = Base64.decode(cleanedPem, Base64.DEFAULT)

            // Generate the certificate from the decoded bytes
            return certificateFactory.generateCertificate(decodedBytes.inputStream())

    }

    private fun getSecureOkHttpClient2(context: Context): OkHttpClient {

            // Load the .pem certificate from res/raw
            val certificateFactory: CertificateFactory = CertificateFactory.getInstance("X.509")
            val certificate: Certificate=loadPemCertificate(context,R.raw.my_cert2)!!
//            context.resources.openRawResource(R.raw.my_cert2).use { inputStream ->
//                certificate = certificateFactory.generateCertificate(inputStream)
//            }


            // Create a KeyStore and add the certificate
            val keyStore: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null, Const.CERT_PASS.toCharArray()) // Initialize with null
                setCertificateEntry("ca", certificate) // Add the certificate
            }

            // Create a TrustManagerFactory and initialize with the KeyStore
            val trustManagerFactory: TrustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            ).apply {
                init(keyStore)
            }

            // Create an SSLContext with the trusted certificates
            val sslContext: SSLContext = SSLContext.getInstance("TLS").apply {
                init(null, trustManagerFactory.trustManagers, null)
            }

            // Build OkHttpClient with the SSLContext
            return OkHttpClient.Builder()
                .sslSocketFactory(
                    sslContext.socketFactory,
                    trustManagerFactory.trustManagers[0] as javax.net.ssl.X509TrustManager
                )
                .build()

    }

    // Configure OkHttpClient with .p12 certificate
    private fun getSecureOkHttpClient(context: Context): OkHttpClient {
        val certificateFactory: CertificateFactory = CertificateFactory.getInstance("X.509")

        // Load the KeyStore with your .p12 file
        val keyStore = KeyStore.getInstance("BKS")

        val certInputStream = context.resources.openRawResource(R.raw.my_cert)
        toLog("certInputStream is ${certInputStream.available()}")
        keyStore.load(certInputStream, Const.CERT_PASS.toCharArray())
        toLog("keyStore is ${keyStore.getCertificate("ca")}")

//        val certificate = certificateFactory.generateCertificate(certInputStream)
//        toLog("certificate is ${certificate.publicKey}")

        val certStream = ByteArrayInputStream(certInputStream.readBytes())
        val certificateFromStream = certificateFactory.generateCertificate(certStream)
        toLog("Certificate from stream: $certificateFromStream")


        // Create a KeyManagerFactory
        val keyManagerFactory =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, Const.CERT_PASS.toCharArray())

        // Create a TrustManagerFactory
        val trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)

        // Get TrustManager
        val trustManagers = trustManagerFactory.trustManagers
        val trustManager = trustManagers[0] as X509TrustManager

        // Initialize SSLContext
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(keyManagerFactory.keyManagers, trustManagers, null)

        // Build OkHttpClient
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(createLoggingInterceptor())
            .build()
    }

    private fun jjj(context: Context):OkHttpClient{
        val clientStore = KeyStore.getInstance("PKCS12")
        val trustInputStream = context.resources.openRawResource(R.raw.certtls)
        val jksKey = context.resources.openRawResource(R.raw.keystore)
        clientStore.load(trustInputStream, Const.CERT_PASS.toCharArray())

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(clientStore, Const.CERT_PASS.toCharArray())
        val kms = kmf.keyManagers

        val trustStore = KeyStore.getInstance("JKS")
        trustStore.load(jksKey, Const.CERT_PASS.toCharArray())

        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(trustStore)
        val tms = tmf.trustManagers

        var sslContext: SSLContext? = null
        sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kms, tms, SecureRandom())


        // Build OkHttpClient
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, tms[0] as X509TrustManager)
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(createLoggingInterceptor())
            .build()
    }
    
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