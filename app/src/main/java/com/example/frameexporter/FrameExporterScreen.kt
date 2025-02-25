package com.example.frameexporter

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import java.io.File

@Composable
fun FrameExporterRoute() {
    val context = LocalContext.current
    val viewModel: FrameExporterViewModel = viewModel()
    val state by viewModel.screenStateFlow.collectAsStateWithLifecycle()
    FrameExporterScreen(
        state = state,
        onVisualMediaPicked = {
            viewModel.extractFrames(context, it)
        },
        restart = {
            viewModel.restart()
        },
        onSelectedFrame = {
            viewModel.onSelectFrame(it)
        },
        onClear = {
            viewModel.onClear()
        },
        onExportAll = {
            viewModel.onExportFrames(it, context)
        },
        onExportSelected = {
            viewModel.onExportFrames(it, context)
        }
    )
}


@Composable
fun FrameExporterScreen(
    modifier: Modifier = Modifier,
    state: FrameExporterState,
    onVisualMediaPicked: (Uri) -> Unit,
    restart: () -> Unit,
    onSelectedFrame: (File) -> Unit,
    onClear: () -> Unit,
    onExportAll: (List<File>) -> Unit,
    onExportSelected: (List<File>) -> Unit
) {

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        when (state) {
            is FrameExporterState.ExtractionSuccess -> {
                ExportingScreen(
                    state,
                    onSelectedFrame = onSelectedFrame,
                    onClear = onClear,
                    onExportAll = onExportAll,
                    onExportSelected = onExportSelected
                )
            }

            is FrameExporterState.Error -> {
                MessageDialog(
                    state.message,
                    modifier = Modifier.padding(8.dp),
                    onDismiss = restart
                )
            }

            is FrameExporterState.Start -> {
                val pickVisualMedia: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?> =
                    rememberLauncherForActivityResult(PickVisualMedia()) {
                        if (it != null) {
                            onVisualMediaPicked(it)
                        }
                    }
                Text(text = "Pick a video")
                Spacer(modifier = Modifier.padding(8.dp))
                Button(
                    onClick = {
                        pickVisualMedia.launch()
                    }
                ) {
                    Text(text = "Browse")
                }
            }

            is FrameExporterState.Extracting -> {
                LoadingDialog(modifier = Modifier.padding(8.dp), "Extracting")
            }

            is FrameExporterState.ExportSuccess -> {
                MessageDialog(
                    state.message,
                    modifier = Modifier.padding(8.dp),
                    onDismiss = restart
                )
            }

            is FrameExporterState.Exporting -> {
                LoadingDialog(modifier = Modifier.padding(8.dp), "Exporting")
            }
        }
    }
}

@Composable
fun LoadingDialog(modifier: Modifier, message: String) {
    Dialog(onDismissRequest = {}) {
        Card {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
                Text(text = message)
                LinearProgressIndicator()
            }
        }
    }
}

@Composable
fun MessageDialog(message: String, modifier: Modifier, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
                Text(text = message)
                Button(onClick = onDismiss) {
                    Text(text = "Close")
                }
            }
        }
    }
}

@Composable
fun ExportingScreen(
    state: FrameExporterState.ExtractionSuccess,
    onSelectedFrame: (File) -> Unit,
    onClear: () -> Unit,
    onExportAll: (List<File>) -> Unit,
    onExportSelected: (List<File>) -> Unit
) {

    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "Choose frames to extract", style = TextStyle(fontSize = 32.sp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = {
                onClear()
            }) {
                Text(text = "Clear")
            }
            Button(onClick = {
                onExportAll(state.framesList)
            }) {
                Text(text = "Export All")
            }
            Button(onClick = {
                onExportSelected(state.selected)
            }) {
                Text(text = "Export selected(${state.selected.size})")
            }
        }
        LazyVerticalGrid(columns = GridCells.Fixed(3)) {
            items(state.framesList) {
                val selected = it in state.selected
                Box() {
                    AsyncImage(
                        model = it.absolutePath.toUri(),
                        contentDescription = null,
                        modifier = Modifier.clickable {
                            onSelectedFrame(it)
                        }
                    )
                    if (selected) {
                        Checkbox(checked = true, onCheckedChange = null)
                    }
                }
            }
        }
    }
}

fun ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>.launch() {
    launch(PickVisualMediaRequest.Builder().build())
}