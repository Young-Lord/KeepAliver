package moe.lyniko.keepaliver.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import moe.lyniko.keepaliver.data.model.ExecutionMode
import moe.lyniko.keepaliver.data.model.IntentType

@Entity(tableName = "intent_entries")
data class IntentEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val intentType: IntentType = IntentType.ACTIVITY,
    val targetPackage: String,
    val targetClass: String? = null,
    val action: String? = null,
    val dataUri: String? = null,
    val category: String? = null,
    val flags: Int? = null,
    val extrasJson: String? = null,
    val useForegroundService: Boolean = false,
    val executionMode: ExecutionMode? = null
)
