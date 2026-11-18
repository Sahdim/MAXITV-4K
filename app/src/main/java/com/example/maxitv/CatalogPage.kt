package com.example.maxitv

import android.os.Bundle
import android.widget.Toast
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.OnItemViewSelectedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Page Films / Séries / Live : une ligne par catégorie. Les lignes se chargent au fur et à mesure qu'on descend
 * (jamais toutes d'un coup). Une ligne qui échoue montre une carte « Réessayer » au lieu de disparaître.
 */
class CatalogPage : RowsSupportFragment() {
    private val rows = ArrayObjectAdapter(ListRowPresenter(FocusHighlight.ZOOM_FACTOR_SMALL).apply { shadowEnabled = false })
    private var cats: List<Cat> = emptyList()
    private var next = 0
    private var busy = false

    private val kind: Kind
        get() = Kind.valueOf(arguments?.getString("k") ?: Kind.MOVIES.name)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = rows
        setOnItemViewClickedListener(object : OnItemViewClickedListener {
            override fun onItemClicked(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
                click(item, row)
            }
        })
        setOnItemViewSelectedListener(object : OnItemViewSelectedListener {
            override fun onItemSelected(itemViewHolder: Presenter.ViewHolder?, item: Any?, rowViewHolder: RowPresenter.ViewHolder?, row: Row?) {
                if (row != null) fill(rows.indexOf(row) + 5)
            }
        })
        start()
    }

    private fun start() {
        rows.clear()
        next = 0
        cats = emptyList()
        rows.add(ListRow(HeaderItem("Chargement…"), ArrayObjectAdapter(CardPresenter())))
        lifecycleScope.launch {
            try {
                cats = Session.categories(kind)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                rows.clear()
                rows.add(retryRow(null))
                return@launch
            }
            rows.clear()
            if (cats.isEmpty()) {
                rows.add(ListRow(HeaderItem("Aucune catégorie"), ArrayObjectAdapter(CardPresenter())))
            } else {
                fill(5)
            }
        }
    }

    private fun fill(target: Int) {
        if (busy || next >= cats.size) return
        busy = true
        lifecycleScope.launch {
            try {
                while (rows.size() < target && next < cats.size) {
                    val c = cats[next++]
                    val row: ListRow? = try {
                        val items = Session.items(kind, c)
                        if (items.isEmpty()) null else makeRow(c, items)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        retryRow(c)
                    }
                    if (row != null) rows.add(row)
                }
            } finally {
                busy = false
            }
        }
    }

    private fun makeRow(c: Cat, items: List<Item>): ListRow {
        val a = ArrayObjectAdapter(CardPresenter())
        a.addAll(0, items)
        return ListRow(HeaderItem(c.name), a)
    }

    private fun retryRow(c: Cat?): ListRow {
        val a = ArrayObjectAdapter(CardPresenter())
        a.add(Retry(c))
        return ListRow(HeaderItem(c?.name ?: "Connexion"), a)
    }

    private fun click(item: Any?, row: Row?) {
        when (item) {
            is Retry -> retry(item, row)
            is Item -> Toast.makeText(requireContext(), item.name, Toast.LENGTH_SHORT).show()
            else -> {}
        }
    }

    private fun retry(r: Retry, row: Row?) {
        val cat = r.cat
        if (cat == null) {
            start()
            return
        }
        lifecycleScope.launch {
            val replacement: ListRow? = try {
                val items = Session.items(kind, cat)
                if (items.isEmpty()) null else makeRow(cat, items)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                retryRow(cat)
            }
            val i = if (row == null) -1 else rows.indexOf(row)
            if (i >= 0) {
                if (replacement == null) rows.removeItems(i, 1) else rows.replace(i, replacement)
            }
        }
    }

    companion object {
        fun of(kind: Kind): CatalogPage {
            val f = CatalogPage()
            f.arguments = Bundle().apply { putString("k", kind.name) }
            return f
        }
    }
}
