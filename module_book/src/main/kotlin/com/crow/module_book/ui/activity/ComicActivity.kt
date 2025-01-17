package com.crow.module_book.ui.activity

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.addCallback
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.crow.base.kt.BaseNotNullVar
import com.crow.base.tools.coroutine.launchDelay
import com.crow.base.tools.extensions.BASE_ANIM_300L
import com.crow.base.tools.extensions.animateFadeIn
import com.crow.base.tools.extensions.animateFadeOut
import com.crow.base.tools.extensions.animateFadeOutGone
import com.crow.base.tools.extensions.doOnClickInterval
import com.crow.base.tools.extensions.hasGlobalPoint
import com.crow.base.tools.extensions.immersionFullScreen
import com.crow.base.tools.extensions.immersionPadding
import com.crow.base.tools.extensions.immersionFullView
import com.crow.base.tools.extensions.immerureCutoutCompat
import com.crow.base.tools.extensions.log
import com.crow.base.tools.extensions.navigateIconClickGap
import com.crow.base.tools.extensions.toJson
import com.crow.base.tools.extensions.toTypeEntity
import com.crow.base.tools.extensions.toast
import com.crow.base.ui.activity.BaseMviActivity
import com.crow.base.ui.view.BaseErrorViewStub
import com.crow.base.ui.view.baseErrorViewStub
import com.crow.base.ui.viewmodel.doOnError
import com.crow.base.ui.viewmodel.doOnResult
import com.crow.mangax.copymanga.entity.AppConfig.Companion.mDarkMode
import com.crow.mangax.copymanga.entity.Fragments
import com.crow.mangax.copymanga.okhttp.AppProgressFactory
import com.crow.mangax.copymanga.tryConvert
import com.crow.module_book.R
import com.crow.module_book.databinding.BookActivityComicBinding
import com.crow.module_book.model.entity.comic.ComicActivityInfo
import com.crow.module_book.model.intent.BookIntent
import com.crow.module_book.ui.fragment.comic.reader.ComicCategories
import com.crow.module_book.ui.fragment.comic.reader.ComicStandardFragment
import com.crow.module_book.ui.fragment.comic.reader.ComicStriptFragment
import com.crow.module_book.ui.helper.GestureHelper
import com.crow.module_book.ui.viewmodel.ComicViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.slider.Slider
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonFrame
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonRecyclerView
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.qualifier.named
import com.crow.base.R as baseR


class ComicActivity : BaseMviActivity<BookActivityComicBinding>(), GestureHelper.GestureListener {

    companion object {
        const val ROTATE = "ROTATE"
        const val READER_MODE = "READER_MODE"
        const val OPTION = "OPTION"
        const val INFO = "INFO"
        const val TITLE = "TITLE"
        const val SUB_TITLE = "SUB_TITLE"
        const val SLIDE = "SLIDE"
        const val CHAPTER_POSITION = "CHAPTER_POSITION"
        const val CHAPTER_POSITION_OFFSET = "CHAPTER_POSITION_OFFSET"
        const val POS = "POSITION"
        const val POS_OFFSET = "OFFSET"
    }

    /**
     * ● 漫画VM
     *
     * ● 2023-07-07 23:53:41 周五 下午
     */
    private val mVM by viewModel<ComicViewModel>()

    /**
     * ● WindowInset For immersure or systembar
     *
     * ● 2023-07-07 23:53:58 周五 下午
     */
    private val mWindowInsetsCompat: WindowInsetsControllerCompat by lazy { WindowInsetsControllerCompat(window, mBinding.root) }

    /**
     * ● 漫画选项（条漫、等...）
     *
     * ● 2023-07-07 23:54:42 周五 下午
     */
    private val mComicCategory by lazy { ComicCategories(this, mBinding.comicFcv) }

    /**
     * ● Activitiy GestureHelper （手势处理）
     *
     * ● 2023-07-08 00:00:48 周六 上午
     */
    private lateinit var mGestureHelper: GestureHelper

    /**
     * ● ErrorViewStub
     *
     * ● 2024-01-27 23:47:49 周六 下午
     * @author crowforkotlin
     */
    private var mBaseErrorViewStub by BaseNotNullVar<BaseErrorViewStub>(true)

    private var mIsSliding = false

    /**
     * ● 获取ViewBinding
     *
     * ● 2023-07-07 23:55:09 周五 下午
     */
    override fun getViewBinding() = BookActivityComicBinding.inflate(layoutInflater)

    override fun onDestroy() {
        super.onDestroy()
        AppProgressFactory.clear()
    }

    /**
     * ● 初始化视图
     *
     * ● 2023-07-07 23:55:31 周五 下午
     */
    override fun initView(savedInstanceState: Bundle?) {

        val dp5 = resources.getDimensionPixelSize(baseR.dimen.base_dp5)

        // 初始化viewstub
        mBaseErrorViewStub = baseErrorViewStub(mBinding.error, lifecycle) {
            mBaseErrorViewStub.loadLayout(visible = false, animation = true)
            mBinding.loading.animateFadeIn()
            launchDelay(BASE_ANIM_300L) {
                mVM.input(BookIntent.GetComicPage(mVM.mPathword, mVM.mCurrentChapterUuid))
            }
        }

        // 全屏
        immersionFullScreen(mWindowInsetsCompat)

        // 沉浸式边距
        immersionPadding(mBinding.root) { view, insets, _ ->
            mBinding.topAppbar.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = insets.top }
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
            }
            mBinding.bottomAppbar.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin =  dp5 + insets.bottom }
        }

        // 沉浸式状态栏和工具栏
        immersionBarStyle()

        // 内存重启 后 savedInstanceState不为空 防止重复添加Fragment
        if (savedInstanceState == null) {

            mBinding.loading.isVisible = true
        }
    }

    /**
     * ● 初始化事件
     *
     * ● 2023-07-08 01:06:02 周六 上午
     */
    override fun initListener() {

        val slideListener = Slider.OnChangeListener  { _, value, _ ->
            when(mVM.mReaderSetting?.mReadMode) {
                ComicCategories.Type.STANDARD -> {
                    supportFragmentManager.setFragmentResult(SLIDE, bundleOf(SLIDE to value.toInt()))
                }
                ComicCategories.Type.STRIPT -> {
                    supportFragmentManager.setFragmentResult(SLIDE, bundleOf(SLIDE to value.toInt()))
                }
                ComicCategories.Type.PAGE -> {

                }
                else -> {

                }
            }
        }

        mGestureHelper =  GestureHelper(this, this)

        mBinding.topAppbar.navigateIconClickGap { finishActivity() }

        mBinding.slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                if (!mIsSliding) { supportFragmentManager.setFragmentResult(SLIDE, bundleOf(SLIDE to slider.value.toInt())) }
                mIsSliding = true
                mBinding.slider.addOnChangeListener(slideListener)

            }
            override fun onStopTrackingTouch(p0: Slider) {
                mIsSliding = false
                mBinding.slider.clearOnChangeListeners()
            }
        })

        mBinding.bottomToolbar.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.action_options -> {
                    get<BottomSheetDialogFragment>(named(Fragments.ComicBottom.name)).show(supportFragmentManager, null)
                }
            }
            true
        }

        mBinding.sliderLight.addOnChangeListener { slider, value, b ->
            val light = value.toInt()
            mVM.updateLight(light)
            mBinding.fullView.setBackgroundColor(ColorUtils.setAlphaComponent(Color.BLACK, light))
        }

        mBinding.light.doOnClickInterval {
            val transition = TransitionSet()
                .setDuration(BASE_ANIM_300L)
                .addTransition(Fade().addTarget(mBinding.cardSlide))
            TransitionManager.beginDelayedTransition(mBinding.bottomAppbar, transition)
            if (mBinding.cardSlide.isVisible) {
                mBinding.cardSlide.isGone = true
            } else {
                mBinding.cardSlide.isVisible = true
            }
        }

        mBinding.rotate.doOnClickInterval {
            supportFragmentManager.setFragmentResult("ROTATE", bundleOf())
        }

        supportFragmentManager.setFragmentResultListener(OPTION, this) { key, bundle ->
            lifecycleScope.launch {
                supportFragmentManager.clearFragmentResultListener(CHAPTER_POSITION)
                supportFragmentManager.clearFragmentResultListener(SLIDE)
                val comicType: ComicCategories.Type = when(bundle.getInt(READER_MODE)) {
                    R.string.book_comic_standard -> { ComicCategories.Type.STANDARD }
                    R.string.book_comic_stript -> { ComicCategories.Type.STRIPT }
                    R.string.book_comic_page -> { ComicCategories.Type.PAGE }
                    else -> { ComicCategories.Type.STANDARD }
                }
                if (comicType == ComicCategories.Type.STANDARD) {
                    val isSame = mVM.mReaderSetting?.mReadMode == comicType
                    mVM.updateReaderMode(comicType)
                    mComicCategory.apply(comicType)
                    setChapterResult(mVM.getPos() - if(isSame) 0 else 1, mVM.getPosOffset())
                } else {
                    mVM.updateReaderMode(comicType)
                    mComicCategory.apply(comicType)
                    setChapterResult(mVM.getPosByChapterId(), mVM.getPosOffset())
                }
            }
        }
    }

    /**
     * ● 初始化数据
     *
     * ● 2023-07-07 23:55:54 周五 下午
     */
    override fun initData(savedInstanceState: Bundle?) {
        if (!intent.getBooleanExtra(ROTATE, false)) {
            val info = toTypeEntity<ComicActivityInfo>(intent.getStringExtra(INFO)) ?: return finishActivity(getString(baseR.string.base_unknow_error))
            mVM.mComicInfo = info
            mVM.mChapterNextUuid = info.mChapterNextUuid
            mVM.mChapterPrevUuid = info.mChapterPrevUuid
            mBinding.topAppbar.title = info.mTitle
            mBinding.topAppbar.subtitle = info.mSubTitle
            mVM.input(BookIntent.GetComicPage(info.mPathword, info.mChapterCurrentUuid))
        } else {
            intent.removeExtra(ROTATE)
        }
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                when(mVM.getSetting()?.mReadMode) {
                    ComicCategories.Type.STANDARD -> { mComicCategory.apply(ComicCategories.Type.STANDARD) }
                    ComicCategories.Type.STRIPT -> { mComicCategory.apply(ComicCategories.Type.STRIPT) }
                    ComicCategories.Type.PAGE -> { mComicCategory.apply(ComicCategories.Type.PAGE) }
                    else -> { mComicCategory.apply(ComicCategories.Type.STANDARD) }
                }
            }
            mVM.initComicReader {
                mVM.mReaderComic?.let { comic ->
                    setChapterResult(comic.mChapterPosition, comic.mChapterPositionOffset)
                }
            }
        } else {
            mVM.initComicReader {
                if (mVM.mScrollPos == 0) {
                    mVM.mReaderComic?.let {
                        setChapterResult(it.mChapterPosition, it.mChapterPositionOffset)
                    }
                } else {
                    setChapterResult(mVM.mScrollPos, mVM.mScrollPosOffset)
                }
            }
        }
    }

    /**
     * ● 初始化观察者
     *
     * ● 2023-09-01 23:08:52 周五 下午
     */
    override fun initObserver(savedInstanceState: Bundle?) {

        lifecycleScope.launch {
            mVM.uiState.collect { state ->
                state?.let { uiState ->
                    if (mBinding.infobar.isGone) mBinding.infobar.animateFadeIn()
                    val readerContent = uiState.mReaderContent
                    val currentPage = uiState.mCurrentPagePos
                    val totalPage = uiState.mTotalPages
                    mBinding.infobar.update(
                        currentPage = currentPage,
                        totalPage = totalPage,
                        info = readerContent.mChapterInfo ?: return@let,
                        percent = mVM.computePercent(
                            pageIndex = currentPage,
                            totalPage = totalPage,
                            info = readerContent.mChapterInfo
                        )
                    )
                    mBinding.topAppbar.title = readerContent.mComicName
                    mBinding.topAppbar.subtitle = readerContent.mChapterInfo.mChapterName
                    if (mBinding.bottomAppbar.isGone || !mIsSliding) {
                        var pageFloat = uiState.mCurrentPagePos.toFloat()
                        var pageTotal = uiState.mTotalPages.toFloat()
                        if (pageFloat in mBinding.slider.valueFrom..mBinding.slider.valueTo) {
                            when(uiState.mReaderMode) {
                                ComicCategories.Type.STANDARD -> {
                                    pageTotal -= 2
                                    pageFloat = pageFloat.coerceIn(1f, pageTotal)
                                }
                                ComicCategories.Type.STRIPT -> {
                                    pageTotal -= 0
                                    pageFloat = pageFloat.coerceIn(1f, pageTotal)
                                }
                                ComicCategories.Type.PAGE -> {

                                }
                            }
                            val valueFrom: Float
                            if (pageTotal <= 1f) {
                                valueFrom = 0f
                                pageFloat = 1f
                                pageTotal =  1f
                            } else {
                                valueFrom = 1f
                            }
                            mBinding.slider.valueFrom = valueFrom
                            mBinding.slider.value = pageFloat
                            mBinding.slider.valueTo = pageTotal
                        }
                    }
                    mVM.tryUpdateReaderComicrInfo(currentPage, state.mCurrentPagePosOffset, state.mChapterID, readerContent.mChapterInfo) {
                        intent.putExtra(INFO, toJson(it))
                    }
                }
            }
        }

        mVM.onOutput { intent ->
            when(intent) {
                is BookIntent.GetComicPage -> {
                    if (!this.intent.getBooleanExtra("INIT", false)) {
                        intent.mViewState
                            .doOnError { _, _ -> showErrorPage() }
                            .doOnResult {
                                setChapterResult(-1, mVM.getPosOffset())
                                val page = intent.comicpage
                                if (page != null) {
                                    this.intent.putExtra("INIT", true)
                                    if(mBinding.loading.isVisible) mBinding.loading.animateFadeOutGone()
                                    lifecycleScope.tryConvert(page.mComic.mName, mBinding.topAppbar::setTitle)
                                    lifecycleScope.tryConvert(page.mChapter.mName, mBinding.topAppbar::setSubtitle)
                                    val total = page.mChapter.mContents.size.toFloat()
                                    val valueFrom: Float
                                    val value: Float
                                    if (total <= 1f) {
                                        valueFrom = 0f
                                        value = 1f
                                    } else {
                                        valueFrom = 1f
                                        value = 1f
                                    }
                                    mBinding.slider.valueFrom = valueFrom
                                    mBinding.slider.value = value
                                    mBinding.slider.valueTo = total
                                } else {
                                    showErrorPage()
                                }
                            }
                    }
                }
            }
        }
    }

    private fun showErrorPage() {
        launchDelay(BASE_ANIM_300L) {
            mBinding.loading.animateFadeOut().withEndAction {
                mBinding.loading.isGone = true
                mBaseErrorViewStub.loadLayout(visible = true, animation = true)
            }
        }
    }

    /**
     * ● Lifecycle onCreate
     *
     * ● 2023-07-07 23:56:16 周五 下午
     */
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        immersionFullView(window)
        immerureCutoutCompat(window)
    }

    /**
     * ● Lifecycle onStart
     *
     * ● 2023-07-07 23:56:48 周五 下午
     */
    override fun onStart() {
        super.onStart()
        onBackPressedDispatcher.addCallback(this) { finishActivity() }
    }

    /**
     * ● Activity Event onTouch
     *
     * ● 2023-07-07 23:56:56 周五 下午
     */
    override fun onTouch(area: Int, ev: MotionEvent) {
        transitionBar(mBinding.topAppbar.isVisible)
    }

    /**
     * ● Activity Event dispatchTouchEvent
     *
     * ● 2023-07-07 23:57:39 周五 下午
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        mGestureHelper.dispatchTouchEvent(ev, hasGlobalPoint(ev))
        return super.dispatchTouchEvent(ev)
    }

    /**
     * ● 检查点击范围内是否存在指定控件
     *
     * ● 2023-09-04 01:30:21 周一 上午
     */
    private fun hasGlobalPoint(ev: MotionEvent): Boolean {
        val rawX = ev.rawX.toInt()
        val rawY = ev.rawY.toInt()
        val hasToolbar = hasGlobalPoint(mBinding.topAppbar, rawX, rawY)
        val hasBottomBar = hasGlobalPoint(mBinding.bottomAppbar, rawX, rawY)
        if (hasToolbar || hasBottomBar) return true
        var hasRetry =false
        var hasButton = false
        val fragment = supportFragmentManager.fragments.firstOrNull()
        mBaseErrorViewStub.mVsBinding?.let { binding ->
            if (mBaseErrorViewStub.isVisible()) {
                hasRetry = hasGlobalPoint(binding.retry, rawX, rawY)
            }
        }
        if (fragment is ComicStandardFragment || fragment is ComicStriptFragment) {
            val rv = ((fragment.view as WebtoonFrame)[0] as WebtoonRecyclerView)
            val childView = rv.findChildViewUnder(ev.x, ev.y)
            if(childView is FrameLayout) {
                childView.forEach {
                    if (fragment.isRemoving) return hasToolbar
                    if(it is MaterialButton) {
                        hasButton = hasGlobalPoint(it, rawX, rawY)
                    }
                }
            }
        }
        return hasToolbar || hasButton || hasRetry
    }

    /**
     * ● Exit Activity With Animation and can add information
     *
     * ● 2023-09-01 22:41:49 周五 下午
     */
    @Suppress("DEPRECATION")
    private fun finishActivity(message: String? = null) {
        message?.let { toast(it) }
        finish()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, android.R.anim.fade_in, android.R.anim.fade_out)
        } else {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    /**
     * ● TransitionBar With Animation
     *
     * ● 2023-09-01 22:43:35 周五 下午
     */
    private fun transitionBar(isHide: Boolean) {
        val transition = TransitionSet()
            .setDuration(BASE_ANIM_300L)
            .addTransition(Slide(Gravity.TOP).addTarget(mBinding.topAppbar))
            .addTransition(Slide(Gravity.BOTTOM).addTarget(mBinding.bottomAppbar))
            .addTransition(Fade().addTarget(mBinding.infobar))
        TransitionManager.beginDelayedTransition(mBinding.root, transition)
        mBinding.topAppbar.isGone = isHide
        mBinding.bottomAppbar.isGone = isHide
        mWindowInsetsCompat.isAppearanceLightStatusBars = !mDarkMode
        mWindowInsetsCompat.isAppearanceLightNavigationBars = !mDarkMode
        if (isHide) {
            immersionFullScreen(mWindowInsetsCompat)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            mWindowInsetsCompat.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    /**
     * ● 沉浸式工具栏、导航栏、状态栏样式
     *
     * ● 2023-09-02 19:12:24 周六 下午
     */
    private fun immersionBarStyle(alpha: Int = 242) {
        (mBinding.topAppbar.background as MaterialShapeDrawable).apply {
            elevation = resources.getDimension(baseR.dimen.base_dp3)
            val color = ColorUtils.setAlphaComponent(resolvedTintColor, alpha)
            window.statusBarColor = color
            window.navigationBarColor = color
        }
    }

    private fun setChapterResult(position: Int, offset: Int) {
        supportFragmentManager.setFragmentResult(CHAPTER_POSITION, Bundle().also {
            it.putInt(CHAPTER_POSITION_OFFSET, offset)
            it.putInt(CHAPTER_POSITION, position)
        })
    }
}
