package com.bretancezar.conversionapp.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Environment
import com.bretancezar.conversionapp.domain.SpeakerClass
import java.io.File
import javax.inject.Inject

class PlayerService @Inject constructor (
    private var _context: Context
) {

    private var _mediaPlayer: MediaPlayer? = null

    fun initPlayer(filename: String, speakerClass: SpeakerClass) {

        if (_mediaPlayer != null) {
            destroyPlayer()
        }

        val path: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (_context.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)!!.absolutePath) + "/$speakerClass/$filename"
        }
        else {
            (_context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)!!.absolutePath) + "/$speakerClass/$filename"
        }

        val file = File(path)

        if (file.exists()) {
            _mediaPlayer = MediaPlayer()
            _mediaPlayer!!.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            _mediaPlayer!!.setDataSource(path)
            _mediaPlayer!!.prepare()
        }
        else {

            throw IllegalStateException("This should never be thrown")
        }
    }

    fun startPlayer() {

        if (_mediaPlayer != null) {
            _mediaPlayer!!.start()
        }
        else {
            throw IllegalStateException("This should never be thrown")
        }
    }

    fun pausePlayer() {

        if (_mediaPlayer != null) {
            _mediaPlayer!!.pause()
        }
        else {
            throw IllegalStateException("This should never be thrown")
        }
    }

    fun resetPlayer() {

        if (_mediaPlayer != null) {
            _mediaPlayer!!.run {
                stop()
                prepare()
            }
        }
        else {
            throw IllegalStateException("This should never be thrown")
        }
    }

    fun destroyPlayer() {

        _mediaPlayer?.run {
            stop()
            release()
        }
        _mediaPlayer = null
    }

    fun getPlayerPosition(): Int {

        if (_mediaPlayer != null) {
            return _mediaPlayer!!.currentPosition
        }
        throw IllegalStateException("This should never be thrown")
    }

    fun setPlayerPosition(ms: Int) {

        if (_mediaPlayer != null) {
            _mediaPlayer!!.seekTo(ms)
        }
        else {
            throw IllegalStateException("This should never be thrown")
        }
    }

    fun getMediaLength(): Int {

        if (_mediaPlayer != null) {
            return _mediaPlayer!!.duration
        }
        throw IllegalStateException("This should never be thrown")
    }
}