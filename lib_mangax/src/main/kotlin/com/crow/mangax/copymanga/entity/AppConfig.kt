
package com.crow.mangax.copymanga.entity

import android.content.SharedPreferences
import com.crow.base.app.app
import com.crow.mangax.copymanga.BaseStrings
import com.crow.mangax.copymanga.MangaXAccountConfig
import com.crow.base.tools.extensions.DataStoreAgent
import com.crow.base.tools.extensions.SpNameSpace
import com.crow.base.tools.extensions.appConfigDataStore
import com.crow.base.tools.extensions.asyncDecode
import com.crow.base.tools.extensions.asyncEncode
import com.crow.base.tools.extensions.decode
import com.crow.base.tools.extensions.getSharedPreferences
import com.crow.base.tools.extensions.toJson
import com.crow.base.tools.extensions.toTypeEntity
import com.squareup.moshi.Json

/**
 * App 全局配置
 *
 * @property mAppFirstInit 第一次初始化
 * @property mHotMangaSite HotManga站点
 * @property mCopyMangaSite CopyManga站点
 * @property mRoute 路线 "0", "1"
 * @property mResolution 分辨率 800、1200、1500
 * @constructor Create empty App config entity
 */
data class AppConfig(

    @Json(name = "App_FirstInit")
    val mAppFirstInit: Boolean = false,

    @Json(name = "HotManga_Site")
    val mHotMangaSite: String = BaseStrings.URL.HotManga,

    @Json(name = "CopyManga_Site")
    val mCopyMangaSite: String = BaseStrings.URL.COPYMANGA,

    @Json(name = "Route")
    val mRoute: String = MangaXAccountConfig.mRoute,

    @Json(name = "Resolution")
    val mResolution: Int = MangaXAccountConfig.mResolution,
) {
    companion object {

        /**
         * ●  黑夜模式
         *
         * ● 2023-12-18 00:10:11 周一 上午
         * @author crowforkotlin
         */
        var mDarkMode = false

        /**
         * ● 更新前置
         *
         * ● 2023-12-18 00:10:25 周一 上午
         * @author crowforkotlin
         */
        var mUpdatePrefix = true
            private set

        /**
         * ● 简繁题转换
         *
         * ● 2023-12-18 00:10:38 周一 上午
         * @author crowforkotlin
         */
        var mChineseConvert = true
            private set

        /**
         * ● 热度精准显示
         *
         * ● 2023-12-18 00:10:53 周一 上午
         * @author crowforkotlin
         */
        var mHotAccurateDisplay = false
            private set

        var mCoverOrinal = false
            private set

        private var mAppConfig: AppConfig? =null

        fun initialization() {
            val sp = SpNameSpace.CATALOG_CONFIG.getSharedPreferences()
            mDarkMode = sp.getBoolean(SpNameSpace.Key.ENABLE_DARK, false)
            mChineseConvert = sp.getBoolean(SpNameSpace.Key.ENABLE_CHINESE_CONVERT, true)
            mHotAccurateDisplay = sp.getBoolean(SpNameSpace.Key.ENABLE_HOT_ACCURATE_DISPLAY, false)
            mUpdatePrefix = sp.getBoolean(SpNameSpace.Key.ENABLE_UPDATE_PREFIX, true)
            mCoverOrinal = sp.getBoolean(SpNameSpace.Key.ENABLE_COVER_ORINAL, false)
        }

        fun getAppSP(): SharedPreferences {
            return SpNameSpace.CATALOG_CONFIG.getSharedPreferences()
        }


        fun getInstance(): AppConfig {
            return mAppConfig!!
        }

        suspend fun saveAppConfig(appConfig: AppConfig) {
            app.appConfigDataStore.asyncEncode(DataStoreAgent.APP_CONFIG, toJson(appConfig.also { mAppConfig = it }))
        }

        suspend fun readAppConfig(): AppConfig? {
            return  toTypeEntity<AppConfig>(app.appConfigDataStore.asyncDecode(DataStoreAgent.APP_CONFIG)).also { mAppConfig = it }
        }

        fun readAppConfigSync(): AppConfig? {
            return toTypeEntity<AppConfig>(app.appConfigDataStore.decode(DataStoreAgent.APP_CONFIG)).also { mAppConfig = it }
        }
    }
}