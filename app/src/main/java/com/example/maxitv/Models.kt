package com.example.maxitv

enum class Kind(val title: String, val categoriesAction: String, val itemsAction: String) {
    LIVE("Live", "get_live_categories", "get_live_streams"),
    MOVIES("Films", "get_vod_categories", "get_vod_streams"),
    SERIES("Séries", "get_series_categories", "get_series")
}

data class Cat(val id: String, val name: String)

data class Item(
    val id: String,
    val kind: Kind,
    val name: String,
    val poster: String,
    val rating: String,
    val added: Long,
    val ext: String
)

/** Carte « Réessayer » affichée à la place d'une ligne qui n'a pas pu se charger (au lieu de la faire disparaître). */
data class Retry(val cat: Cat?)

/** Carte d'action (profil, ajout de profil, déconnexion). */
data class Action(val id: String, val title: String, val sub: String)
