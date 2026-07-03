package com.openfiles.core.data

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

data class DocSearchMatch(
    val anchorIndex: Int,
    val sheetName: String? = null,
    val snippet: String,
)

/**
 * Searches already-extracted Office document text (Word paragraphs, Excel cells, PPTX slide text)
 * for a query string. PDF text search is intentionally out of scope for v1 -- see the F3 section
 * intro in the Ring 3 playbook for why.
 */
@Singleton
class DocumentSearchRepository @Inject constructor(
    private val wordRepository: WordRepository,
    private val excelRepository: ExcelRepository,
    private val slidesRepository: SlidesRepository,
) {
    suspend fun searchWord(uri: Uri, query: String): List<DocSearchMatch> {
        if (query.isBlank()) return emptyList()
        return wordRepository.readParagraphs(uri).mapIndexedNotNull { index, paragraph ->
            if (paragraph.contains(query, ignoreCase = true)) {
                DocSearchMatch(anchorIndex = index, snippet = paragraph.take(SNIPPET_LENGTH))
            } else {
                null
            }
        }
    }

    suspend fun searchSlides(uri: Uri, query: String): List<DocSearchMatch> {
        if (query.isBlank()) return emptyList()
        return slidesRepository.readSlides(uri).flatMap { slide ->
            slide.textBlocks.filter { it.contains(query, ignoreCase = true) }
                .map { block -> DocSearchMatch(anchorIndex = slide.index, snippet = block.take(SNIPPET_LENGTH)) }
        }
    }

    suspend fun searchExcel(uri: Uri, query: String): List<DocSearchMatch> {
        if (query.isBlank()) return emptyList()
        return excelRepository.readWorkbook(uri).flatMap { sheet ->
            sheet.rows.mapIndexedNotNull { rowIndex, row ->
                val match = row.firstOrNull { it.contains(query, ignoreCase = true) }
                match?.let { DocSearchMatch(anchorIndex = rowIndex, sheetName = sheet.name, snippet = it.take(SNIPPET_LENGTH)) }
            }
        }
    }

    private companion object {
        const val SNIPPET_LENGTH = 80
    }
}
