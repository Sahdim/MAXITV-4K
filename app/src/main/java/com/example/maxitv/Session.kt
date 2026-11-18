package com.example.maxitv

import android.content.Context

/** Connexion en cours : garde les catégories et les dernières listes en mémoire (peu : la mémoire d'une box est limitée). */
object Session {
    private var api: XtreamApi? = null
    private val cats = HashMap<Kind, List<Cat>>()
    private val lists = LinkedHashMap<String, List<Item>>()
    private const val ROW_MAX = 40
    private const val CACHE_MAX = 8

    val ready: Boolean get() = api != null

    fun begin(ctx: Context): Boolean {
        val p = Profiles.active(ctx) ?: return false
        return try {
            api = XtreamApi(p.host, p.user, p.pass)
            cats.clear()
            lists.clear()
            true
        } catch (e: ApiError) {
            api = null
            false
        }
    }

    private fun api(): XtreamApi = api ?: throw ApiError("Non connecté.")

    suspend fun categories(kind: Kind): List<Cat> {
        cats[kind]?.let { return it }
        val l = api().categories(kind)
        cats[kind] = l
        return l
    }

    suspend fun items(kind: Kind, cat: Cat): List<Item> {
        val key = kind.name + ":" + cat.id
        lists[key]?.let { return it }
        val l = api().items(kind, cat.id).take(ROW_MAX)
        lists[key] = l
        while (lists.size > CACHE_MAX) {
            val it = lists.keys.iterator()
            it.next()
            it.remove()
        }
        return l
    }
}
