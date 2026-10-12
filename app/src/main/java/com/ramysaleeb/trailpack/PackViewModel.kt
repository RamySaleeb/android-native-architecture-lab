package com.ramysaleeb.trailpack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PackFilter { All, Remaining, Packed }

data class PackState(
    val packedIds: Set<String> = emptySet(),
    val filter: PackFilter = PackFilter.All,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val readFailed: Boolean = false,
    val failNextSave: Boolean = false,
) {
    val packedCount get() = sampleGear.count { it.id in packedIds }
    val visibleGear get() = sampleGear.filter {
        when (filter) {
            PackFilter.All -> true
            PackFilter.Remaining -> it.id !in packedIds
            PackFilter.Packed -> it.id in packedIds
        }
    }
}

/** UI actions flow in; immutable state flows out. Durable reads determine checkmarks. */
class PackViewModel(private val repository: PackRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(PackState())
    val state = mutableState.asStateFlow()
    private var readJob: Job? = null
    init { retryRead() }

    fun retryRead() {
        readJob?.cancel()
        mutableState.update { it.copy(loading = true, readFailed = false, error = null) }
        readJob = viewModelScope.launch {
            try {
                repository.packedIds.collect { ids ->
                    mutableState.update { it.copy(packedIds = ids, loading = false) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                mutableState.update { it.copy(loading = false, readFailed = true,
                    error = "Could not read your checklist. Retry to keep your saved data safe.") }
            }
        }
    }

    fun selectFilter(filter: PackFilter) { mutableState.update { it.copy(filter = filter) } }
    fun armFailure() { mutableState.update { it.copy(failNextSave = true, error = null) } }

    fun setPacked(id: String, packed: Boolean) {
        val current = state.value
        if (current.loading || current.saving || current.readFailed || sampleGear.none { it.id == id }) return
        val shouldFail = current.failNextSave
        mutableState.update { it.copy(saving = true, error = null, failNextSave = false) }
        viewModelScope.launch {
            try {
                if (shouldFail) throw IOException("Simulated write failure")
                repository.setPacked(id, packed)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                mutableState.update { it.copy(error = "Save failed. Your checklist is unchanged. Try the item again.") }
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }
}
