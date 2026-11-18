package com.example.maxitv

import android.os.Bundle
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter

/** Page Profil : profils enregistrés (un par code Xtream), ajout d'un profil, déconnexion. */
class ProfilePage : RowsSupportFragment() {
    private val rows = ArrayObjectAdapter(ListRowPresenter(FocusHighlight.ZOOM_FACTOR_SMALL).apply { shadowEnabled = false })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = rows
        setOnItemViewClickedListener(object : OnItemViewClickedListener {
            override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
                if (item is Action) act(item)
            }
        })
        build()
    }

    private fun build() {
        val ctx = requireContext()
        val active = Profiles.active(ctx)?.id
        val profiles = ArrayObjectAdapter(CardPresenter())
        for (p in Profiles.all(ctx)) {
            profiles.add(Action("p:" + p.id, p.name, if (p.id == active) "Profil actif" else "Appuie pour passer à ce profil"))
        }
        rows.clear()
        rows.add(ListRow(HeaderItem("Profils"), profiles))
        val account = ArrayObjectAdapter(CardPresenter())
        account.add(Action("add", "Ajouter un profil", "Autre code Xtream"))
        account.add(Action("out", "Se déconnecter", "Retire le profil actif"))
        rows.add(ListRow(HeaderItem("Compte"), account))
    }

    private fun act(a: Action) {
        val ctx = requireContext()
        val main = activity as? MainActivity ?: return
        when {
            a.id.startsWith("p:") -> {
                Profiles.setActive(ctx, a.id.substring(2))
                main.showHome()
            }
            a.id == "add" -> main.showLogin()
            a.id == "out" -> {
                Profiles.active(ctx)?.let { Profiles.remove(ctx, it.id) }
                main.showHome()
            }
        }
    }
}
