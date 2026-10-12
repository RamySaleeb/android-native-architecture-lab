package com.ramysaleeb.trailpack

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class PackPersistenceTest {
    @Test fun packedItemsSurviveStoreRecreationAndConcurrentWrites() = runBlocking {
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "persistence-${System.nanoTime()}.preferences_pb")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val first = DataStorePackRepository(PreferenceDataStoreFactory.create(scope = scope) { file })
            coroutineScope {
                launch { first.setPacked("bottle", true) }
                launch { first.setPacked("map", true) }
            }
            assertEquals(setOf("bottle", "map"), first.packedIds.first())
            scope.coroutineContext[Job]!!.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val reopened = DataStorePackRepository(PreferenceDataStoreFactory.create(scope = scope) { file })
            assertEquals(setOf("bottle", "map"), reopened.packedIds.first())
            reopened.setPacked("map", false)
            assertEquals(setOf("bottle"), reopened.packedIds.first())
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
            file.delete()
        }
    }
}
