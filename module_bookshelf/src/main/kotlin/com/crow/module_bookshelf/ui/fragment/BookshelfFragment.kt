package com.crow.module_bookshelf.ui.fragment

import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.crow.base.current_project.BaseStrings
import com.crow.base.current_project.BaseUser
import com.crow.base.current_project.processTokenError
import com.crow.base.tools.coroutine.FlowBus
import com.crow.base.tools.extensions.*
import com.crow.base.ui.fragment.BaseMviFragment
import com.crow.base.ui.viewmodel.ViewState
import com.crow.base.ui.viewmodel.doOnError
import com.crow.base.ui.viewmodel.doOnResult
import com.crow.module_bookshelf.databinding.BookshelfFragmentBinding
import com.crow.module_bookshelf.model.intent.BookShelfIntent
import com.crow.module_bookshelf.ui.adapter.BookshelfRvAdapter
import com.crow.module_bookshelf.ui.viewmodel.BookshelfViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.crow.base.R as baseR

/*************************
 * @Machine: RedmiBook Pro 15 Win11
 * @RelativePath: com\crow\module_bookshelf\ui\fragment\BookShelfFragment.kt
 * @Path: D:\Programing\Android\2023\CopyManga\module_bookshelf\src\main\kotlin\com\crow\module_bookshelf\ui\fragment\BookShelfFragment.kt
 * @Author: CrowForKotlin
 * @Time: 2023/3/22 23:56 Wed PM
 * @Description:BookShelfFragment
 * @formatter:on
 *************************/

class BookshelfFragment() : BaseMviFragment<BookshelfFragmentBinding>() {

    // 书架VM
    private val mBookshelfVM by viewModel<BookshelfViewModel>()

    // Bookshelf 适配器
    private lateinit var mBookshelfRvAdapter: BookshelfRvAdapter

    private var mRefreshLayout: SwipeRefreshLayout? = null

    override fun getViewBinding(inflater: LayoutInflater) = BookshelfFragmentBinding.inflate(inflater)

    override fun initView() {

        // 设置 内边距属性 实现沉浸式效果
        mBinding.root.setPadding(0, mContext.getStatusBarHeight(), 0, 0)

        // 初始化适配器
        mBookshelfRvAdapter = BookshelfRvAdapter {  }

        // 设置适配器
        mBinding.bookshelfRv.adapter = mBookshelfRvAdapter
    }

    override fun initData() {
        // mBookshelfVM.input(BookShelfIntent.GetBookShelf())
    }

    override fun initObserver() {

        repeatOnLifecycle(Lifecycle.State.CREATED) {

            // 发送获取书架的意图
            mBookshelfVM.input(BookShelfIntent.GetBookShelf())

            // 收集书架Pager状态
            mBookshelfVM.mBookshelfFlowPager?.collect { data -> mBookshelfRvAdapter.submitData(data) }
        }

        // 接收意图
        mBookshelfVM.onOutput { intent ->
            when(intent) {
                is BookShelfIntent.GetBookShelf -> {
                    intent.mViewState

                        .doOnResult {

                            // 文本不可见 代表成功获取到数据
                            mBinding.bookshelfRvText.visibility = View.GONE

                            // 判斷是否正在刷新
                            if (mRefreshLayout?.isRefreshing == true) {
                                toast(getString(baseR.string.BaseRefreshScucess))
                                mRefreshLayout!!.isRefreshing = false
                            }
                        }
                        .doOnError { code, msg ->

                            // 获取书架 适配器数据 0 才显示文本 顺便取消刷新
                            if (mBookshelfRvAdapter.itemCount == 0) {
                                mBinding.bookshelfRvText.visibility = View.VISIBLE
                                mRefreshLayout?.isRefreshing = false
                            }

                            // 解析地址失败 且 Resumed的状态才提示
                            if (code == ViewState.Error.UNKNOW_HOST && this.isResumed) mBinding.root.showSnackBar(msg ?: getString(baseR.string.BaseLoadingError))

                            // Token为空不处理 Token错误校验
                            else if (BaseUser.CURRENT_USER_TOKEN.isEmpty()) return@doOnError

                            // 处理Token错误校验
                            else mBinding.root.processTokenError(code, msg,
                                doOnCancel = {
                                    mBookshelfRvAdapter.refresh()
                                    FlowBus.with<Unit>(BaseStrings.Key.CLEAR_USER_INFO).post(lifecycleScope, Unit)
                                },
                                doOnConfirm = {
                                    navigate(baseR.id.mainUserloginfragment)
                                    FlowBus.with<Unit>(BaseStrings.Key.CLEAR_USER_INFO).post(lifecycleScope, Unit)
                                }
                            )
                        }
                }
            }
        }
    }

    fun doRefresh(refreshlayout: SwipeRefreshLayout) {
        mRefreshLayout = refreshlayout
        viewLifecycleOwner.lifecycleScope.launch {
            mBookshelfRvAdapter.refresh()
            delay(500L)
        }

    }
}