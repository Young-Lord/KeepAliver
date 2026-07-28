package moe.lyniko.keepaliver.data.db

import androidx.room.TypeConverter
import moe.lyniko.keepaliver.data.model.ExecutionMode
import moe.lyniko.keepaliver.data.model.ExtraItem
import moe.lyniko.keepaliver.data.model.ExtraType
import moe.lyniko.keepaliver.data.model.IntentType
import org.json.JSONArray
import org.json.JSONObject

class Converters {

    @TypeConverter
    fun intentTypeToString(value: IntentType): String = value.name

    @TypeConverter
    fun stringToIntentType(value: String): IntentType = IntentType.valueOf(value)

    @TypeConverter
    fun executionModeToString(value: ExecutionMode?): String? {
        return value?.name
    }

    @TypeConverter
    fun stringToExecutionMode(value: String?): ExecutionMode? {
        return value?.let { runCatching { ExecutionMode.valueOf(it) }.getOrNull() }
    }

    @TypeConverter
    fun extraListToJson(value: List<ExtraItem>?): String? {
        if (value == null) return null
        val jsonArray = JSONArray()
        value.forEach { item ->
            val obj = JSONObject().apply {
                put("key", item.key)
                put("value", item.value)
                put("type", item.type.name)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    @TypeConverter
    fun jsonToExtraList(value: String?): List<ExtraItem>? {
        if (value == null) return null
        val jsonArray = JSONArray(value)
        return (0 until jsonArray.length()).map { i ->
            val obj = jsonArray.getJSONObject(i)
            ExtraItem(
                key = obj.getString("key"),
                value = obj.getString("value"),
                type = ExtraType.valueOf(obj.getString("type"))
            )
        }
    }
}
