package com.alphaboom.idolypridehelper.script

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.annotation.CallSuper
import com.alphaboom.idolypridehelper.cv.Template
import com.alphaboom.idolypridehelper.cv.TemplateMatching
import com.alphaboom.idolypridehelper.cv.Templates
import com.alphaboom.idolypridehelper.cv.getBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlin.time.Duration.Companion.seconds

abstract class Task {
    protected val driver = TaskDriver
    private val _taskActive = MutableStateFlow(true)
    val taskActive = _taskActive.asStateFlow()

    abstract suspend fun run()

    @CallSuper
    open fun destroy() {
        _taskActive.value = false
    }

    protected fun findMatchRect(
        screenshot: Bitmap,
        template: Template,
        threshold: Float = 0.8f
    ): RectF? {
        val templateBitmap = template.getBitmap(driver.context!!)
        return TemplateMatching.findMatchRect(templateBitmap, screenshot, threshold)
    }

    protected suspend fun click(
        screenshot: Bitmap,
        template: Template,
        threshold: Float = 0.8f
    ): Boolean {
        return findMatchRect(screenshot, template, threshold)?.let {
            driver.click(it.centerX(), it.centerY())
            true
        } ?: false
    }

    protected fun match(
        screenshot: Bitmap,
        template: Template,
        threshold: Float = 0.8f
    ): Boolean {
        return findMatchRect(screenshot, template, threshold)?.let { true } ?: false
    }
}

/**
 * 处理Live流程
 */
class LiveStartTask : Task() {
    override suspend fun run() {
        while (taskActive.value) {
            val rect = findMatchRect(driver.screenshot(), Templates.Start)
            if (rect != null) {
                flow<Unit> {
                    findMatchRect(driver.screenshot(), Templates.Recommend)!!.let {
                        driver.click(it.centerX(), it.centerY())
                        delay(3.seconds)
                    }
                }.retry(3).collect()
                driver.click(rect.centerX(), rect.centerY())
                break
            }
        }

        while (taskActive.value) {
            val screenshot = driver.screenshot()
            click(screenshot, Templates.Close)
            click(screenshot, Templates.Download)
            if (match(screenshot, Templates.LiveLogo)) {
                driver.longPress(500f, 1500f, 8000)
                continue
            }
            if (click(screenshot, Templates.Next)) {
                continue
            }
            if (match(screenshot, Templates.FailedNext) || match(
                    screenshot,
                    Templates.FailedLogo
                )
            ) {
                throw IllegalStateException("Detect failed live scene")
            }
            if (click(screenshot, Templates.Finish)) {
                break
            }
        }
    }

}

/**
 * 处理VenusTower清除任务
 */
class VenusTowerTask : Task() {
    private var liveStartTask: LiveStartTask? = null
    override suspend fun run() {
        while (taskActive.value) {
            findMatchRect(driver.screenshot(), Templates.Confirm)?.let {
                delay(1.seconds)
                driver.click(it.centerX(), it.centerY())
                delay(1.seconds)
                liveStartTask = LiveStartTask().apply {
                    run()
                }
            }
        }
    }

    override fun destroy() {
        super.destroy()
        liveStartTask?.destroy()
    }
}

/**
 * 主线Live
 */
class MasterLiveTask : Task() {
    private var liveStartTask: LiveStartTask? = null
    override suspend fun run() {
        while (taskActive.value) {
            findMatchRect(driver.screenshot(), Templates.LiveHomePage)?.let {
                driver.click(535f, 1350f)
                delay(2.seconds)
            }
            findMatchRect(driver.screenshot(), Templates.CompositionConfirm)?.let {
                driver.click(it.centerX(), it.centerY())
                liveStartTask = LiveStartTask().apply {
                    run()
                }
            }
        }
    }

    override fun destroy() {
        super.destroy()
        liveStartTask?.destroy()
    }

}