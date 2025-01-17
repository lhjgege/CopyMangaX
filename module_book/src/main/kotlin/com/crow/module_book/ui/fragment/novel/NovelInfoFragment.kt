
package com.crow.module_book.ui.fragment.novel

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.imageLoader
import coil.request.ImageRequest
import com.crow.base.app.app
import com.crow.mangax.copymanga.BaseEventEnum
import com.crow.mangax.copymanga.BaseStrings
import com.crow.mangax.copymanga.MangaXAccountConfig
import com.crow.mangax.copymanga.entity.Fragments
import com.crow.mangax.copymanga.formatHotValue
import com.crow.mangax.copymanga.getSpannableString
import com.crow.mangax.copymanga.okhttp.AppProgressFactory
import com.crow.base.tools.coroutine.FlowBus
import com.crow.base.tools.extensions.animateFadeIn
import com.crow.base.tools.extensions.animateFadeOutInVisibility
import com.crow.base.tools.extensions.doOnClickInterval
import com.crow.base.tools.extensions.onCollect
import com.crow.base.tools.extensions.px2dp
import com.crow.base.tools.extensions.px2sp
import com.crow.base.tools.extensions.removeWhiteSpace
import com.crow.base.tools.extensions.toast
import com.crow.base.ui.viewmodel.doOnError
import com.crow.base.ui.viewmodel.doOnLoading
import com.crow.base.ui.viewmodel.doOnResult
import com.crow.mangax.copymanga.entity.AppConfig.Companion.mChineseConvert
import com.crow.mangax.tools.language.ChineseConverter
import com.crow.module_book.R
import com.crow.module_book.model.database.model.BookChapterEntity
import com.crow.module_book.model.entity.BookType
import com.crow.module_book.model.intent.BookIntent
import com.crow.module_book.model.resp.NovelChapterResp
import com.crow.module_book.model.resp.comic_info.Status
import com.crow.module_book.ui.adapter.novel.NovelChapterRvAdapter
import com.crow.module_book.ui.fragment.InfoFragment
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.core.qualifier.named

class NovelInfoFragment : InfoFragment() {

    /**
     * ● Regist FlowBus
     *
     * ● 2023-06-24 23:45:12 周六 下午
     */
    init {
        FlowBus.with<BookChapterEntity>(BaseEventEnum.UpdateChapter.name).register(this) {
            mVM.updateBookChapterOnDB(it)
        }
    }

    /**
     * ● 轻小说章节Rv
     *
     * ● 2023-06-15 22:57:42 周四 下午
     */
    private var mAdapter: NovelChapterRvAdapter? = null

    /**
     * ● 显示轻小说信息页面
     *
     * ● 2023-06-15 22:57:28 周四 下午
     */
    private fun showNovelInfoPage() {
        val novelInfoPage = mVM.mNovelInfoPage?.mNovel ?: return
        mVM.findReadedBookChapterOnDB(novelInfoPage.mUuid, BookType.NOVEL)
        mProgressFactory = AppProgressFactory.createProgressListener(novelInfoPage.mCover) { _, _, percentage, _, _ -> mBinding.bookInfoProgressText.text = AppProgressFactory.formateProgress(percentage) }
        app.imageLoader.enqueue(
            ImageRequest.Builder(mContext)
                .listener(
                    onSuccess = { _, _ ->
                        mBinding.bookInfoLoading.isInvisible = true
                        mBinding.bookInfoProgressText.isInvisible = true
                    },
                    onError = { _, _ -> mBinding.bookInfoProgressText.text = "-1%" },
                )
                .data(novelInfoPage.mCover)
                .target(mBinding.bookInfoImage)
                .build()
        )
        mBinding.author.text = getString(R.string.book_author, novelInfoPage.mAuthor.joinToString { it.mName })
        mBinding.hot.text = getString(R.string.book_hot, formatHotValue(novelInfoPage.mPopular))
        mBinding.update.text = getString(R.string.book_update, novelInfoPage.mDatetimeUpdated)

        val status = when (novelInfoPage.mStatus.mValue) {
            Status.LOADING -> getString(R.string.BookComicStatus, novelInfoPage.mStatus.mDisplay).getSpannableString(ContextCompat.getColor(mContext, R.color.book_green), 3)
            Status.FINISH -> getString(R.string.BookComicStatus, novelInfoPage.mStatus.mDisplay).getSpannableString(ContextCompat.getColor(mContext, R.color.book_red), 3)
            else -> ". . ."
        }.toString()
        if (mChineseConvert) {
            lifecycleScope.launch {
                mBinding.chapter.text = ChineseConverter.convert(getString(R.string.book_new_chapter, novelInfoPage.mLastChapter.mName))
                mBinding.status.text = ChineseConverter.convert(status)
                mBinding.name.text = ChineseConverter.convert(novelInfoPage.mName)
                mBinding.desc.text = ChineseConverter.convert(novelInfoPage.mBrief.removeWhiteSpace())
                novelInfoPage.mTheme.forEach { theme ->
                    mBinding.bookInfoThemeChip.addView(Chip(mContext).also {
                        it.text = ChineseConverter.convert(theme.mName)
                        it.textSize = app.px2sp(resources.getDimension(com.crow.base.R.dimen.base_sp12_5))
                        it.chipStrokeWidth = app.px2dp(resources.getDimension(com.crow.base.R.dimen.base_dp1))
                        it.isClickable = false
                    })
                }
            }
        } else {
            mBinding.chapter.text = getString(R.string.book_new_chapter, novelInfoPage.mLastChapter.mName)
            mBinding.status.text = status
            mBinding.name.text = novelInfoPage.mName
            mBinding.desc.text = novelInfoPage.mBrief.removeWhiteSpace()
            novelInfoPage.mTheme.forEach { theme ->
                mBinding.bookInfoThemeChip.addView(Chip(mContext).also {
                    it.text = theme.mName
                    it.textSize = app.px2sp(resources.getDimension(com.crow.base.R.dimen.base_sp12_5))
                    it.chipStrokeWidth = app.px2dp(resources.getDimension(com.crow.base.R.dimen.base_dp1))
                    it.isClickable = false
                })
            }
        }

        mBinding.bookInfoCardview.animateFadeIn()
    }

    /**
     * ● 处理添加轻小说至 书架意图
     *
     * ● 2023-06-15 22:57:55 周四 下午
     */
    private fun processAddNovelIntent(intent: BookIntent.AddNovelToBookshelf) {
        intent.mViewState
            .doOnLoading { showLoadingAnim() }
            .doOnError { _, _ -> dismissLoadingAnim { toast(getString(com.crow.mangax.R.string.mangax_unknow_error)) } }
            .doOnResult {
                dismissLoadingAnim {
                    if (intent.isCollect == 1) {
                        toast(getString(R.string.book_add_success))
                        setButtonRemoveFromBookshelf()
                    } else {
                        toast(getString(R.string.book_remove_success))
                        setButtonAddToBookshelf()
                    }
                }
            }
    }

    /**
     * ● 显示章节页面
     *
     * ● 2023-06-15 22:57:13 周四 下午
     */
    private fun notifyChapterPageShowNow(novelChapterResp: NovelChapterResp) {
        // 添加章节选择器
        addBookChapterSlector(null, novelChapterResp)

        // 开启协程 数据交给适配器去做出调整
        viewLifecycleOwner.lifecycleScope.launch {
            mAdapter?.doNotify(novelChapterResp.mList.toMutableList())
        }
    }

    /**
     * ● 初始化数据
     *
     * ● 2023-06-15 22:58:06 周四 下午
     */
    override fun onInitData() {

        if (MangaXAccountConfig.mAccountToken.isNotEmpty()) mVM.input(BookIntent.GetNovelBrowserHistory(mPathword))

        if (mVM.mNovelInfoPage == null) mVM.input(BookIntent.GetNovelInfoPage(mPathword))
    }

    /**
     * ● 处理章节
     *
     * ● 2023-06-15 22:58:25 周四 下午
     */
    override fun <T> showChapterPage(chapterResp: T?, invalidResp: String?) {
        if (chapterResp == null) {
            processChapterFailureResult(invalidResp)
            return
        }

        if (chapterResp is NovelChapterResp) {

            if (!mBinding.bookInfoRefresh.isRefreshing) { dismissLoadingAnim() }

            if (mBinding.comicInfoErrorTips.isVisible) mBinding.comicInfoErrorTips.animateFadeOutInVisibility()

            if (!mBinding.bookInfoLinearChapter.isVisible)  mBinding.bookInfoLinearChapter.animateFadeIn()

            if (!mBinding.bookInfoRvChapter.isVisible) mBinding.bookInfoRvChapter.animateFadeIn()

            notifyChapterPageShowNow(chapterResp)
        }
    }

    /**
     * ● 刷新
     *
     * ● 2023-06-24 23:45:01 周六 下午
     */
    override fun onRefresh() {

        if (mVM.mNovelInfoPage == null) {
            mVM.input(BookIntent.GetNovelInfoPage(mPathword))
        }
        mVM.input(BookIntent.GetNovelChapter(mPathword))
    }

    /**
     * ● 初始化视图
     *
     * ● 2023-06-24 23:44:56 周六 下午
     */
    override fun initView(savedInstanceState: Bundle?) {

        // 初始化父View
        super.initView(savedInstanceState)


        // 设置适配器
        mBinding.bookInfoRvChapter.adapter = mAdapter!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mAdapter = NovelChapterRvAdapter { toast("很抱歉暂未开发完成...") }
        super.onViewCreated(view, savedInstanceState)
    }

    /**
     * ● 初始化观察者
     *
     * ● 2023-06-24 23:44:47 周六 下午
     */
    override fun initObserver(savedInstanceState: Bundle?) {
        super.initObserver(savedInstanceState)

        mVM.mChapterEntity.onCollect(this) { chapter ->
            if (mBaseEvent.getBoolean(LOGIN_CHAPTER_HAS_BEEN_SETED) == null && chapter != null && mBaseEvent.getBoolean(
                    HIDDEN_CHANED
                ) != true) {
                toast(getString(R.string.book_readed_chapter, chapter.mChapterName))
                mAdapter?.mChapterName = chapter.mChapterName
                mAdapter?.notifyItemRangeChanged(0, mAdapter?.itemCount ?: return@onCollect)
            }
        }

        mVM.onOutput { intent ->
            when(intent) {
                is BookIntent.GetNovelChapter -> doOnBookPageChapterIntent<NovelChapterResp>(intent)
                is BookIntent.GetNovelInfoPage -> doOnBookPageIntent(intent) { showNovelInfoPage() }
                is BookIntent.AddNovelToBookshelf -> { processAddNovelIntent(intent) }
                is BookIntent.GetNovelBrowserHistory -> {
                    intent.mViewState
                        .doOnResult {
                            intent.novelBrowser!!.apply {
                                if (mCollect == null) setButtonAddToBookshelf() else setButtonRemoveFromBookshelf()
                                mAdapter?.mChapterName = mBrowse?.chapterName ?: return@doOnResult
                                mBaseEvent.setBoolean(LOGIN_CHAPTER_HAS_BEEN_SETED, true)
                                mAdapter?.notifyItemRangeChanged(0, mAdapter?.itemCount ?: return@doOnResult)
                                toast(getString(R.string.book_readed_chapter, mAdapter?.mChapterName))
                            }
                        }
                }

            }
        }
    }

    /**
     * ● 初始化监听器
     *
     * ● 2023-06-24 23:44:28 周六 下午
     */
    override fun initListener() {
        super.initListener()

        mBinding.bookInfoCardview.doOnClickInterval {
           navigateImage(get<Fragment>(named(Fragments.Image.name)).also { it.arguments =
               bundleOf(
                   BaseStrings.IMAGE_URL to mVM.mNovelInfoPage?.mNovel?.mCover,
                   BaseStrings.NAME to mVM.mNovelInfoPage?.mNovel?.mName
               )
           })
        }

        mBinding.bookInfoAddToBookshelf.doOnClickInterval{
            if (mVM.mNovelInfoPage == null) return@doOnClickInterval
            if (MangaXAccountConfig.mAccountToken.isEmpty()) {
                toast(getString(R.string.book_add_invalid))
                return@doOnClickInterval
            }
            mVM.input(BookIntent.AddNovelToBookshelf(mVM.mUuid ?: return@doOnClickInterval, if (mBinding.bookInfoAddToBookshelf.text == getString(R.string.book_comic_add_to_bookshelf)) 1 else 0))
        }

    }

    /**
     * ● Lifecycle onStop
     *
     * ● 2023-06-24 23:33:11 周六 下午
     */
    override fun onStop() {
        super.onStop()
        mBaseEvent.remove(LOGIN_CHAPTER_HAS_BEEN_SETED)
    }

    /**
     * ● Lifecycle onDestroyView
     *
     * ● 2023-06-24 23:44:12 周六 下午
     */
    override fun onDestroyView() {
        super.onDestroyView()

        // 轻小说适配器置空
        mAdapter = null
    }
}