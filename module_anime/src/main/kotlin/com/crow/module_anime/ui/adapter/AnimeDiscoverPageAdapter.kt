package com.crow.module_anime.ui.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.GenericTransitionOptions
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.request.transition.DrawableCrossFadeTransition
import com.bumptech.glide.request.transition.NoTransition
import com.crow.base.copymanga.appComicCardHeight
import com.crow.base.copymanga.entity.IBookAdapterColor
import com.crow.base.copymanga.formatValue
import com.crow.base.copymanga.glide.AppGlideProgressFactory
import com.crow.base.tools.extensions.BASE_ANIM_200L
import com.crow.base.tools.extensions.doOnClickInterval
import com.crow.base.ui.adapter.BaseGlideLoadingViewHolder
import com.crow.base.ui.view.ToolTipsView
import com.crow.module_anime.databinding.AnimeFragmentRvBinding
import com.crow.module_anime.model.resp.discover.DiscoverPageResult

class AnimeDiscoverPageAdapter(
    inline val mDoOnTapComic: (DiscoverPageResult) -> Unit
) : PagingDataAdapter<DiscoverPageResult, AnimeDiscoverPageAdapter.LoadingViewHolder>(DiffCallback()), IBookAdapterColor<AnimeDiscoverPageAdapter.LoadingViewHolder> {

    class DiffCallback : DiffUtil.ItemCallback<DiscoverPageResult>() {
        override fun areItemsTheSame(oldItem: DiscoverPageResult, newItem: DiscoverPageResult): Boolean {
            return oldItem.mName == newItem.mName
        }

        override fun areContentsTheSame(oldItem: DiscoverPageResult, newItem: DiscoverPageResult): Boolean {
            return oldItem == newItem
        }
    }

    class LoadingViewHolder(binding: AnimeFragmentRvBinding) : BaseGlideLoadingViewHolder<AnimeFragmentRvBinding>(binding)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : LoadingViewHolder {
        return LoadingViewHolder(AnimeFragmentRvBinding.inflate(LayoutInflater.from(parent.context), parent, false)).also { vh ->

            vh.binding.discoverRvImage.layoutParams.height = appComicCardHeight

            vh.binding.discoverRvBookCard.doOnClickInterval {
                mDoOnTapComic(getItem(vh.absoluteAdapterPosition) ?: return@doOnClickInterval)
            }

            vh.binding.root.doOnClickInterval {
                mDoOnTapComic(getItem(vh.absoluteAdapterPosition) ?: return@doOnClickInterval)
            }

            ToolTipsView.showToolTipsByLongClick(vh.binding.discoverRvName)
        }
    }

    override fun onBindViewHolder(vh: LoadingViewHolder, position: Int) {
        val item = getItem(position) ?: return

        vh.binding.discoverLoading.isVisible = true
        vh.binding.discoverProgressText.isVisible = true
        vh.binding.discoverProgressText.text = AppGlideProgressFactory.PERCENT_0
        vh.mAppGlideProgressFactory?.onRemoveListener()?.onCleanCache()

        vh.mAppGlideProgressFactory = AppGlideProgressFactory.createGlideProgressListener(item.mCover) { _, _, percentage, _, _ ->
            vh.binding.discoverProgressText.text = AppGlideProgressFactory.getProgressString(percentage)
        }

        Glide.with(vh.itemView.context)
            .load(item.mCover)
            .addListener(vh.mAppGlideProgressFactory?.getRequestListener())
            .transition(GenericTransitionOptions<Drawable>().transition { dataSource, _ ->
                if (dataSource == DataSource.REMOTE) {
                    vh.binding.discoverLoading.isInvisible = true
                    vh.binding.discoverProgressText.isInvisible = true
                    DrawableCrossFadeTransition(BASE_ANIM_200L.toInt(), true)
                } else {
                    vh.binding.discoverLoading.isInvisible = true
                    vh.binding.discoverProgressText.isInvisible = true
                    NoTransition()
                }
            })
            .into(vh.binding.discoverRvImage)

        vh.binding.discoverRvName.text = item.mName
        vh.binding.discoverRvHot.text = formatValue(item.mPopular)
        vh.binding.discoverRvTime.text = item.mDatetimeUpdated
    }

    override fun setColor(vh: LoadingViewHolder, color: Int) {
        vh.binding.discoverRvName.setTextColor(color)
        vh.binding.discoverRvHot.setTextColor(color)
        vh.binding.discoverRvTime.setTextColor(color)
    }
}