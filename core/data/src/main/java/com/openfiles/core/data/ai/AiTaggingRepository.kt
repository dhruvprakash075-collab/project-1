package com.openfiles.core.data.ai

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.genai.imagedescription.ImageDescriberOptions
import com.google.mlkit.genai.imagedescription.ImageDescription
import com.google.mlkit.genai.imagedescription.ImageDescriptionRequest
import com.google.mlkit.genai.summarization.Summarization
import com.google.mlkit.genai.summarization.SummarizationRequest
import com.google.mlkit.genai.summarization.SummarizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device only (no network calls) image tagging + document summarization via ML Kit GenAI /
 * AICore. Callers must check AiAvailability first and hide this functionality entirely when
 * UNAVAILABLE -- see AiAvailability's kdoc.
 */
@Singleton
class AiTaggingRepository @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun describeImage(bitmap: Bitmap): String {
        val describer = ImageDescription.getClient(ImageDescriberOptions.builder(context).build())
        val request = ImageDescriptionRequest.builder(bitmap).build()
        return describer.runInference(request).await().description
    }

    suspend fun summarize(text: String): String {
        val summarizer = Summarization.getClient(SummarizerOptions.builder(context).build())
        val request = SummarizationRequest.builder(text).build()
        return summarizer.runInference(request).await().summary
    }
}
