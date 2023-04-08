package com.crow.base.ui.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.crow.base.tools.extensions.permissionext.IBasePerEvent
import com.crow.base.tools.extensions.permissionext.IBasePermission
import com.crow.base.ui.dialog.LoadingAnimDialog
import com.crow.base.ui.viewmodel.mvi.IBaseMviExt

/*************************
 * @Machine: RedmiBook Pro 15 Win11
 * @Path: lib_base/src/main/java/com/barry/base/fragment
 * @Time: 2022/11/14 20:15
 * @Author: CrowForKotlin
 * @Description: BaseVBFragmentImpl
 * @formatter:off
 **************************/
abstract class BaseFragmentImpl : Fragment(), IBaseFragment, IBasePermission {

    private val mHandler = Handler(Looper.getMainLooper())
    private val mPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it.containsValue(false)) iBasePerEvent?.onFailure()
        else iBasePerEvent?.onSccess()
    }

    // 初始化View
    override fun initView(bundle: Bundle?) {}

    // 初始化监听事件
    override fun initListener() {}

    // 初始化数据
    override fun initData() { }

    override var iBasePerEvent: IBasePerEvent? = null

    override fun showLoadingAnim(loadingAnimConfig: LoadingAnimDialog.LoadingAnimConfig?) { LoadingAnimDialog.show(childFragmentManager, loadingAnimConfig) }

    override fun dismissLoadingAnim() { LoadingAnimDialog.dismiss(childFragmentManager) }

    override fun dismissLoadingAnim(loadingAnimCallBack: LoadingAnimDialog.LoadingAnimCallBack) {
        LoadingAnimDialog.dismiss(childFragmentManager) { loadingAnimCallBack.onAnimEnd()  }
    }

    override fun requestPermission(permissions: Array<String>, iBasePerEvent: IBasePerEvent) {
        this.iBasePerEvent = iBasePerEvent
        mPermission.launch(permissions)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return getView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)
        initView(savedInstanceState)
        initData()
        initListener()
    }

    override fun onDestroy(){
        super.onDestroy()
        dismissLoadingAnim()
    }
}
