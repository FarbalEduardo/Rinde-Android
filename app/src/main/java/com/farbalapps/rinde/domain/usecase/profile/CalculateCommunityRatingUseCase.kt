package com.farbalapps.rinde.domain.usecase.profile

import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.VerdictCalculator
import javax.inject.Inject

class CalculateCommunityRatingUseCase @Inject constructor() {
    operator fun invoke(posts: List<CommunityPost>): Pair<Float?, Int> {
        val eligiblePosts = posts.filter {
            (it.truthCount + it.falseCount) >= VerdictCalculator.MIN_VOTES_THRESHOLD
        }
        return if (eligiblePosts.isNotEmpty()) {
            val avgRatio = eligiblePosts.map {
                it.truthCount.toFloat() / (it.truthCount + it.falseCount)
            }.average().toFloat()
            val stars = (1f + avgRatio * 4f).coerceIn(1f, 5f)
            stars to eligiblePosts.size
        } else {
            null to 0
        }
    }
}
