package com.farbalapps.rinde.data.util

import android.content.Context
import com.farbalapps.rinde.domain.model.CatalogItem
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonAssetReader @Inject constructor() {

    fun readCatalogFromAssets(context: Context, fileName: String): List<CatalogItem> {
        return try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            val items = mutableListOf<CatalogItem>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                items.add(
                    CatalogItem(
                        id = obj.getInt("id"),
                        nombre = obj.getString("nombre"),
                        categoria = obj.getString("categoria"),
                        emoji = obj.getString("emoji")
                    )
                )
            }
            items
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
