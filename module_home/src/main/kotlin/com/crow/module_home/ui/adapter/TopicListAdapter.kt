package com.crow.module_home.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.crow.base.R
import com.crow.base.app.app
import com.crow.mangax.copymanga.appComicCardHeight
import com.crow.mangax.copymanga.appComicCardWidth
import com.crow.mangax.copymanga.formatHotValue
import com.crow.base.tools.extensions.doOnClickInterval
import com.crow.base.tools.extensions.px2sp
import com.crow.mangax.copymanga.entity.AppConfig.Companion.mChineseConvert
import com.crow.mangax.tools.language.ChineseConverter
import com.crow.mangax.ui.adapter.MangaCoilVH
import com.crow.module_home.databinding.HomeTopicRvBinding
import com.crow.module_home.model.resp.topic.TopicResult
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

/*************************
 * @Machine: RedmiBook Pro 15 Win11
 * @Package: com.crow.module_main.ui.adapter
 * @Time: 2023/10/3 18:39
 * @Author: CrowForKotlin
 * @Description: HistoryListAdapter
 * @formatter:on
 **************************/
class TopicListAdapter(
    private val mLifecycleScope: LifecycleCoroutineScope,
    private val onClick: (name: String, pathword: String) -> Unit
) : PagingDataAdapter<TopicResult, TopicListAdapter.HistoryVH>(DiffCallback()) {

    private val mChipTextSize = app.px2sp(app.resources.getDimension(R.dimen.base_sp12_5))

    inner class HistoryVH(binding: HomeTopicRvBinding) : MangaCoilVH<HomeTopicRvBinding>(binding) {

        init {
            initComponent(binding.loading, binding.loadingText, binding.image)

            binding.card.layoutParams.apply {
                width = appComicCardWidth
                height = appComicCardHeight
            }

            itemView.doOnClickInterval { (getItem(absoluteAdapterPosition) ?: return@doOnClickInterval).apply { onClick(mName, mPathWord) } }
        }

        fun onBind(item: TopicResult) {
            if (mChineseConvert) {
                mLifecycleScope.launch {
                    binding.chipGroup.removeAllViews()
                    item.mTheme.forEach {
                        val chip = Chip(itemView.context)
                        chip.text = ChineseConverter.convert(it.mName)
                        chip.textSize = mChipTextSize
                        binding.chipGroup.addView(chip)
                    }
                    binding.name.text = ChineseConverter.convert(item.mName)
                }
            } else {
                binding.chipGroup.removeAllViews()
                item.mTheme.forEach {
                    val chip = Chip(itemView.context)
                    chip.text = it.mName
                    chip.textSize = mChipTextSize
                    binding.chipGroup.addView(chip)
                }
                binding.name.text = item.mName
            }
            binding.author.text = item.mAuthor.joinToString { it.mName }
            binding.hot.text = formatHotValue(item.mPopular)
            loadCoverImage(item.mCover)
        }
    }

    /**
     * ● DiffCallback
     *
     * ● 2023-11-01 00:05:03 周三 上午
     * @author crowforkotlin
     */
    class DiffCallback : DiffUtil.ItemCallback<TopicResult>() {
        override fun areItemsTheSame(oldItem: TopicResult, newItem: TopicResult): Boolean {
            return oldItem.mPathWord == newItem.mPathWord
        }

        override fun areContentsTheSame(oldItem: TopicResult, newItem: TopicResult): Boolean {
            return oldItem == newItem
        }
    }

    /**
     * ● 复用VH
     *
     * ● 2023-11-01 00:04:50 周三 上午
     * @author crowforkotlin
     */
    override fun onBindViewHolder(holder: HistoryVH, position: Int) { holder.onBind(getItem(position) ?: return) }

    /**
     * ● 创建VH
     *
     * ● 2023-11-01 00:04:43 周三 上午
     * @author crowforkotlin
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryVH { return HistoryVH(HomeTopicRvBinding.inflate(LayoutInflater.from(parent.context), parent, false)) }
}