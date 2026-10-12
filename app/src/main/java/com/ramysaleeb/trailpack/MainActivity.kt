package com.ramysaleeb.trailpack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("A LITTLE PREPARATION. MORE POSSIBILITY.", style = MaterialTheme.typography.labelSmall)
                        Text("Trail Pack", style = MaterialTheme.typography.displayMedium,
                            modifier = Modifier.semantics { heading() })
                        Text("A quiet checklist for your next day out.", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                item {
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
                }
                item {
                    // Wrap instead of shrinking controls when font size or display size increases.
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PackFilter.entries.forEach { filter ->
                            FilterChip(selected = state.filter == filter, onClick = { onFilter(filter) },
                                label = { Text(filter.name) })
                        }
                    }
                }
                if (state.loading) item { Text("Loading your checklist…") }
                state.error?.let { error ->
                    item {
                        Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                            Text(error, color = MaterialTheme.colorScheme.error)
                            if (state.readFailed) TextButton(onClick = onRetry) { Text("Retry reading") }
                        }
                    }
                }
                if (!state.loading && state.visibleGear.isEmpty()) item {
                    Text(if (state.filter == PackFilter.Packed) "Nothing packed yet. Start with one small thing."
                        else "All packed. Enjoy the day out.")
                }
                items(state.visibleGear, key = { it.id }) { gear ->
                    val packed = gear.id in state.packedIds
                    Card {
                        Row(
                            Modifier.fillMaxWidth().toggleable(
                                value = packed, role = Role.Checkbox,
                                enabled = !state.loading && !state.saving && !state.readFailed,
                                onValueChange = { onPacked(gear.id, it) },
                            ).padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Checkbox(checked = packed, onCheckedChange = null)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(gear.category.uppercase(), style = MaterialTheme.typography.labelSmall)
                                Text(gear.name, style = MaterialTheme.typography.titleMedium)
                                Text(gear.detail, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                item {
                    HorizontalDivider()
                    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Under the hood", style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() })
                        Text("Kotlin · Jetpack Compose · DataStore\nAn original native Android architecture demo.")
                        OutlinedButton(onClick = onFail,
                            enabled = !state.loading && !state.saving && !state.readFailed && !state.failNextSave) {
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
}
