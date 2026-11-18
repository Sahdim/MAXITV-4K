package com.example.maxitv

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class HttpErr(val code: Int) : IOException("HTTP $code")

/**
 * Réseau de l'appli. Certificats vérifiés normalement partout. Seul le serveur IPTV saisi par l'utilisateur
 * à la connexion peut avoir un certificat auto-signé (fréquent chez les fournisseurs IPTV) : l'exception ne vaut
 * que pour ce nom exact. Les redirections sont suivies à la main, avec le bon client pour chaque adresse,
 * pour qu'une redirection ne transmette jamais l'exception à un autre serveur.
 */
object Net {
    const val UA = "Mozilla/5.0 (Linux; Android 9; AFTMM) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    @Volatile
    private var trusted = ""

    fun trust(host: String) {
        trusted = host.trim().lowercase()
    }

    private val strict: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    private val lenient: OkHttpClient by lazy {
        val tm = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val ctx = SSLContext.getInstance("TLS")
        ctx.init(null, arrayOf<TrustManager>(tm), null)
        strict.newBuilder()
            .sslSocketFactory(ctx.socketFactory, tm)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    fun clientFor(url: HttpUrl): OkHttpClient =
        if (url.isHttps && url.host.lowercase() == trusted) lenient else strict

    /** Télécharge une adresse (appel bloquant : à lancer hors du fil principal). */
    fun fetchString(start: HttpUrl): String {
        var url = start
        for (hop in 0..3) {
            val req = Request.Builder().url(url).header("User-Agent", UA).build()
            clientFor(url).newCall(req).execute().use { r ->
                if (r.isRedirect) {
                    val next = r.header("Location")?.let { url.resolve(it) } ?: throw IOException("redirection invalide")
                    url = next
                } else {
                    if (!r.isSuccessful) throw HttpErr(r.code)
                    return r.body?.string() ?: ""
                }
            }
        }
        throw IOException("trop de redirections")
    }
}
