package com.ramysaleeb.trailpack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = DataStorePackRepository(applicationContext.packDataStore)
        setContent {
            val model: PackViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = PackViewModel(repository) as T
            })
            val state by model.state.collectAsStateWithLifecycle()
            TrailPackTheme {
                PackScreen(state, model::setPacked, model::selectFilter, model::armFailure, model::retryRead)
            }
        }
    }
}

@Composable
fun TrailPackTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(
        primary = Color(0xFF225C45), onPrimary = Color.White,
        secondaryContainer = Color(0xFFE0EBDF), onSecondaryContainer = Color(0xFF173D2D),
        background = Color(0xFFF7F6F0), surface = Color(0xFFFFFEF9),
        onBackground = Color(0xFF202B25), onSurface = Color(0xFF202B25),
    ), content = content)
}

@Composable
fun PackScreen(
    state: PackState,
    onPacked: (String, Boolean) -> Unit,
    onFilter: (PackFilter) -> Unit,
    onFail: () -> Unit,
    onRetry: () -> Unit,
) {
    val filterFocus = remember { PackFilter.entries.associateWith { FocusRequester() } }
    var pendingFocusReturn by remember { mutableStateOf<String?>(null) }
    var retryFocused by remember { mutableStateOf(false) }
    val visibleIds = state.visibleGear.map { it.id }
    // Restore keyboard focus only when its row disappears. Do not force TalkBack focus.
    LaunchedEffect(visibleIds) {
        if (pendingFocusReturn != null && pendingFocusReturn !in visibleIds) {
            filterFocus.getValue(state.filter).requestFocus()
            pendingFocusReturn = null
        }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxSize()
                    .onPreviewKeyEvent { pendingFocusReturn = null; false }
                    .verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A LITTLE PREPARATION. MORE POSSIBILITY.", style = MaterialTheme.typography.labelSmall)
                    Text("Trail Pack", style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.semantics { heading() })
                    Text("A quiet checklist for your next day out.", style = MaterialTheme.typography.bodyLarge)
                }
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("THE WEEKEND EDIT", style = MaterialTheme.typography.labelMedium)
                        Text("${state.packedCount} of ${sampleGear.size} packed", style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        LinearProgressIndicator(progress = { state.packedCount.toFloat() / sampleGear.size },
                            modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
                            color = Color(0xFFC7E5AE), trackColor = Color(0xFF477C65))
                        Text("Saved on this device. Ready without a connection.")
                    }
                }
                // Wrap instead of shrinking controls when font size or display size increases.
                FlowRow(modifier = Modifier.focusGroup().semantics { isTraversalGroup = true },
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PackFilter.entries.forEach { filter ->
                        FilterChip(selected = state.filter == filter, onClick = { pendingFocusReturn = null; onFilter(filter) },
                            label = { Text(filter.name, modifier = Modifier.clearAndSetSemantics {}) },
                            modifier = Modifier.heightIn(min = 48.dp)
                                .focusRequester(filterFocus.getValue(filter))
                                .semantics { contentDescription = "${filter.name} items filter" })
                    }
                }
                if (state.loading) { Text("Loading your checklist…", Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                state.error?.let { error ->
                    Column {
                        Text(error, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        if (state.readFailed) TextButton(
                            onClick = {
                                pendingFocusReturn = null
                                if (retryFocused) filterFocus.getValue(state.filter).requestFocus()
                                onRetry()
                            },
                            modifier = Modifier.onFocusChanged { retryFocused = it.isFocused },
                        ) { Text("Retry reading") }
                    }
                }
                if (!state.loading && state.visibleGear.isEmpty()) {
                    Text(if (state.filter == PackFilter.Packed) "Nothing packed yet. Start with one small thing."
                        else "All packed. Enjoy the day out.",
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                state.visibleGear.forEach { gear ->
                    key(gear.id) {
                        var rowFocused by remember { mutableStateOf(false) }
                        val packed = gear.id in state.packedIds
                        Card {
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    .border(if (rowFocused) 3.dp else 0.dp,
                                        if (rowFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        MaterialTheme.shapes.medium)
                                    .onFocusChanged {
                                        rowFocused = it.isFocused
                                    }
                                    .testTag("gear-${gear.id}")
                                    .toggleable(
                                        value = packed, role = Role.Checkbox,
                                        enabled = !state.loading && !state.readFailed,
                                        onValueChange = {
                                            if (!state.saving) {
                                                pendingFocusReturn =
                                                    if (rowFocused && state.filter != PackFilter.All) gear.id else null
                                                onPacked(gear.id, it)
                                            }
                                        },
                                    ).padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Checkbox(checked = packed, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics {})
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(gear.name, modifier = Modifier.fillMaxWidth(), softWrap = true, style = MaterialTheme.typography.titleMedium)
                                    Text(gear.category, modifier = Modifier.fillMaxWidth(), softWrap = true, style = MaterialTheme.typography.labelSmall)
                                    Text(gear.detail, modifier = Modifier.fillMaxWidth(), softWrap = true, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Under the hood", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() })
                    Text("Kotlin · Jetpack Compose · DataStore\nAn original native Android architecture demo.")
                    OutlinedButton(onClick = { pendingFocusReturn = null; if (!state.saving && !state.failNextSave) onFail() },
                        enabled = !state.loading && !state.readFailed,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite }) {
                        Text(if (state.failNextSave) "Failure armed — tap an item" else "Simulate next save failure")
                    }
                    Text("Try it: arm a failure, then tap an item. The saved checklist stays unchanged. Tap again to recover.",
                        style = MaterialTheme.typography.bodySmall)
                    Text("Fictional sample checklist. No accounts, tracking, or network access.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
