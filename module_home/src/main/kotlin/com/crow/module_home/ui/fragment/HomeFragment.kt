package com.crow.module_home.ui.fragment

import android.graphics.Color
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.crow.base.current_project.BaseStrings
import com.crow.base.tools.coroutine.FlowBus
import com.crow.base.tools.extensions.*
import com.crow.base.ui.fragment.BaseMviFragment
import com.crow.base.ui.viewmodel.*
import com.crow.module_home.R
import com.crow.module_home.databinding.HomeComicBinding
import com.crow.module_home.databinding.HomeFragmentBinding
import com.crow.module_home.model.ComicType
import com.crow.module_home.model.entity.ComicTapEntity
import com.crow.module_home.model.intent.HomeIntent
import com.crow.module_home.model.resp.homepage.*
import com.crow.module_home.model.resp.homepage.results.RecComicsResult
import com.crow.module_home.model.resp.homepage.results.Results
import com.crow.module_home.ui.adapter.HomeBannerAdapter
import com.crow.module_home.ui.adapter.HomeComicRvAdapter
import com.crow.module_home.ui.viewmodel.HomeViewModel
import com.google.android.material.R.attr.materialIconButtonStyle
import com.google.android.material.button.MaterialButton
import com.to.aboomy.pager2banner.IndicatorView
import com.to.aboomy.pager2banner.ScaleInTransformer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.crow.base.R as baseR


/*************************
 * @Machine: RedmiBook Pro 15 Win11
 * @Path: module_home/src/main/kotlin/com/crow/module_home/view
 * @Time: 2023/3/6 0:14
 * @Author: CrowForKotlin
 * @Description: HomeBodyFragment
 * @formatter:on
 **************************/
class HomeFragment : BaseMviFragment<HomeFragmentBinding>() {

    // 主页 VM
    private val mHomeVM by viewModel<HomeViewModel>()

    // 刷新按钮（换一批） ＆ 主页刷新布局控件
    private var mRecRefreshButton : MaterialButton? = null
    private var mSwipeRefreshLayout : SwipeRefreshLayout? = null

    // 主页布局刷新的时间 第一次进入布局默认20Ms 之后刷新 为 50Ms
    private var mHomePageLayoutRefreshTime = 10L

    // 主页数据量较多， 采用Rv方式
    private lateinit var mHomeBannerAdapter: HomeBannerAdapter
    private lateinit var mHomeRecAdapter: HomeComicRvAdapter<RecComicsResult>
    private lateinit var mHomeHotAdapter: HomeComicRvAdapter<HotComic>
    private lateinit var mHomeNewAdapter: HomeComicRvAdapter<NewComic>
    private lateinit var mHomeFinishAdapter: HomeComicRvAdapter<FinishComic>
    private lateinit var mHomeTopicAapter: HomeComicRvAdapter<Topices>
    private lateinit var mHomeRankAapter: HomeComicRvAdapter<RankComics>

    override fun getViewBinding(inflater: LayoutInflater) = HomeFragmentBinding.inflate(inflater)

    override fun onDestroyView() {
        super.onDestroyView()

        // 置空两个控件 避免内存泄漏
        mSwipeRefreshLayout = null
        mRecRefreshButton = null
    }

    override fun initObserver() {
        mHomeVM.onOutput { intent ->
            when (intent) {

                // （获取主页）（根据 刷新事件 来决定是否启用加载动画） 正常加载数据、反馈View
                is HomeIntent.GetHomePage -> {
                    intent.mViewState
                        .doOnLoading { if(mSwipeRefreshLayout == null) showLoadingAnim() }
                        .doOnResult {
                            // 刷新控件null 代表 用的是加载动画 -> 取消加载动画 否则直接加载页面数据
                            if (mSwipeRefreshLayout == null) dismissLoadingAnim { doLoadHomePage(intent.homePageData!!.mResults) }
                            else doLoadHomePage(intent.homePageData!!.mResults)
                        }
                        .doOnError { code, msg ->
                            if (code == ViewState.Error.UNKNOW_HOST) mBinding.root.showSnackBar(msg ?: getString(baseR.string.BaseLoadingError))
                            if (mSwipeRefreshLayout == null) dismissLoadingAnim() else  mSwipeRefreshLayout?.isRefreshing = false
                        }
                }

                // （刷新获取）不启用 加载动画 正常加载数据 -> 反馈View
                is HomeIntent.GetRecPageByRefresh -> {
                    intent.mViewState
                        .doOnSuccess { mRecRefreshButton!!.isEnabled = true }
                        .doOnError { _, _ -> mBinding.root.showSnackBar(getString(baseR.string.BaseLoadingError)) }
                        .doOnResult {
                            viewLifecycleOwner.lifecycleScope.launch {
                                mHomeRecAdapter.doRecNotify(mHomeRecAdapter, intent.recPageData?.mResults?.mResult?.toMutableList() ?: return@launch, mHomePageLayoutRefreshTime)
                            }
                        }
                }
            }
        }
    }

    override fun initData() {

        // 重建View的同时 判断是否已获取数据
        if (mHomeVM.getResult() != null) return

        // 获取主页数据
        mHomeVM.input(HomeIntent.GetHomePage())
    }

    override fun initView() {

        // 适配器可以作为局部成员，但不要直接初始化，不然会导致被View引用从而内存泄漏
        mHomeRecAdapter = HomeComicRvAdapter(mType = ComicType.Rec) { type, pathword ->
            FlowBus.with<ComicTapEntity>(BaseStrings.Key.HOME_COMIC_TAP).post(lifecycleScope, ComicTapEntity(type, pathword))
        }
        mHomeHotAdapter = HomeComicRvAdapter(mType = ComicType.Hot) { type, pathword ->
            FlowBus.with<ComicTapEntity>(BaseStrings.Key.HOME_COMIC_TAP).post(lifecycleScope, ComicTapEntity(type, pathword))
        }
        mHomeNewAdapter = HomeComicRvAdapter(mType = ComicType.New) { type, pathword ->
            FlowBus.with<ComicTapEntity>(BaseStrings.Key.HOME_COMIC_TAP).post(lifecycleScope, ComicTapEntity(type, pathword))
        }
        mHomeFinishAdapter = HomeComicRvAdapter(mType = ComicType.Commit) { type, pathword ->
            FlowBus.with<ComicTapEntity>(BaseStrings.Key.HOME_COMIC_TAP).post(lifecycleScope, ComicTapEntity(type, pathword))
        }
        mHomeRankAapter = HomeComicRvAdapter(mType = ComicType.Rank) { type, pathword ->
            FlowBus.with<ComicTapEntity>(BaseStrings.Key.HOME_COMIC_TAP).post(lifecycleScope, ComicTapEntity(type, pathword))
        }
        mHomeTopicAapter = HomeComicRvAdapter(mType = ComicType.Topic) { type, pathword ->
            FlowBus.with<ComicTapEntity>(BaseStrings.Key.HOME_COMIC_TAP).post(lifecycleScope, ComicTapEntity(type, pathword))
        }

        // 初始化刷新 推荐的按钮
        mRecRefreshButton = initRecRefreshView()

        // 设置 Banner 的高度 （1.875 屏幕宽高指定倍数）、（添加页面效果、指示器、指示器需要设置BottomMargin不然会卡在Banner边缘（产生重叠））
        mBinding.homeBanner.doOnLayout { it.layoutParams.height = (it.width / 1.875 + 0.5).toInt() }
        mBinding.homeBanner.addPageTransformer(ScaleInTransformer())
            .setPageMargin(mContext.dp2px(20f), mContext.dp2px(10f))
            .setIndicator(
                IndicatorView(mContext)
                    .setIndicatorColor(Color.DKGRAY)
                    .setIndicatorSelectorColor(Color.WHITE)
                    .setIndicatorStyle(IndicatorView.IndicatorStyle.INDICATOR_BEZIER)
                    .also { it.doOnLayout { view -> (view.layoutParams as RelativeLayout.LayoutParams).bottomMargin = mContext.resources.getDimensionPixelSize(baseR.dimen.base_dp20) } })


        // 设置每一个子布局的 （Icon、标题、适配器）
        mBinding.homeComicRec.initHomeComicRvView(R.drawable.home_ic_recommed_24dp, R.string.home_recommend_comic, mHomeRecAdapter).also{ it.homeComicConstraint.addView(mRecRefreshButton) }
        mBinding.homeComicHot.initHomeComicRvView(R.drawable.home_ic_hot_24dp, R.string.home_hot_comic, mHomeHotAdapter)
        mBinding.homeComicNew.initHomeComicRvView(R.drawable.home_ic_new_24dp, R.string.home_new_comic, mHomeNewAdapter)
        mBinding.homeComicFinish.initHomeComicRvView(R.drawable.home_ic_finish_24dp, R.string.home_commit_finish, mHomeFinishAdapter)
        mBinding.homeComicRank.initHomeComicRvView(R.drawable.home_ic_rank_24dp, R.string.home_rank_comic, mHomeRankAapter)
        mBinding.homeComicTopic.initHomeComicRvView(R.drawable.home_ic_topic_24dp, R.string.home_topic_comic, mHomeTopicAapter).also { it.homeComicBookRv.layoutManager = GridLayoutManager(mContext, 2) }

        // 判断数据是否为空 不为空则加载数据
        doLoadHomePage(mHomeVM.getResult() ?: return)
    }

    override fun initListener() {

        // 刷新推荐按钮 点击监听事件
        mRecRefreshButton!!.setOnClickListener { view ->

            // 禁用 之后请求完毕会恢复
            view.isEnabled = false

            // 发送意图
            mHomeVM.input(HomeIntent.GetRecPageByRefresh())
        }

        // 每个主页漫画类型（显示更多）卡片的点击事件
        mBinding.homeComicRec.homeComicMore.clickGap { _, _ -> }
        mBinding.homeComicHot.homeComicMore.clickGap { _, _ -> }
        mBinding.homeComicNew.homeComicMore.clickGap { _, _ -> }
        mBinding.homeComicFinish.homeComicMore.clickGap { _, _ -> }
        mBinding.homeComicTopic.homeComicMore.clickGap { _, _ -> }
        mBinding.homeComicRank.homeComicMore.clickGap { _, _ -> }
    }

    // 初始化主页Rv视图
    private fun <T> HomeComicBinding.initHomeComicRvView(@DrawableRes iconRes: Int, @StringRes iconText: Int, adapter: HomeComicRvAdapter<T>): HomeComicBinding {
        homeComicButtonTitle.setIconResource(iconRes)
        homeComicButtonTitle.text = mContext.getString(iconText)
        homeComicBookRv.adapter = adapter
        return this
    }

    // 初始化刷新按钮
    private fun initRecRefreshView(): MaterialButton {
        return MaterialButton(mContext, null, materialIconButtonStyle).apply {
            layoutParams =  ConstraintLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                endToEnd = PARENT_ID
                topToBottom = mBinding.homeComicRec.homeComicBookRv.id
            }
            icon = ContextCompat.getDrawable(mContext, R.drawable.home_ic_refresh_24dp)
            iconSize = mContext.resources.getDimensionPixelSize(baseR.dimen.base_dp24)
            iconTint = null
            iconPadding = mContext.resources.getDimensionPixelSize(baseR.dimen.base_dp6)
            text = mContext.getString(R.string.home_refresh)
        }
    }

    // 加载主页数据
    private fun doLoadHomePage(results: Results) {

        // 设置轮播图数据
        mHomeBannerAdapter = HomeBannerAdapter(results.mBanners.filter { banner -> banner.mType <= 2 }.toMutableList()) { type, pathword ->
            FlowBus.with<ComicTapEntity>(BaseStrings.Key.HOME_COMIC_TAP).post(lifecycleScope, ComicTapEntity(type, pathword))
        }

        mBinding.homeBanner.adapter = mHomeBannerAdapter

        // 布局不可见 则淡入 否则代表正在刷新 提示即可
        if (!mBinding.homeLinearLayout.isVisible) {  mBinding.homeLinearLayout.animateFadeIn() }
        else toast(getString(baseR.string.BaseRefreshScucess))

        viewLifecycleOwner.lifecycleScope.launch {

            // 等待刷新控件动画消失 150MS
            if (mSwipeRefreshLayout != null && mSwipeRefreshLayout!!.isRefreshing) {
                mSwipeRefreshLayout!!.isRefreshing = false
                delay(150L)
            }

            // 通知适配器
            mHomeRecAdapter.doRecNotify(mHomeRecAdapter, results.mRecComicsResult.mResult.toMutableList(), mHomePageLayoutRefreshTime)
            mHomeHotAdapter.doHotNotify(mHomeHotAdapter, results.mHotComics.toMutableList(), mHomePageLayoutRefreshTime)
            mHomeNewAdapter.doNewNotify(mHomeNewAdapter, results.mNewComics.toMutableList(), mHomePageLayoutRefreshTime)
            mHomeFinishAdapter.doFinishNotify(mHomeFinishAdapter, results.mFinishComicDatas.mResult.toMutableList(), mHomePageLayoutRefreshTime)
            mHomeRankAapter.doRankNotify(mHomeRankAapter, results.mRankDayComics.mResult.toMutableList(), mHomePageLayoutRefreshTime)
            mHomeTopicAapter.doTopicNotify(mHomeTopicAapter, results.mTopics.mResult.toMutableList(), mHomePageLayoutRefreshTime)

            // 设置布局刷新时间 50MS
            if (mHomePageLayoutRefreshTime == 10L) mHomePageLayoutRefreshTime = 50L
        }
    }

    // 暴露的函数 提供给 ContainerFragment 用于通知主页刷新
    fun doRefresh(swipeRefreshLayout: SwipeRefreshLayout) {

        // 初始化刷新控件布局 300MS 后执行获取主页的意图（给刷新控件的动画加一点时间执行，不然请求快的话会导致控件直接消失）
        mSwipeRefreshLayout = swipeRefreshLayout
        doAfterDelay(300L) { mHomeVM.input(HomeIntent.GetHomePage()) }
    }

}