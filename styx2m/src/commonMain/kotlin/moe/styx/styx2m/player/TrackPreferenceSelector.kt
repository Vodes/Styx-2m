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

        val audioTrack = input.manualAudioOverrideId?.let { id ->
            audioTracks.firstOrNull { it.id == id }
        } ?: selectAudioTrack(audioTracks, input.mediaPreferences, input.languagePreferences)

        val subtitleTrack = when (input.manualSubtitleOverrideId) {
            -1 -> null
            null -> selectSubtitleTrack(subtitleTracks, audioTrack, input.mediaPreferences, input.languagePreferences)
            else -> subtitleTracks.firstOrNull { it.id == input.manualSubtitleOverrideId }
        }

        return TrackSelectionResult(
            audioTrackId = audioTrack?.id,
            subtitleTrackId = subtitleTrack?.id,
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
        selectedAudioTrack: PlayerTrack?,
        mediaPreferences: MediaPreferences?,
        languagePreferences: PlayerLanguagePreferences
    ): PlayerTrack? {
        if (mediaPreferences?.preferGermanSub != true) {
            when {
                mediaPreferences?.preferGermanDub == true && selectedAudioTrack?.matchesAnyLanguage(germanLanguageCodes) == true ->
                    return tracks.firstForcedMatchingLanguage(germanLanguageCodes)
                mediaPreferences?.preferEnglishDub == true && selectedAudioTrack?.matchesAnyLanguage(englishLanguageCodes) == true ->
                    return tracks.firstForcedMatchingLanguage(englishLanguageCodes)
            }
        }

        val preferred = buildList {
            if (mediaPreferences?.preferGermanSub == true) {
                addAll(germanLanguageCodes)
            }
            addAll(languagePreferences.preferredSubtitleLanguages)
        }

        return tracks.bestMatchingSubtitle(preferred, selectedAudioTrack)
            ?: tracks.firstOrNull { it.isSelected && !it.isForced }
            ?: tracks.firstOrNull { it.isDefault && !it.isForced }
            ?: tracks.firstOrNull { !it.isForced }
    }

    private fun List<PlayerTrack>.firstMatchingLanguage(languages: List<String>): PlayerTrack? {
        if (languages.isEmpty()) return null
        return languages.asSequence()
            .flatMap { it.languageAliases().asSequence() }
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .mapNotNull { language -> firstOrNull { it.matchesLanguage(language) } }
            .firstOrNull()
    }

    private fun List<PlayerTrack>.firstForcedMatchingLanguage(languages: List<String>): PlayerTrack? {
        if (languages.isEmpty()) return null
        return languages.asSequence()
            .flatMap { it.languageAliases().asSequence() }
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .mapNotNull { language -> firstOrNull { it.isForced && it.matchesLanguage(language) } }
            .firstOrNull()
    }

    private fun List<PlayerTrack>.bestMatchingSubtitle(languages: List<String>, selectedAudioTrack: PlayerTrack?): PlayerTrack? {
        if (languages.isEmpty()) return null

        return languages.asSequence()
            .flatMap { it.languageAliases().asSequence() }
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .mapNotNull { language ->
                val matchingTracks = filter { it.matchesLanguage(language) }
                if (matchingTracks.isEmpty()) return@mapNotNull null

                val audioMatchesSubtitleLanguage = selectedAudioTrack?.matchesLanguage(language) == true
                if (audioMatchesSubtitleLanguage) {
                    matchingTracks.firstOrNull { it.isForced }
                        ?: matchingTracks.firstOrNull { !it.isForced && it.isDefault }
                        ?: matchingTracks.firstOrNull { !it.isForced }
                        ?: matchingTracks.first()
                } else {
                    matchingTracks.firstOrNull { !it.isForced && it.isDefault }
                        ?: matchingTracks.firstOrNull { !it.isForced }
                }
            }
            .firstOrNull()
    }

    private fun PlayerTrack.matchesAnyLanguage(languages: List<String>): Boolean {
        return languages.any { language ->
            language.languageAliases().any { alias -> matchesLanguage(alias.trim().lowercase()) }
        }
    }

    private fun PlayerTrack.matchesLanguage(language: String): Boolean {
        if (language.isEmpty()) return false
        val normalized = this.language?.trim()?.lowercase().orEmpty()
        val normalizedTitle = title?.trim()?.lowercase().orEmpty()
        return normalized == language || normalizedTitle.contains(language)
    }

    private fun String.languageAliases(): List<String> {
        return when (trim().lowercase()) {
            "de", "deu", "ger", "german", "deutsch" -> germanLanguageCodes
            "en", "eng", "english" -> englishLanguageCodes
            "ja", "jp", "jpn", "japanese", "japanisch" -> japaneseLanguageCodes
            else -> listOf(this)
        }
    }

    private val germanLanguageCodes = listOf("de", "deu", "ger", "german", "deutsch")
    private val englishLanguageCodes = listOf("en", "eng", "english")
    private val japaneseLanguageCodes = listOf("ja", "jp", "jpn", "japanese", "japanisch")
}
