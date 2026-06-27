package com.farbalapps.rinde.data.local.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room [TypeConverter]s for types that SQLite cannot store natively.
 *
 * Currently handles:
 * - [List]<[String]> ↔ JSON [String] (used for photo URL lists in [CommunityPostEntity]).
 *
 * Uses Gson for serialization. The result is a proper JSON array, which is safer
 * than the previous comma-separated approach (URLs can contain commas).
 */
class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}
