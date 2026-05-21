package moe.styx.styx2m.player

import moe.styx.common.data.MediaPreferences

data class PlayerLanguagePreferences(
    val preferredAudioLanguages: List<String> = emptyList(),
    val preferredSubtitleLanguages: List<String> = emptyList(),
    val preferForcedSubtitles: Boolean = true
)

data class TrackSelectionInput(
    val tracks: List<PlayerTrack>,
    val mediaPreferences: MediaPreferences?,
    val languagePreferences: PlayerLanguagePreferences = PlayerLanguagePreferences(),
    val manualAudioOverrideId: Int? = null,
    val manualSubtitleOverrideId: Int? = null
)

data class TrackSelectionResult(
    val audioTrackId: Int?,
    val subtitleTrackId: Int?,
    val subtitlesDisabled: Boolean = false
)

object TrackPreferenceSelector {
    fun select(input: TrackSelectionInput): TrackSelectionResult {
        val audioTracks = input.tracks.filter { it.type == PlayerTrackType.AUDIO }
        val subtitleTracks = input.tracks.filter { it.type == PlayerTrackType.SUBTITLE }

        val audioTrack = input.manualAudioOverrideId
            ?: selectAudioTrack(audioTracks, input.mediaPreferences, input.languagePreferences)?.id

        val subtitleTrack = input.manualSubtitleOverrideId
            ?: selectSubtitleTrack(subtitleTracks, input.mediaPreferences, input.languagePreferences)?.id

        return TrackSelectionResult(
            audioTrackId = audioTrack,
            subtitleTrackId = subtitleTrack,
            subtitlesDisabled = subtitleTrack == null
        )
    }

    private fun selectAudioTrack(
        tracks: List<PlayerTrack>,
        mediaPreferences: MediaPreferences?,
        languagePreferences: PlayerLanguagePreferences
    ): PlayerTrack? {
        val preferred = buildList {
            if (mediaPreferences?.preferGermanDub == true) addAll(germanLanguageCodes)
            if (mediaPreferences?.preferEnglishDub == true) addAll(englishLanguageCodes)
            addAll(languagePreferences.preferredAudioLanguages)
        }

        return tracks.firstMatchingLanguage(preferred)
            ?: tracks.firstOrNull { it.isSelected }
            ?: tracks.firstOrNull { it.isDefault }
            ?: tracks.firstOrNull()
    }

    private fun selectSubtitleTrack(
        tracks: List<PlayerTrack>,
        mediaPreferences: MediaPreferences?,
        languagePreferences: PlayerLanguagePreferences
    ): PlayerTrack? {
        val preferred = buildList {
            if (mediaPreferences?.preferGermanSub == true) addAll(germanLanguageCodes)
            addAll(languagePreferences.preferredSubtitleLanguages)
        }

        if (preferred.isEmpty() && mediaPreferences?.preferGermanDub == true) {
            return tracks.firstOrNull { it.isForced && it.matchesAnyLanguage(germanLanguageCodes) }
        }

        return tracks.firstMatchingLanguage(preferred)
            ?: tracks.firstOrNull { it.isSelected }
            ?: tracks.firstOrNull { it.isDefault }
            ?: tracks.firstOrNull()
    }

    private fun List<PlayerTrack>.firstMatchingLanguage(languages: List<String>): PlayerTrack? {
        if (languages.isEmpty()) return null
        return firstOrNull { it.matchesAnyLanguage(languages) }
    }

    private fun PlayerTrack.matchesAnyLanguage(languages: List<String>): Boolean {
        val normalized = language?.trim()?.lowercase().orEmpty()
        val normalizedTitle = title?.trim()?.lowercase().orEmpty()
        return languages.any { language ->
            val candidate = language.trim().lowercase()
            normalized == candidate || normalizedTitle.contains(candidate)
        }
    }

    private val germanLanguageCodes = listOf("de", "deu", "ger", "german", "deutsch")
    private val englishLanguageCodes = listOf("en", "eng", "english")
}
