package com.coverit.app

import android.app.Application
import com.coverit.app.image.FaceFinder
import com.coverit.app.image.Ocr
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class CoverItApp : Application() {

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(this)
        Ocr.install(this)
        FaceFinder.install(this)
    }
}
