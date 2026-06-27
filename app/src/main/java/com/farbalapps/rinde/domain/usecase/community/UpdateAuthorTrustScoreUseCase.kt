package com.farbalapps.rinde.domain.usecase.community

import com.farbalapps.rinde.data.remote.model.CommunityPostDto
import com.farbalapps.rinde.domain.model.VerificationStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import java.util.Date
import kotlin.math.max

class UpdateAuthorTrustScoreUseCase @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend operator fun invoke(authorId: String) {
        try {
            // 1. Fetch all posts from the author
            val postsSnapshot = firestore.collection("posts")
                .whereEqualTo("authorId", authorId)
                .get().await()

            if (postsSnapshot.isEmpty) return

            var totalWeightedScore = 0f
            var totalWeight = 0f
            var verifiedCount = 0

            val now = Date().time

            val postsToUpdate = mutableListOf<String>()

            for (doc in postsSnapshot.documents) {
                val post = doc.toObject(CommunityPostDto::class.java) ?: continue
                postsToUpdate.add(doc.id)

                val truthCount = post.truthCount
                val falseCount = post.falseCount
                val totalVotes = truthCount + falseCount
                
                // Skip posts with too few votes to prevent statistical noise
                if (totalVotes < 3) continue

                // Calculate base score for post (0 to 5)
                var postScore = (truthCount.toFloat() / totalVotes.toFloat()) * 5f

                // Penalties and bonuses
                val status = VerificationStatus.valueOf(post.verificationStatus)
                if (status == VerificationStatus.EXPIRED || status == VerificationStatus.DISPUTED) {
                    postScore -= 0.5f
                }
                
                // Verified bonus (truth > 80%)
                if (truthCount.toFloat() / totalVotes.toFloat() >= 0.8f) {
                    postScore += 0.3f
                    verifiedCount++
                }

                postScore = postScore.coerceIn(0f, 5f)

                // Temporal weight (decay factor based on days since post)
                val daysSincePost = ((now - (post.timestamp?.time ?: now)) / (1000 * 60 * 60 * 24)).toFloat()
                val weight = 1f / (1f + max(0f, daysSincePost) * 0.05f)

                totalWeightedScore += (postScore * weight)
                totalWeight += weight
            }

            val finalTrustScore = if (totalWeight > 0f) {
                totalWeightedScore / totalWeight
            } else {
                0f
            }

            val trustLevel = when {
                finalTrustScore >= 4.5f -> "PLATINUM"
                finalTrustScore >= 3.5f -> "GOLD"
                finalTrustScore >= 2.5f -> "SILVER"
                finalTrustScore >= 1.5f -> "BRONZE"
                else -> "NEW"
            }

            // 2. Batch update user profile and all their posts
            firestore.runBatch { batch ->
                // Update User Profile
                val userRef = firestore.collection("users").document(authorId)
                batch.set(
                    userRef, 
                    mapOf(
                        "trustScore" to finalTrustScore,
                        "trustLevel" to trustLevel,
                        "verifiedPostsCount" to verifiedCount
                    ), 
                    SetOptions.merge()
                )

                // Update all posts from the author to show their new reputation
                for (postId in postsToUpdate) {
                    val postRef = firestore.collection("posts").document(postId)
                    batch.update(
                        postRef,
                        mapOf(
                            "authorTrustScore" to finalTrustScore,
                            "authorTrustLevel" to trustLevel
                        )
                    )
                }
            }.await()
            
            android.util.Log.d("UpdateAuthorTrustScore", "✅ Trust score for $authorId updated to $finalTrustScore")

        } catch (e: Exception) {
            android.util.Log.e("UpdateAuthorTrustScore", "❌ Error calculating trust score", e)
        }
    }
}
