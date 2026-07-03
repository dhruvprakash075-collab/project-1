package com.openfiles.core.data

import com.openfiles.core.data.db.DocAnnotation
import com.openfiles.core.data.db.FileDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnotationRepository @Inject constructor(private val fileDao: FileDao) {
    fun annotationsForDocument(documentUri: String) = fileDao.annotationsForDocument(documentUri)

    suspend fun addAnnotation(documentUri: String, anchorIndex: Int, note: String, sheetName: String? = null) {
        fileDao.addAnnotation(
            DocAnnotation(
                documentUri = documentUri,
                anchorIndex = anchorIndex,
                sheetName = sheetName,
                note = note,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun removeAnnotation(annotation: DocAnnotation) = fileDao.removeAnnotation(annotation)
}
