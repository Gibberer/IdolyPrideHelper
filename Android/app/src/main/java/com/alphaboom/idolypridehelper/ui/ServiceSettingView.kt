package com.alphaboom.idolypridehelper.ui

import android.app.Application
import android.provider.MediaStore.Audio.Radio
import android.widget.RadioGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ServiceSettingView(viewModel: ServiceSettingViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val (selectedOption, onOptionSelected) = viewModel.selectedOptions
    Column(modifier = Modifier.padding(16.dp)) {
        if (state.isServiceActive) {
            Text(text = "当前无障碍服务已启用")
            OutlinedButton(onClick = { viewModel.jumpToAccessibilitySystemSetting(context) }) {
                Text(text = "关闭")
            }
        } else {
            Text(text = "无障碍服务未启用")
            OutlinedButton(onClick = { viewModel.jumpToAccessibilitySystemSetting(context) }) {
                Text(text = "开启")
            }
        }
        viewModel.allOptions.forEachIndexed { index, option ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                onOptionSelected(index)
            }){
                RadioButton(
                    selected = index == selectedOption,
                    onClick = { onOptionSelected(index) })
                Text(text = option)
            }
        }
        when(state.taskState) {
            TaskState.ACTIVE -> {
                OutlinedButton(onClick = { viewModel.stopTask() }) {
                    Text(text = "停止")
                }
            }
            TaskState.INACTIVE -> {
                OutlinedButton(onClick = { viewModel.startTask() }) {
                    Text(text = "开始")
                }
            }
            TaskState.READY -> {
                OutlinedButton(onClick = { }, enabled = false) {
                    Text(text = "准备就绪")
                }
            }
        }
    }
}

@Preview
@Composable
fun Preview() {
    ServiceSettingView(ServiceSettingViewModel(Application()))
}