package com.crow.base.ui.viewmodel.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crow.base.R
import com.crow.base.app.app
import com.crow.base.tools.extensions.logger
import com.crow.base.tools.extensions.toast
import com.crow.base.ui.viewmodel.BaseViewState
import com.crow.base.ui.viewmodel.BaseViewState.Error
import com.crow.base.ui.viewmodel.BaseViewState.Loading
import com.crow.base.ui.viewmodel.BaseViewState.Result
import com.crow.base.ui.viewmodel.BaseViewState.Success
import com.crow.base.ui.viewmodel.ViewStateException
import com.orhanobut.logger.Logger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/*************************
 * @Machine: RedmiBook Pro 15 Win11
 * @Path: lib_base/src/main/kotlin/com/crow/base/viewmodel
 * @Time: 2023/3/9 14:40
 * @Author: CrowForKotlin
 * @Description: BaseMviViewModel
 * @formatter:on
 **************************/
abstract class BaseMviViewModel<I : BaseMviIntent> : ViewModel() {

    @PublishedApi internal val _sharedFlow: MutableSharedFlow<I> = MutableSharedFlow (1, 3, BufferOverflow.DROP_OLDEST)

    val sharedFlow: SharedFlow<I> get() = _sharedFlow

    open fun dispatcher(intent: I) { }

    open fun dispatcher(intent: I, onEndAction: Runnable) { }

    fun input(intent: I) = dispatcher(intent)

    fun input(intent: I, onEndAction: Runnable) = dispatcher(intent, onEndAction)

    suspend fun output(baseMviSuspendResult: BaseMviSuspendResult<I>) {
        _sharedFlow.collect { baseMviSuspendResult.onResult(it) }
    }

    /**
     * ● 将 Flow<T> 转换成适合于 MVI 架构的结果，并利用 _sharedFlow.emit() 发送结果到 UI。
     *
     * ● 2023-08-31 21:41:19 周四 下午
     */
    fun <T> flowResult(intent: I, flow: Flow<T>, result: BaseMviFlowResult<I, T>) {
        viewModelScope.launch {
            flow
                .onStart { emitValueMoreoverDelayAfter(intent.also { it.mViewState = Loading }) }
                .onCompletion { emitValueMoreoverDelayAfter(intent.also { it.mViewState = Success }) }
                .catch { catch -> emitValueMoreoverDelayAfter(intent.also { it.mViewState = Error(if (catch is ViewStateException) Error.UNKNOW_HOST else Error.DEFAULT, msg = catch.message ?: app.getString(R.string.base_unknow_error)) })}
                .collect { emitValueMoreoverDelayAfter(result.onResult(it).also { event -> event.mViewState = Result }) }
        }
    }

    /**
     * ● 将 Flow<T> 转换成适合于 MVI 架构的结果，并根据 意图判断是否需要通过 _sharedFlow.emit() 发送结果到 UI 否则 直接获取结果。
     *
     * ● 2023-08-31 21:41:13 周四 下午
     */
    suspend fun <T> flowResult(flow: Flow<T>, intent: I? = null, context: CoroutineContext = Dispatchers.Main,  result: BaseMviFlowResult<I, T>) = suspendCancellableCoroutine { continuation ->
        viewModelScope.launch(context) {
            flow
                .onStart { trySendIntent(intent, Loading) }
                .onCompletion { catch -> trySendIntent(intent, Success) { if (catch != null && !continuation.isCompleted) continuation.resumeWithException(catch) } }
                .catch { catch ->
                    trySendIntent(intent, Error (if (catch is ViewStateException) Error.UNKNOW_HOST else Error.DEFAULT, catch.message ?: app.getString(R.string.base_unknow_error))) {
                        if (!continuation.isCompleted) continuation.resumeWithException(catch)
                    }
                }
                .collect {
                    if (intent != null) emitValueMoreoverDelayAfter(result.onResult(it).also { event -> event.mViewState = Result })
                    if (!continuation.isCompleted) continuation.resume(it)
                }
        }
    }

    private suspend inline fun trySendIntent(intent: I?, state: BaseViewState, endLogic: () -> Unit = {}): I? {
        if (intent != null) {
            intent.mViewState = state
            emitValueMoreoverDelayAfter(intent)
        }
        endLogic()
        return intent
    }

    private suspend  fun emitValueMoreoverDelayAfter(result: I, delayMs: Long = 1L) {
        _sharedFlow.emit(result)
        delay(delayMs)
    }

    inline fun toEmitValue(context: CoroutineContext = Dispatchers.Main, crossinline result: suspend () -> I) {
        viewModelScope.launch(context) { _sharedFlow.emit(result().also { it.mViewState = Result }) }
    }

    inline fun launchJob(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        coroutineExceptionHandler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, catch ->
            logger("BaseMviViewModel Catch : ${catch.stackTraceToString()}", Logger.ERROR)
        },
        crossinline block: suspend CoroutineScope.() -> Unit
    ) : Job = viewModelScope.launch(context + coroutineExceptionHandler, start) { block() }
}