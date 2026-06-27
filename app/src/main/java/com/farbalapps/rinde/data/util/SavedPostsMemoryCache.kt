package com.farbalapps.rinde.data.util

import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

@Singleton
class SavedPostsMemoryCache @Inject constructor() {
    private val cache = ConcurrentHashMap<String, Boolean>()

    fun setSaved(postId: String, isSaved: Boolean) {
        cache[postId] = isSaved
    }

    fun isSaved(postId: String): Boolean? {
        return cache[postId]
    }

    fun clear() {
        cache.clear()
    }
}
