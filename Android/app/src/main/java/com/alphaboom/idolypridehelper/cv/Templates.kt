package com.alphaboom.idolypridehelper.cv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

data class Template(val name: String, val description: String = "")

object Templates {
    val Close = Template("close.png")
    val CompositionConfirm = Template("composition_confirm.png")
    val Confirm = Template("confirm.png")
    val Download = Template("download.png")
    val FailedLogo = Template("failed_logo.png")
    val FailedNext = Template("failed_next.png")
    val Finish = Template("finish.png")
    val LiveHomePage = Template("live_home_page.png")
    val LiveLogo = Template("live_logo.png")
    val Next = Template("next.png")
    val Recommend = Template("recommend.png")
    val Start = Template("start.png")
}

val templateCache = mutableMapOf<String, Bitmap>()

fun Template.getBitmap(context:Context): Bitmap {
    if (this.name in templateCache) {
        return templateCache[this.name]!!
    }
    context.assets.open(this.name).use {
        val bitmap = BitmapFactory.decodeStream(it)
        templateCache[this.name] = bitmap
        return bitmap
    }
}