package com.alphaboom.idolypridehelper.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import com.alphaboom.idolypridehelper.debug
import com.alphaboom.idolypridehelper.script.TaskRuntime


class IdolyHelperAccessibilityService : AccessibilityService() {

    companion object {
        private const val IDOLY_PRIDE_PACKAGE_NAME = "game.qualiarts.idolypride"
        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val expectedComponentName =
                ComponentName(context, IdolyHelperAccessibilityService::class.java)

            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
                ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)

            while (colonSplitter.hasNext()) {
                val componentNameString = colonSplitter.next()
                val enabledService = ComponentName.unflattenFromString(componentNameString)

                if (enabledService != null && enabledService == expectedComponentName)
                    return true
            }
            return false
        }
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        debug("onAccessibilityEvent:${event?.eventType}")
        if (event == null || event.eventType != AccessibilityEvent.TYPE_VIEW_FOCUSED) {
            return
        }
        if (event.packageName == IDOLY_PRIDE_PACKAGE_NAME) {
            TaskRuntime.start()
        } else {
            TaskRuntime.stop()
        }
    }

    override fun onServiceConnected() {
        TaskRuntime.attachService(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        TaskRuntime.detachService()
        TaskRuntime.stop()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        TaskRuntime.stop()
    }

    override fun onInterrupt() {
        debug("onInterrupt")
    }
}