package com.example.maxitv

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.PageRow
import androidx.leanback.widget.Row

/** Accueil : menu à gauche (Films, Séries, Live, Profil), chaque entrée ouvre sa page. */
class HomeFragment : BrowseSupportFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)
        headersState = BrowseSupportFragment.HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        brandColor = ContextCompat.getColor(requireContext(), R.color.panel)

        val rows = ArrayObjectAdapter(ListRowPresenter())
        rows.add(PageRow(HeaderItem(0, "Films")))
        rows.add(PageRow(HeaderItem(1, "Séries")))
        rows.add(PageRow(HeaderItem(2, "Live")))
        rows.add(PageRow(HeaderItem(3, "Profil")))
        adapter = rows
        mainFragmentRegistry.registerFragment(PageRow::class.java, PageFactory())
    }

    private class PageFactory : BrowseSupportFragment.FragmentFactory<Fragment>() {
        override fun createFragment(rowObj: Any?): Fragment {
            val row = rowObj as Row
            return when (row.headerItem.id) {
                0L -> CatalogPage.of(Kind.MOVIES)
                1L -> CatalogPage.of(Kind.SERIES)
                2L -> CatalogPage.of(Kind.LIVE)
                else -> ProfilePage()
            }
        }
    }
}
