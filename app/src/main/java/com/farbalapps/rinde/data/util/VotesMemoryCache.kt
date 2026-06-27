package com.farbalapps.rinde.data.util

import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

@Singleton
class VotesMemoryCache @Inject constructor() {
    private val cache = ConcurrentHashMap<String, Int>()

    fun setVote(postId: String, voteValue: Int) {
        cache[postId] = voteValue
    }

    fun getVote(postId: String): Int? {
        return cache[postId]
    }

    fun clear() {
        cache.clear()
    }
}
