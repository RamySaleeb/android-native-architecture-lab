package com.ramysaleeb.trailpack

import androidx.lifecycle.ViewModelStore
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PackViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }
    private fun model(repo: PackRepository): PackViewModel = PackViewModel(repo).also { store.put("pack", it) }

    @Test fun storedItemsLoadAndFilterWithoutMutatingStorage() = runTest(dispatcher) {
        val repo = FakeRepository(setOf("bottle"))
        val model = model(repo)
        runCurrent()
        assertFalse(model.state.value.loading)
        model.selectFilter(PackFilter.Remaining)
        assertEquals(5, model.state.value.visibleGear.size)
        assertFalse(model.state.value.visibleGear.any { it.id == "bottle" })
        model.selectFilter(PackFilter.Packed)
        assertEquals(listOf("bottle"), model.state.value.visibleGear.map { it.id })
        assertEquals(0, repo.writes)
    }

    @Test fun successfulSaveAndUnpackFollowRepository() = runTest(dispatcher) {
        val repo = FakeRepository()
        val model = model(repo)
        runCurrent()
        model.setPacked("bottle", true)
        runCurrent()
        assertEquals(setOf("bottle"), model.state.value.packedIds)
        model.setPacked("bottle", false)
        runCurrent()
        assertTrue(model.state.value.packedIds.isEmpty())
        assertEquals(2, repo.writes)
    }

    @Test fun simulatedFailureIsOneShotAndDoesNotWrite() = runTest(dispatcher) {
        val repo = FakeRepository()
        val model = model(repo)
        runCurrent()
        model.armFailure()
        model.setPacked("bottle", true)
        runCurrent()
        assertEquals(0, repo.writes)
        assertTrue(model.state.value.packedIds.isEmpty())
        assertNotNull(model.state.value.error)
        assertFalse(model.state.value.failNextSave)
        model.setPacked("bottle", true)
        runCurrent()
        assertEquals(setOf("bottle"), model.state.value.packedIds)
        assertNull(model.state.value.error)
    }

    @Test fun realWriteFailureRetainsLastSavedStateAndAllowsRetry() = runTest(dispatcher) {
        val repo = FakeRepository(setOf("map")).apply { failWrite = true }
        val model = model(repo)
        runCurrent()
        model.setPacked("bottle", true)
        runCurrent()
        assertEquals(setOf("map"), model.state.value.packedIds)
        assertNotNull(model.state.value.error)
        assertFalse(model.state.value.saving)
        repo.failWrite = false
        model.setPacked("bottle", true)
        runCurrent()
        assertEquals(setOf("map", "bottle"), model.state.value.packedIds)
    }

    @Test fun pendingWritePreventsOverlappingActionsAndOptimisticCheckmarks() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeRepository().apply { writeGate = gate }
        val model = model(repo)
        runCurrent()
        model.setPacked("bottle", true)
        model.setPacked("map", true)
        runCurrent()
        assertTrue(model.state.value.saving)
        assertTrue(model.state.value.packedIds.isEmpty())
        gate.complete(Unit)
        runCurrent()
        assertEquals(setOf("bottle"), model.state.value.packedIds)
        assertEquals(1, repo.writes)
    }

    @Test fun failedReadBlocksWritesUntilRetrySucceeds() = runTest(dispatcher) {
        val repo = FakeRepository(setOf("map")).apply { failRead = true }
        val model = model(repo)
        runCurrent()
        assertTrue(model.state.value.readFailed)
        model.setPacked("bottle", true)
        runCurrent()
        assertEquals(0, repo.writes)
        repo.failRead = false
        model.retryRead()
        runCurrent()
        assertFalse(model.state.value.readFailed)
        assertEquals(setOf("map"), model.state.value.packedIds)
        assertNull(model.state.value.error)
    }

    @Test fun unknownSavedIdsDoNotInflateProgressAndUnknownActionsAreIgnored() = runTest(dispatcher) {
        val repo = FakeRepository(setOf("obsolete", "map"))
        val model = model(repo)
        model.setPacked("bottle", true) // loading blocks mutation
        runCurrent()
        model.setPacked("unknown", true)
        runCurrent()
        assertEquals(1, model.state.value.packedCount)
        assertEquals(0, repo.writes)
    }
}

private class FakeRepository(initial: Set<String> = emptySet()) : PackRepository {
    private val data = MutableStateFlow(initial)
    var failRead = false
    var failWrite = false
    var writes = 0
    var writeGate: CompletableDeferred<Unit>? = null
    override val packedIds: Flow<Set<String>> = flow {
        if (failRead) throw IOException("Read failed")
        data.collect { emit(it) }
    }
    override suspend fun setPacked(id: String, packed: Boolean) {
        writes++
        writeGate?.await()
        if (failWrite) throw IOException("Write failed")
        data.value = if (packed) data.value + id else data.value - id
    }
}
