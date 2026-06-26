package moe.lyniko.keepaliver.executor

import moe.lyniko.keepaliver.data.model.ExtraItem
import moe.lyniko.keepaliver.data.model.ExtraType
import org.json.JSONArray
import org.json.JSONObject

object IntentExtrasParser {

    fun parse(extrasJson: String?): List<ExtraItem> {
        if (extrasJson.isNullOrBlank()) return emptyList()
        return try {
            val jsonArray = JSONArray(extrasJson)
            (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                ExtraItem(
                    key = obj.getString("key"),
                    value = obj.getString("value"),
                    type = ExtraType.valueOf(obj.getString("type"))
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun toJson(extras: List<ExtraItem>): String? {
        if (extras.isEmpty()) return null
        val jsonArray = JSONArray()
        extras.forEach { item ->
            val obj = JSONObject().apply {
                put("key", item.key)
                put("value", item.value)
                put("type", item.type.name)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }
}
