package com.crow.module_main.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import androidx.activity.addCallback
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import coil.size.Scale
import coil.transform.CircleCropTransformation
import com.crow.base.app.app
import com.crow.base.tools.extensions.doOnClickInterval
import com.crow.base.tools.extensions.dp2px
import com.crow.base.tools.extensions.getCurrentVersionName
import com.crow.base.tools.extensions.getNavigationBarHeight
import com.crow.base.tools.extensions.getStatusBarHeight
import com.crow.base.tools.extensions.popSyncWithClear
import com.crow.base.ui.fragment.BaseMviFragment
import com.crow.base.ui.viewmodel.doOnError
import com.crow.base.ui.viewmodel.doOnResult
import com.crow.mangax.copymanga.entity.Fragments
import com.crow.module_main.R
import com.crow.module_main.databinding.MainFragmentAboutBinding
import com.crow.module_main.model.intent.AppIntent
import com.crow.module_main.ui.viewmodel.MainViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.crow.mangax.R as mangaR

/*************************
 * @Machine: RedmiBook Pro 15 Win11
 * @Path: module_user/src/main/kotlin/com/crow/module_user/ui/fragment
 * @Time: 2023/4/7 13:13
 * @Author: CrowForKotlin
 * @Description: AboutAuthor
 * @formatter:on
 **************************/
class AboutAuthorFragment : BaseMviFragment<MainFragmentAboutBinding>() {

    // ContainerVM
    private val mContainerVm by viewModel<MainViewModel>()

    private fun navigateUp() = parentFragmentManager.popSyncWithClear(Fragments.About.name)

    override fun getViewBinding(inflater: LayoutInflater) = MainFragmentAboutBinding.inflate(inflater)

    override fun onStart() {
        super.onStart()
        mBackDispatcher = requireActivity().onBackPressedDispatcher.addCallback(this) { navigateUp() }
    }

    override fun initView(savedInstanceState: Bundle?) {

//        mBinding.composeView.setContent { AboutScreen() }

        mBinding.root.setPadding(0, mContext.getStatusBarHeight(), 0, mContext.getNavigationBarHeight())

        app.imageLoader.enqueue(
//            mBinding.icon.load(mangaR.drawable.base_icon_crow)
            ImageRequest.Builder(mContext)
                .data(mangaR.drawable.base_icon_crow) // 加载的图片地址或占位符
                .target(mBinding.icon)
                .build()
        )

        val builder = SpannableStringBuilder()
        builder.appendLine(mContext.getString(R.string.main_about_crow_email))
        builder.appendLine()
        builder.appendLine(mContext.getString(R.string.main_about_crow_help))
        mBinding.content.text = builder
        mBinding.version.text = getString(R.string.main_about_app_version, getCurrentVersionName().split("_")[0])
    }

    override fun initObserver(savedInstanceState: Bundle?) {
        mContainerVm.onOutput {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            when(it) {
                is AppIntent.GetQQGroup -> {
                    it.mViewState
                        .doOnResult {
                            intent.data = Uri.parse(it.link!!)
                            startActivity(intent)
                        }
                        .doOnError { _, _ ->
                            intent.data = Uri.parse(getString(R.string.main_about_tg_gropu))
                            startActivity(intent)
                        }
                }
            }
        }
    }

    override fun initListener() {
        mBinding.mainAboutBack.doOnClickInterval { navigateUp() }
        mBinding.userAboutAddQqGroup.doOnClickInterval {
            mContainerVm.input(AppIntent.GetQQGroup())
        }
    }
}