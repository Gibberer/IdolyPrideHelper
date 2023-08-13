package com.alphaboom.idolypridehelper.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alphaboom.idolypridehelper.script.MasterLiveTask
import com.alphaboom.idolypridehelper.script.TaskManager
import com.alphaboom.idolypridehelper.script.TaskRuntime
import com.alphaboom.idolypridehelper.script.VenusTowerTask
import com.alphaboom.idolypridehelper.service.IdolyHelperAccessibilityService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ServiceSettingViewModel(private val application: Application) :
    AndroidViewModel(application) {
    private val _state = MutableStateFlow(
        ServiceState(
            IdolyHelperAccessibilityService.isAccessibilityServiceEnabled(
                application
            ),
            activeToTaskState(TaskRuntime.taskActive.value)
        )
    )
    private var currentTaskJob: Job? = null

    val state = _state.asStateFlow()
    val selectedOptions = mutableStateOf(0)
    val allOptions = listOf(
        "Venus Tower",
        "Master Live"
    )

    init {
        // 监听状态
        viewModelScope.launch {
            TaskRuntime.taskActive.collect {
                _state.value = _state.value.copy(
                    taskState = activeToTaskState(it)
                )
            }
        }
    }

    private fun activeToTaskState(active: Boolean): TaskState {
        return if (active) {
            TaskState.ACTIVE
        } else {
            TaskState.INACTIVE
        }
    }

    fun refreshState() {
        _state.value = _state.value.copy(
            isServiceActive = IdolyHelperAccessibilityService.isAccessibilityServiceEnabled(
                application
            )
        )
    }

    fun jumpToAccessibilitySystemSetting(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    fun startTask() {
        when (selectedOptions.value) {
            0 -> VenusTowerTask()
            1 -> MasterLiveTask()
            else -> null
        }?.let {
            currentTaskJob = viewModelScope.launch {
                _state.value = _state.value.copy(taskState = TaskState.READY)
                TaskManager.sendTask(it)
            }
        }
    }

    fun stopTask() {
        currentTaskJob?.cancel()
        TaskManager.stopCurrentTask()
    }
}

enum class TaskState {
    ACTIVE, READY, INACTIVE
}

data class ServiceState(
    val isServiceActive: Boolean = false,
    val taskState: TaskState = TaskState.INACTIVE
)