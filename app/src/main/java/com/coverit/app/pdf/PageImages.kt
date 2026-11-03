package com.coverit.app.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject

object PageImages {

    fun pages(doc: PDDocument): Set<Int> = (0 until doc.numberOfPages).filter { i ->
        runCatching { hasImage(doc.getPage(i).resources, 0) }.getOrDefault(true)
    }.toSet()

    private fun hasImage(resources: PDResources?, depth: Int): Boolean {
        if (resources == null || depth > 4) return false
        return resources.xObjectNames.any { name ->
            when (val x = resources.getXObject(name)) {
                is PDImageXObject -> x.width >= 48 && x.height >= 48
                is PDFormXObject -> hasImage(x.resources, depth + 1)
                else -> false
            }
        }
    }
}
