package com.ramysaleeb.trailpack

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Gear(val id: String, val name: String, val category: String, val detail: String)

// Fictional sample checklist, not outdoor safety guidance.
val sampleGear = listOf(
    Gear("bottle", "Water bottle", "Essentials", "Refill before you leave"),
    Gear("map", "Pocket map", "Essentials", "A little room for a detour"),
    Gear("jacket", "Light jacket", "Clothing", "For the cooler part of the day"),
    Gear("cap", "Everyday cap", "Clothing", "Keep it within reach"),
    Gear("camera", "Compact camera", "Extras", "Bring home a few good frames"),
    Gear("notebook", "Field notebook", "Extras", "Make space for an observation"),
)

interface PackRepository {
    val packedIds: Flow<Set<String>>
    suspend fun setPacked(id: String, packed: Boolean)
}

val Context.packDataStore by preferencesDataStore(name = "trail_pack")

/** One durable source of truth. DataStore serializes read-modify-write transactions. */
class DataStorePackRepository(private val store: DataStore<Preferences>) : PackRepository {
    private val key = stringSetPreferencesKey("packed_ids_v1")
    override val packedIds = store.data.map { it[key].orEmpty().toSet() }
    override suspend fun setPacked(id: String, packed: Boolean) {
        require(sampleGear.any { it.id == id }) { "Unknown gear: $id" }
        store.edit { prefs ->
            val current = prefs[key].orEmpty()
            prefs[key] = if (packed) current + id else current - id
        }
    }
}
