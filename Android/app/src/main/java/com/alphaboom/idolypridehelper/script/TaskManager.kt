package com.alphaboom.idolypridehelper.script

import kotlinx.coroutines.channels.Channel

object TaskManager{
    private var currentTask:Task? = null
    val channel = Channel<Task>()

    suspend fun sendTask(task:Task) {
        currentTask = task
        channel.send(task)
    }

    fun stopCurrentTask(){
        currentTask?.destroy()
    }

}