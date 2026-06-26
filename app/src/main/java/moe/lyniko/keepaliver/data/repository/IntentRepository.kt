package moe.lyniko.keepaliver.data.repository

import kotlinx.coroutines.flow.Flow
import moe.lyniko.keepaliver.data.db.IntentEntry
import moe.lyniko.keepaliver.data.db.IntentEntryDao

class IntentRepository(private val dao: IntentEntryDao) {

    fun getAllEntries(): Flow<List<IntentEntry>> = dao.getAllEntries()

    suspend fun getEnabledEntries(): List<IntentEntry> = dao.getEnabledEntries()

    suspend fun getEntryById(id: Long): IntentEntry? = dao.getEntryById(id)

    suspend fun insertEntry(entry: IntentEntry): Long = dao.insertEntry(entry)

    suspend fun updateEntry(entry: IntentEntry) = dao.updateEntry(entry)

    suspend fun deleteEntry(entry: IntentEntry) = dao.deleteEntry(entry)
}
