package com.example.maxitv

import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat

/**
 * Cartes des lignes. Les images sont décodées à la taille exacte de la carte (en RGB 565) : c'est ce qui garde
 * l'app fluide sur une box peu puissante. Les animations de mise au point (zoom) de Leanback sont conservées.
 */
class CardPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val ctx = parent.context
        val card = ImageCardView(ctx)
        card.isFocusable = true
        card.isFocusableInTouchMode = true
        card.setInfoAreaBackgroundColor(ContextCompat.getColor(ctx, R.color.panel))
        return Presenter.ViewHolder(card)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
        val card = viewHolder.view as ImageCardView
        val d = card.resources.displayMetrics.density
        fun px(v: Int) = (v * d).toInt()
        when (item) {
            is Item -> {
                val live = item.kind == Kind.LIVE
                val w = px(if (live) 200 else 140)
                val h = px(if (live) 113 else 210)
                card.setMainImageDimensions(w, h)
                card.setMainImageScaleType(if (live) ImageView.ScaleType.FIT_CENTER else ImageView.ScaleType.CENTER_CROP)
                card.titleText = item.name
                card.contentText = if (item.rating.isNotEmpty()) "★ " + item.rating else ""
                load(card, item.poster, w, h, live)
            }
            is Retry -> {
                card.setMainImageDimensions(px(200), px(113))
                card.setMainImageScaleType(ImageView.ScaleType.CENTER_INSIDE)
                card.titleText = "Impossible de charger"
                card.contentText = "Appuie pour réessayer"
                load(card, "", px(200), px(113), true)
            }
            is Action -> {
                card.setMainImageDimensions(px(200), px(113))
                card.setMainImageScaleType(ImageView.ScaleType.CENTER_INSIDE)
                card.titleText = item.title
                card.contentText = item.sub
                load(card, "", px(200), px(113), true)
            }
            else -> {}
        }
    }

    private fun load(card: ImageCardView, url: String, w: Int, h: Int, fit: Boolean) {
        val iv = card.mainImageView ?: return
        if (url.isEmpty()) {
            Glide.with(card.context).clear(iv)
            iv.setImageResource(R.drawable.movie)
            return
        }
        val req = Glide.with(card.context).load(url)
            .override(w, h)
            .format(DecodeFormat.PREFER_RGB_565)
            .placeholder(R.drawable.movie)
            .error(R.drawable.movie)
        (if (fit) req.fitCenter() else req.centerCrop()).into(iv)
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        val card = viewHolder.view as ImageCardView
        card.mainImageView?.let { Glide.with(card.context).clear(it) }
        card.mainImage = null
    }
}
