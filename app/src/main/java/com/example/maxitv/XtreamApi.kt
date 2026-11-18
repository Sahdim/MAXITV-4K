package com.example.maxitv

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ApiError(message: String) : Exception(message)

private fun JSONObject.s(key: String): String = if (isNull(key)) "" else optString(key, "").trim()

class XtreamApi(rawHost: String, private val user: String, private val pass: String) {
    private val base: HttpUrl

    init {
        var s = rawHost.trim()
        if (!s.lowercase().startsWith("http")) s = "http://$s"
        s = s.trimEnd('/')
        base = s.toHttpUrlOrNull() ?: throw ApiError("Adresse du serveur invalide.")
        Net.trust(base.host)
    }

    private fun url(action: String?, vararg extra: Pair<String, String>): HttpUrl {
        val b = base.newBuilder()
            .addPathSegment("player_api.php")
            .addQueryParameter("username", user)
            .addQueryParameter("password", pass)
        if (action != null) b.addQueryParameter("action", action)
        for ((k, v) in extra) b.addQueryParameter(k, v)
        return b.build()
    }

    /**
     * Toutes les requêtes passent par une file : 2 à la fois au maximum, une courte pause entre chaque et
     * jusqu'à 3 tentatives. Les serveurs IPTV refusent (erreur HTTP) quand l'appli les inonde de requêtes.
     * Les messages d'erreur ne contiennent jamais d'adresse ni d'identifiant.
     */
    private suspend fun get(u: HttpUrl): String = gate.withPermit {
        var last: Exception = ApiError("Connexion impossible.")
        for (attempt in 0 until 3) {
            if (attempt > 0) delay(attempt * 900L)
            try {
                val body = withContext(Dispatchers.IO) { Net.fetchString(u) }
                delay(120)
                return@withPermit body
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpErr) {
                last = ApiError("Le serveur a répondu avec une erreur (HTTP ${e.code}).")
                if (e.code == 401 || e.code == 404) break
            } catch (e: IOException) {
                last = ApiError("Connexion impossible.")
            }
        }
        throw last
    }

    /** Vérifie les identifiants. */
    suspend fun login() {
        val body = get(url(null))
        val j = try { JSONObject(body) } catch (e: Exception) { throw ApiError("Réponse du serveur invalide.") }
        val info = j.optJSONObject("user_info") ?: throw ApiError("Identifiants refusés par le serveur.")
        val auth = info.opt("auth")?.toString() ?: "0"
        if (auth != "1" && auth != "true") throw ApiError("Identifiants refusés par le serveur.")
    }

    suspend fun categories(kind: Kind): List<Cat> {
        val arr = try { JSONArray(get(url(kind.categoriesAction))) } catch (e: CancellationException) { throw e } catch (e: ApiError) { throw e } catch (e: Exception) { return emptyList() }
        val out = ArrayList<Cat>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.s("category_id")
            val name = o.s("category_name")
            if (id.isNotEmpty() && name.isNotEmpty()) out.add(Cat(id, name))
        }
        return out
    }

    suspend fun items(kind: Kind, catId: String): List<Item> {
        val arr = try { JSONArray(get(url(kind.itemsAction, "category_id" to catId))) } catch (e: CancellationException) { throw e } catch (e: ApiError) { throw e } catch (e: Exception) { return emptyList() }
        val out = ArrayList<Item>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = if (kind == Kind.SERIES) o.s("series_id") else o.s("stream_id")
            if (id.isEmpty()) continue
            val rawPoster = if (kind == Kind.SERIES) o.s("cover").ifEmpty { o.s("stream_icon") } else o.s("stream_icon")
            val poster = if (rawPoster.startsWith("http://") || rawPoster.startsWith("https://")) rawPoster else ""
            val rating = o.s("rating").let { r -> if ((r.toFloatOrNull() ?: 0f) > 0f) r else "" }
            val added = (if (kind == Kind.SERIES) o.s("last_modified") else o.s("added")).toLongOrNull() ?: 0L
            out.add(Item(id, kind, o.s("name"), poster, rating, added, o.s("container_extension")))
        }
        return out
    }

    companion object {
        private val gate = Semaphore(2)
    }
}
