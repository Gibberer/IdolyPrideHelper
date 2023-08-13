package com.alphaboom.idolypridehelper.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import com.alphaboom.idolypridehelper.debug
import org.opencv.android.OpenCVLoader

class ServiceSettingActivity:ComponentActivity() {
    private val viewModel by viewModels<ServiceSettingViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (OpenCVLoader.initDebug()){
            debug("opencv load debug success")
        }
        setContent {
            MaterialTheme {
                ServiceSettingView(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshState()
    }
}