package com.example.frameexporter

import java.io.File

sealed interface FrameExporterState {

    data object Start: FrameExporterState
    data class ExtractionSuccess(val framesList: List<File>, val selected: List<File> = emptyList()): FrameExporterState
    data class Error(val message: String): FrameExporterState
    data object Extracting: FrameExporterState
    data object Exporting: FrameExporterState
    data class ExportSuccess(val message: String): FrameExporterState

}