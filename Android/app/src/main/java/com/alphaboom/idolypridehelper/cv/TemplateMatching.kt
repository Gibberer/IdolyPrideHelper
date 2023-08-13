package com.alphaboom.idolypridehelper.cv

import android.graphics.Bitmap
import android.graphics.RectF
import com.alphaboom.idolypridehelper.debug
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

object TemplateMatching {
    init {
        OpenCVLoader.initDebug()
    }

    fun findMatchRect(
        templateBitmap: Bitmap,
        sourceBitmap: Bitmap,
        threshold: Float = 0.8f
    ): RectF? {
        val source = Mat().apply {
            Utils.bitmapToMat(sourceBitmap, this)
        }
        val template = Mat().apply {
            Utils.bitmapToMat(templateBitmap, this)
        }
        val resultCols = source.cols() - template.cols() + 1
        val resultRows = source.rows() - template.rows() + 1
        val result = Mat(resultRows, resultCols, CvType.CV_32FC1)
        Imgproc.matchTemplate(source, template, result, Imgproc.TM_CCOEFF_NORMED)
        val mmr = Core.minMaxLoc(result)
        if (mmr.maxVal > threshold) {
            return RectF(
                mmr.maxLoc.x.toFloat(),
                mmr.maxLoc.y.toFloat(),
                mmr.maxLoc.x.toFloat() + templateBitmap.width,
                mmr.maxLoc.y.toFloat() + templateBitmap.height
            )
        }
        return null
    }
}