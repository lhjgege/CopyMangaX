package com.crow.base.viewmodel

import androidx.fragment.app.FragmentManager
import com.crow.base.dialog.LoadingAnimDialog

/*
@Machine: RedmiBook Pro 15
@RelativePath: cn\barry\base\viewmodel\ViewState.kt
@Path: D:\Barry\B_study\Android\Android_Project\JetpackApp\lib_base\src\main\java\cn\barry\base\viewmodel\ViewState.kt
@Author: CrowForKotlin
@Time: 2022/4/26 9:46 周二 上午
@Description:
*/

sealed class ViewState {

    // 用于预构建
    object Default : ViewState()

    // 正在加载中
    object Loading : ViewState()

    // 加载成功
    object Success : ViewState()

    // With结果
    object Result : ViewState()

    // 加载失败
    class Error(val type: Int = DEFAULT, val msg: String? = null) : ViewState() {
        companion object {
            const val DEFAULT = -1
            const val UNKNOW_HOST = -2
        }
    }


}

//自定义error 可以抛出来结束流的运行
class ViewStateException(msg: String, throwable: Throwable? = null) : Exception(msg, throwable)

inline fun ViewState.doOnResultWithLoading(
    fragmentManager: FragmentManager,
    crossinline onResult: () -> Unit,
    crossinline animEnd: () -> Unit,
) {
    when (this) {
        is ViewState.Default -> {}
        is ViewState.Loading -> LoadingAnimDialog.show(fragmentManager)
        is ViewState.Error -> LoadingAnimDialog.dismiss(fragmentManager) { animEnd() }
        is ViewState.Success -> LoadingAnimDialog.dismiss(fragmentManager) { animEnd() }
        is ViewState.Result -> onResult()
    }
}

inline fun ViewState.doOnLoading(crossinline block: () -> Unit): ViewState {
    if (this is ViewState.Loading) block()
    return this
}

inline fun ViewState.doOnSuccess(crossinline block: () -> Unit): ViewState {
    if (this is ViewState.Success) block()
    return this
}

inline fun ViewState.doOnError(crossinline block: (Int, String?) -> Unit): ViewState {
    if (this is ViewState.Error) block(type, msg)
    return this
}

inline fun ViewState.doOnResult(crossinline block: () -> Unit): ViewState {
    if (this is ViewState.Result) block()
    return this
}

suspend inline fun ViewState.doOnLoadingInCoroutine(crossinline block: suspend () -> Unit): ViewState {
    if (this is ViewState.Loading) block()
    return this
}

suspend inline fun ViewState.doOnSuccessInCoroutine(crossinline block: suspend () -> Unit): ViewState {
    if (this is ViewState.Success) block()
    return this
}

suspend inline fun ViewState.doOnErrorInCoroutine(crossinline block: suspend (Int, String?) -> Unit): ViewState {
    if (this is ViewState.Error) block(type, msg)
    return this
}

suspend inline fun ViewState.doOnResultInCoroutine(crossinline block: suspend () -> Unit): ViewState {
    if (this is ViewState.Result) block()
    return this
}