package com.crow.module_comic.ui.viewmodel

import com.crow.base.viewmodel.mvi.BaseMviViewModel
import com.crow.module_comic.model.intent.ComicIntent
import com.crow.module_comic.model.resp.ComicChapterResp
import com.crow.module_comic.model.resp.ComicInfoResp
import com.crow.module_comic.network.ComicRepository

/*************************
 * @Machine: RedmiBook Pro 15 Win11
 * @Path: module_comic/src/main/kotlin/com/crow/module_comic/ui/viewmodel
 * @Time: 2023/3/15 0:20
 * @Author: CrowForKotlin
 * @Description: ComicViewModel
 * @formatter:on
 **************************/
class ComicViewModel(val repository: ComicRepository) : BaseMviViewModel<ComicIntent>() {

    private var mChapterStartIndex = 0

    var mComicInfoPage: ComicInfoResp? = null
        private set
    var mComicChapterPage: ComicChapterResp? = null
        private set

    var mIsSaveData = false
        private set

    override fun dispatcher(intent: ComicIntent) {
        when (intent) {
            is ComicIntent.GetComicInfo -> getComicInfo(intent)
            is ComicIntent.GetComicChapter -> getComicChapter(intent)
            is ComicIntent.GetComic -> getComic(intent)
        }
    }

    private fun getComicInfo(intent: ComicIntent.GetComicInfo) {
        intent.flowResult(repository.getComicInfo(intent.pathword)) { value ->
            mComicInfoPage = value
            intent.copy(comicInfo = value)
        }
    }

    private fun getComicChapter(intent: ComicIntent.GetComicChapter) {
        intent.flowResult(repository.getComicChapter(intent.pathword, mChapterStartIndex, 100)) { value ->
            mComicChapterPage = value
            intent.copy(comicChapter = value)
        }
    }

    private fun getComic(intent: ComicIntent.GetComic) {
        intent.flowResult(repository.getComic(intent.pathword, intent.uuid)) { value ->
            intent.copy(comicChapter = value)
        }
    }

    fun clearAllData() {
        mComicInfoPage = null
        mComicChapterPage = null
    }

    fun setSaveData(isSaveData: Boolean) {
        mIsSaveData = isSaveData
    }
}