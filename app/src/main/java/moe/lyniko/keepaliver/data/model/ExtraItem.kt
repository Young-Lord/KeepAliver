package moe.lyniko.keepaliver.data.model

data class ExtraItem(
    val key: String,
    val value: String,
    val type: ExtraType
)

enum class ExtraType {
    STRING,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    BOOLEAN,
    URI,
    STRING_ARRAY
}
