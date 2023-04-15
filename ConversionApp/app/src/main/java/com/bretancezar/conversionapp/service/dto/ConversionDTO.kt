package com.bretancezar.conversionapp.service.dto

data class ConversionDTO(

    val targetSpeaker: String,
    val audioFormat: String,
    val sampleRate: Int,
    val audioData: String
) {
    override fun equals(other: Any?): Boolean {

        if (this === other) return true

        if (other !is ConversionDTO) return false

        if (targetSpeaker != other.targetSpeaker) return false

        if (audioFormat != other.audioFormat) return false

        if (sampleRate != other.sampleRate) return false

        return audioData.contentEquals(other.audioData)
    }

    override fun hashCode(): Int {

        var result = targetSpeaker.hashCode()

        result = 31 * result + audioFormat.hashCode()

        result = 31 * result + sampleRate

        result = 31 * result + audioData.hashCode()

        return result
    }
}