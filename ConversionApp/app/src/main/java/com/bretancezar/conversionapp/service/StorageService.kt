package com.bretancezar.conversionapp.service

import android.app.Application
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.lifecycle.LiveData
import com.bretancezar.conversionapp.domain.Recording
import com.bretancezar.conversionapp.domain.SpeakerClass
import com.bretancezar.conversionapp.repository.RecordingRepository
import com.bretancezar.conversionapp.utils.formatToReadableDateTime
import java.io.*
import java.time.LocalDateTime
import javax.inject.Inject

class StorageService @Inject constructor(
    private val repo: RecordingRepository,
    private val context: Application
) {

    fun getBySpeakerClass(speakerClass: SpeakerClass): LiveData<List<Recording>> {

        return repo.findBySpeakerClass(speakerClass)
    }

    fun readRecording(speakerClass: SpeakerClass, filename: String): ByteArray {

        return readRecordingFile(getRecordingFile(speakerClass, filename))
    }

    fun addOriginalRecording(recording: Recording): Recording {

        return repo.save(recording)
    }

    fun addReceivedRecording(bytes: ByteArray, speakerClass: SpeakerClass) {

        val currentDateTime = LocalDateTime.now()

        val filename = "rec-${currentDateTime.formatToReadableDateTime()}-original.wav"

        writeRecordingFile(bytes, getRecordingFile(speakerClass, filename))

        repo.save(Recording(
            id = null,
            datetime = currentDateTime,
            speakerClass = speakerClass,
            filename = filename
        ))
    }

    fun deleteRecording(id: Long) {

        val recording = repo.findById(id)

        val file = getRecordingFile(recording.speakerClass, recording.filename)

        if (file.exists()) {

            deleteRecordingFile(file)
            repo.deleteById(id)
        }
        else {

            throw IllegalStateException("The file meant for deletion no longer exists or was not found on storage.")
        }
    }

    fun renameRecording(id: Long, newName: String) {

        val recording = repo.findById(id)

        val speakerClass = recording.speakerClass
        val originalName = recording.filename

        val originalFile = getRecordingFile(speakerClass, originalName)
        val newFile = getRecordingFile(speakerClass, newName)

        if (originalFile.exists() && !newFile.exists()) {

            renameRecordingFile(originalFile, newFile)

            repo.updateFilenameById(id, newName)
        }
        else if (newFile.exists() && originalName != newName) {

            throw IllegalArgumentException("A recording with the same name in the speaker class already exists.")
        }
        else if (!originalFile.exists()) {

            throw IllegalStateException("The original recording to be renamed no longed exists.")
        }
    }

    fun getRecordingFile(speakerClass: SpeakerClass, filename: String): File {

        val path: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            (context.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)!!.absolutePath) + "/$speakerClass/$filename"
        }
        else {

            (context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)!!.absolutePath) + "/$speakerClass/$filename"
        }

        return File(path)
    }

    private fun readRecordingFile(file: File): ByteArray {

        val size = file.length().toInt()
        val bytes = ByteArray(size)

        try {

            val buf = BufferedInputStream(FileInputStream(file))
            buf.read(bytes, 0, bytes.size)
            buf.close()

            Log.i("STORAGE", "File successfully read.")
        }
        catch (e: FileNotFoundException) {
            Log.e("STORAGE", e.stackTraceToString())
        }
        catch (e: IOException) {
            Log.e("STORAGE", e.stackTraceToString())
        }

        return bytes
    }

    private fun writeRecordingFile(bytes: ByteArray, file: File) {

        if (file.createNewFile()) {

            try {

                val buf = BufferedOutputStream(FileOutputStream(file))
                buf.write(bytes, 0, bytes.size)
                buf.close()

                Log.i("STORAGE", "File successfully written.")
            }
            catch (e: FileNotFoundException) {
                Log.e("STORAGE", e.stackTraceToString())
            }
            catch (e: IOException) {
                Log.e("STORAGE", e.stackTraceToString())
            }
        }
        else {

            throw IllegalArgumentException("A recording with the same name already exists.")
        }
    }

    /**
     *  Call this function only after ensuring the file does exist.
     */
    fun deleteRecordingFile(file: File) {

        if (file.delete()) {

            Log.i("STORAGE", "File successfully deleted.")
        }
        else {

            Log.e("STORAGE", "An unexpected error occurred on file deletion.")
        }
    }

    /**
     *  Call this function only after ensuring newFile doesn't already exist and that
     *  originalFile does exist.
     */
    private fun renameRecordingFile(originalFile: File, newFile: File) {

        if (originalFile.renameTo(newFile)) {

            Log.i("STORAGE", "File successfully renamed.")
        }
        else {

            Log.e("STORAGE", "An unexpected error occurred on file renaming.")
        }
    }
}