package com.openfiles.core.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import javax.inject.Inject
import javax.inject.Singleton

/** Read-only .docx text extraction via Apache POI (paragraph-by-paragraph, no rich formatting in v1). */
@Singleton
class WordRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun readParagraphs(uri: Uri): List<String> = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)!!.use { input ->
            XWPFDocument(input).use { doc ->
                doc.paragraphs.map { it.text }
            }
        }
    }
}
