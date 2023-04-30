package com.bretancezar.conversionapp.service

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.lifecycle.LiveData
import com.bretancezar.conversionapp.domain.Recording
import com.bretancezar.conversionapp.domain.SpeakerClass
import com.bretancezar.conversionapp.repository.RecordingRepository
import com.bretancezar.conversionapp.utils.*
import java.io.*
import java.time.LocalDateTime
import javax.inject.Inject

class StorageService @Inject constructor (
    private val repo: RecordingRepository,
    private val context: Context
) {

    init {

        // Create folders
        SpeakerClass.values().toList().forEach {

            File(getRecordingsFolderPath() + "/$it").mkdir()
        }
    }

    fun getBySpeakerClass(speakerClass: SpeakerClass): LiveData<List<Recording>> {

        return repo.findBySpeakerClass(speakerClass)
    }

    fun readRecording(speakerClass: SpeakerClass, filename: String): ByteArray {

        return readRecordingFile(getRecordingFile(speakerClass, filename))
    }

    fun addOriginalRecording(recording: Recording): Recording {

        return repo.save(recording)
    }

    fun addConvertedRecording(bytes: ByteArray, speakerClass: SpeakerClass, originalName: String, format: AudioFileFormats) {

        val currentDateTime = LocalDateTime.now()

        val filename = "${getFilenameWithoutExtension(originalName)}-conv-${speakerClass}.${format.toString().lowercase()}"

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

    fun renameRecording(id: Long, newName: String): Recording {

        val recording = repo.findById(id)

        if (newName.contains(regex = Regex("[|?*<\":>+/'\\[\\]]"))) {

            throw IllegalArgumentException("The new filename must not contain the following characters: | ? * < \" : > + / ' [ ]")
        }

        if (!checkFilenameHasExtension(newName, getFileExtension(recording.filename))) {

            throw IllegalArgumentException("The new filename must have the same extension as the original one.")
        }

        val speakerClass = recording.speakerClass
        val originalName = recording.filename

        val originalFile = getRecordingFile(speakerClass, originalName)
        val newFile = getRecordingFile(speakerClass, newName)

        if (originalFile.exists() && !newFile.exists()) {

            val ret = repo.updateFilenameById(id, newName)
            renameRecordingFile(originalFile, newFile)

            return ret
        }
        else if (newFile.exists() && originalName != newName) {

            throw IllegalArgumentException("A recording with the same name in the speaker class already exists.")
        }
        else if (originalName == newName) {

            return repo.findById(id)
        }
        else if (!originalFile.exists()) {

            throw IllegalStateException("The original recording to be renamed no longer exists.")
        }
        else {

            throw IllegalStateException("This should never be thrown")
        }
    }

    private fun getRecordingsFolderPath(): String {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)!!.absolutePath)
        }
        else {
            (context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)!!.absolutePath)
        }
    }

    fun getRecordingFile(speakerClass: SpeakerClass, filename: String): File {

        val path: String = getRecordingsFolderPath() + "/$speakerClass/$filename"

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