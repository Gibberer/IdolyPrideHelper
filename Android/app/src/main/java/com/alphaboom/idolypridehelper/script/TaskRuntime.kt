package com.alphaboom.idolypridehelper.script

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Path
import android.view.Display
import com.alphaboom.idolypridehelper.debug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object TaskDriver {
    private const val MIN_TAKE_SCREENSHOT_INTERVAL = 500
    private var serviceReference: WeakReference<AccessibilityService>? = null
    private val screenshotExecutor = Executors.newSingleThreadExecutor()
    private var lastTakeScreenshotTime = 0L
    val context: Context?
        get() = serviceReference?.get()

    fun attachService(accessibilityService: AccessibilityService) {
        serviceReference = WeakReference(accessibilityService)
    }

    fun detachService() {
        serviceReference?.clear()
        serviceReference = null
    }

    suspend fun screenshot(): Bitmap = withService {
        val delta = System.currentTimeMillis() - lastTakeScreenshotTime
        if (delta < MIN_TAKE_SCREENSHOT_INTERVAL) {
            delay(MIN_TAKE_SCREENSHOT_INTERVAL - delta)
        }
        lastTakeScreenshotTime = System.currentTimeMillis()
        suspendCoroutine {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                screenshotExecutor,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                        val hardwareBitmap = Bitmap.wrapHardwareBuffer(
                            screenshot.hardwareBuffer,
                            screenshot.colorSpace
                        )
                        val softwareBitmap = hardwareBitmap!!.copy(Bitmap.Config.ARGB_8888, false)
                        screenshot.hardwareBuffer.close()
                        hardwareBitmap.recycle()
                        it.resume(softwareBitmap)
                    }

                    override fun onFailure(errorCode: Int) {
                        it.resume(Bitmap.createBitmap(1920, 2400, Bitmap.Config.ARGB_8888))
                        debug("take screenshot failed: $errorCode")
                    }

                })
        }

    }!!

    suspend fun click(x: Float, y: Float) = withService {
        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    Path().apply {
                        moveTo(x, y)
                    }, 0, 1
                )
            ).build()
        suspendCoroutine {
            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    it.resume(Unit)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    it.resume(Unit)
                }
            }, null)
        }
    }

    suspend fun longPress(x: Float, y: Float, duration: Long = 2000L) = withService {
        val gesture = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    Path().apply {
                        moveTo(x, y)
                    }, 0, duration
                )
            ).build()
        suspendCoroutine {
            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    it.resume(Unit)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    it.resume(Unit)
                }
            }, null)
        }
    }

    private suspend fun <T> withService(block: suspend AccessibilityService.() -> T): T? {
        return serviceReference?.get()?.run {
            block.invoke(this)
        }
    }
}


object TaskRuntime {
    @OptIn(DelicateCoroutinesApi::class)
    private val scope = CoroutineScope(SupervisorJob() + newSingleThreadContext("TaskRuntime"))
    private var taskFlow = TaskManager.channel.receiveAsFlow()
    private var currentJob: Job? = null
    private val _taskActive = MutableStateFlow(false)
    val taskActive = _taskActive.asStateFlow()

    fun attachService(accessibilityService: AccessibilityService) {
        TaskDriver.attachService(accessibilityService)
    }

    fun detachService() {
        TaskDriver.detachService()
    }

    fun start() {
        if (currentJob == null || !currentJob!!.isActive) {
            currentJob = scope.launch {
                taskFlow.collect {
                    try {
                        _taskActive.value = true
                        it.run()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        it.destroy()
                        _taskActive.value  = false
                    }
                }
            }
        }
    }

    fun stop() {
        currentJob?.cancel()
        currentJob = null
    }
}