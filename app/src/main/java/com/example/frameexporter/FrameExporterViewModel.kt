package com.example.frameexporter

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arthenica.ffmpegkit.FFmpegKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class FrameExporterViewModel : ViewModel() {

    private val _frameExporterStateFlow: MutableStateFlow<FrameExporterState> =
        MutableStateFlow(FrameExporterState.Start)
    val screenStateFlow = _frameExporterStateFlow.asStateFlow()

    private fun launchFramesExtraction(filePath: String, outputDir: File, fileName: String) {

        val outputPath = "${outputDir.absolutePath}/${fileName}_frame_%04d.png"
        val command = "-i $filePath -vf fps=30 $outputPath"
        FFmpegKit.executeAsync(command) { session ->
            val state = session.state
            val returnCode = session.returnCode

            if (returnCode.isValueSuccess) {
                Log.d("FRAMES", "Frames extracted successfully")
                updateState(
                    FrameExporterState.ExtractionSuccess(
                        outputDir.listFiles()?.toList() ?: emptyList()
                    )
                )
            } else {
                Log.d("FRAMES", "Failed to extract frames: $returnCode")
                updateState(FrameExporterState.Error("Failed to extract frames"))
            }
        }
    }

    fun extractFrames(context: Context, uri: Uri) {
        updateState(FrameExporterState.Extracting)
        val filePath = getRealPathFromURI(context, uri)
        val fileName = getFileNameFromURI(context, uri)
        launchFramesExtraction(filePath.toString(), createTempFolder(context), fileName.toString())
    }

    fun onSelectFrame(file: File) {
        val currentState = screenStateFlow.value as? FrameExporterState.ExtractionSuccess ?: return
        val newList =
            if (currentState.selected.contains(file)) currentState.selected - file else currentState.selected + file
        updateState(
            currentState.copy(
                selected = newList
            )
        )
    }

    private fun getRealPathFromURI(context: Context, uri: Uri): String? {
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            return if (cursor.moveToFirst()) cursor.getString(columnIndex) else null
        }
        return null
    }

    private fun getFileNameFromURI(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
            return if (cursor.moveToFirst()) cursor.getString(columnIndex) else null
        }
        return null
    }

    private fun createTempFolder(context: Context): File {
        val tempDir = context.cacheDir.resolve("framesTemp").also {
            it.deleteRecursively()
            it.mkdirs()
        }
        return tempDir
    }

    fun onClear() {
        updateState(FrameExporterState.Start)
    }

    fun onExportFrames(framesList: List<File>, context: Context) {

        updateState(FrameExporterState.Exporting)
        viewModelScope.launch(Dispatchers.IO) {
            framesList.forEach {
                saveFileToDownloads(context, it)
            }

            withContext(Dispatchers.Main) {
                updateState(FrameExporterState.ExportSuccess("Frames exported succesfully"))
            }
        }
    }

    fun saveFileToDownloads(context: Context, sourceFile: File) {

        val contentResolver: ContentResolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, sourceFile.name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
        }

        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            try {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    FileInputStream(sourceFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun updateState(state: FrameExporterState) {
        viewModelScope.launch {
            _frameExporterStateFlow.emit(state)
        }
    }

    fun restart() {
        updateState(FrameExporterState.Start)
    }
}