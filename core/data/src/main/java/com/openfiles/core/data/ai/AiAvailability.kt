package com.openfiles.core.data.ai

import android.content.Context
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.imagedescription.ImageDescriberOptions
import com.google.mlkit.genai.imagedescription.ImageDescription
import com.google.mlkit.genai.summarization.Summarization
import com.google.mlkit.genai.summarization.SummarizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.guava.await
import javax.inject.Inject
import javax.inject.Singleton

enum class AiFeatureState { AVAILABLE, DOWNLOADABLE, DOWNLOADING, UNAVAILABLE }

/**
 * Runtime AICore/ML Kit GenAI availability check. Bonus layer per the Ring 3 feasibility audit:
 * narrow device support (Pixel/Samsung S24+/vivo X200-series-class devices, no emulator support
 * as of mid-2026) means callers must fully hide the corresponding UI -- not grey it out -- when
 * this reports UNAVAILABLE.
 */
@Singleton
class AiAvailability @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun imageDescriptionState(): AiFeatureState {
        val client = ImageDescription.getClient(ImageDescriberOptions.builder(context).build())
        return client.checkFeatureStatus().await().toAiFeatureState()
    }

    suspend fun summarizationState(): AiFeatureState {
        val client = Summarization.getClient(SummarizerOptions.builder(context).build())
        return client.checkFeatureStatus().await().toAiFeatureState()
    }

    private fun Int.toAiFeatureState(): AiFeatureState = when (this) {
        FeatureStatus.AVAILABLE -> AiFeatureState.AVAILABLE
        FeatureStatus.DOWNLOADABLE -> AiFeatureState.DOWNLOADABLE
        FeatureStatus.DOWNLOADING -> AiFeatureState.DOWNLOADING
        else -> AiFeatureState.UNAVAILABLE
    }
}
