package com.openfiles.core.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFTextShape
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Read-only .pptx text extraction per slide via Apache POI. Slide images are out of scope for v1. */
@Singleton
class SlidesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class Slide(val index: Int, val textBlocks: List<String>)

    suspend fun readSlides(uri: Uri): List<Slide> = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Can't open file: $uri")
        input.use { input ->
            XMLSlideShow(input).use { ppt ->
                ppt.slides.mapIndexed { i, slide ->
                    val texts = slide.shapes.filterIsInstance<XSLFTextShape>().map { it.text }
                    Slide(i, texts)
                }
            }
        }
    }
}
