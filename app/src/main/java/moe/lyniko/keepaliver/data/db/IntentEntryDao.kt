package moe.lyniko.keepaliver.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IntentEntryDao {

    @Query("SELECT * FROM intent_entries ORDER BY id ASC")
    fun getAllEntries(): Flow<List<IntentEntry>>

    @Query("SELECT * FROM intent_entries WHERE enabled = 1")
    suspend fun getEnabledEntries(): List<IntentEntry>

    @Query("SELECT * FROM intent_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): IntentEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: IntentEntry): Long

    @Update
    suspend fun updateEntry(entry: IntentEntry)

    @Delete
    suspend fun deleteEntry(entry: IntentEntry)
}
