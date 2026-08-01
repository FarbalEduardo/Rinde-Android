package com.farbalapps.rinde.data.repository

import android.content.Context
import com.farbalapps.rinde.domain.model.Comment
import com.farbalapps.rinde.domain.model.Reply
import com.farbalapps.rinde.domain.repository.CommentRepository
import com.farbalapps.rinde.util.CloudinaryHelper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

import com.farbalapps.rinde.data.remote.model.CommentDto
import com.farbalapps.rinde.data.remote.model.ReplyDto
import com.farbalapps.rinde.data.mapper.toDomain
import com.farbalapps.rinde.data.mapper.toDto

class CommentRepositoryImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val rtdb: FirebaseDatabase,
    private val firestore: FirebaseFirestore
) : CommentRepository {

    override fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val ref = rtdb.getReference("comments").child(postId).orderByChild("timestamp")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val comments = snapshot.children.mapNotNull { child ->
                    try {
                        val dto = child.getValue(CommentDto::class.java)
                        dto?.copy(id = child.key ?: dto.id)?.toDomain()
                    } catch (e: Exception) {
                        android.util.Log.e("CommentRepository", "Error parsing comment ${child.key}: ${e.message}")
                        null
                    }
                }.reversed() // Más recientes primero
                trySend(comments)
            }

            override fun onCancelled(error: DatabaseError) {
                // «Permission denied» u otro error de RTDB: no cierres el Flow con excepción
                // porque eso mata el proceso. Simplemente logueamos y emitimos lista vacía.
                android.util.Log.e("CommentRepository", "Error al leer comentarios ($postId): ${error.message}")
                trySend(emptyList())
            }
        }
        
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun getReplies(commentId: String): Flow<List<Reply>> = callbackFlow {
        val ref = rtdb.getReference("replies").child(commentId).orderByChild("timestamp")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val replies = snapshot.children.mapNotNull { child ->
                    try {
                        val dto = child.getValue(ReplyDto::class.java)
                        dto?.copy(id = child.key ?: dto.id)?.toDomain()
                    } catch (e: Exception) {
                        android.util.Log.e("CommentRepository", "Error parsing reply ${child.key}: ${e.message}")
                        null
                    }
                }
                trySend(replies)
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("CommentRepository", "Error al leer respuestas ($commentId): ${error.message}")
                trySend(emptyList())
            }
        }
        
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun addComment(postId: String, comment: Comment, localImageUri: android.net.Uri?): Result<Unit> = runCatching {
        val imageUrl = uploadImageIfNeeded(localImageUri, comment.imageUrl)
        
        val commentRef = rtdb.getReference("comments").child(postId).push()
        val commentId = commentRef.key ?: throw Exception("Error generando ID de comentario")
        val finalComment = comment.copy(id = commentId, imageUrl = imageUrl)
        
        commentRef.setValue(buildCommentMap(finalComment)).await()
        
        incrementCommentCounts(postId, finalComment.authorId)
        notifyPostAuthor(postId, finalComment)
    }

    private suspend fun uploadImageIfNeeded(localImageUri: android.net.Uri?, currentImageUrl: String?): String? {
        if (localImageUri == null) return currentImageUrl
        
        val optimizedFile = com.farbalapps.rinde.util.ImageOptimizer.optimizeImage(context, localImageUri)
        return if (optimizedFile != null) {
            val uploadedUrl = CloudinaryHelper.uploadImage(optimizedFile.absolutePath, "Comentarios")
            optimizedFile.delete()
            uploadedUrl
        } else {
            currentImageUrl
        }
    }

    private fun buildCommentMap(comment: Comment): Map<String, Any> {
        return mapOf(
            "id" to comment.id,
            "postId" to comment.postId,
            "authorId" to comment.authorId,
            "authorName" to comment.authorName,
            "authorPhotoUrl" to (comment.authorPhotoUrl ?: ""),
            "text" to comment.text,
            "imageUrl" to (comment.imageUrl ?: ""),
            "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP,
            "likesCount" to comment.likesCount,
            "repliesCount" to comment.repliesCount,
            "isEdited" to comment.isEdited
        )
    }

    private suspend fun incrementCommentCounts(postId: String, authorId: String) {
        firestore.collection("posts").document(postId)
            .update("commentsCount", com.google.firebase.firestore.FieldValue.increment(1)).await()
        if (authorId.isNotEmpty()) {
            firestore.collection("users").document(authorId)
                .update("commentsCount", com.google.firebase.firestore.FieldValue.increment(1))
        }
    }

    private suspend fun notifyPostAuthor(postId: String, comment: Comment) {
        val postDoc = firestore.collection("posts").document(postId).get().await()
        val postAuthorId = postDoc.getString("authorId") ?: ""
        val postTitle = postDoc.getString("title") ?: "Tu oferta"
        if (postAuthorId.isNotEmpty() && postAuthorId != comment.authorId) {
            val notifId = java.util.UUID.randomUUID().toString()
            val notifData = hashMapOf(
                "id" to notifId,
                "type" to "NEW_COMMENT",
                "postId" to postId,
                "postTitle" to postTitle,
                "actorName" to comment.authorName,
                "actorPhotoUrl" to comment.authorPhotoUrl,
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false
            )
            firestore.collection("notifications").document(postAuthorId).collection("items").document(notifId).set(notifData)
        }
    }

    override suspend fun addReply(commentId: String, reply: Reply, localImageUri: android.net.Uri?): Result<Unit> = runCatching {
        val imageUrl = uploadImageIfNeeded(localImageUri, reply.imageUrl)
        
        val replyRef = rtdb.getReference("replies").child(commentId).push()
        val replyId = replyRef.key ?: throw Exception("Error generando ID de respuesta")
        val finalReply = reply.copy(id = replyId, imageUrl = imageUrl)
        
        replyRef.setValue(buildReplyMap(finalReply)).await()
        
        incrementReplyCounts(reply.postId, commentId, finalReply.authorId)
    }

    private fun buildReplyMap(reply: Reply): Map<String, Any> {
        return mapOf(
            "id" to reply.id,
            "commentId" to reply.commentId,
            "postId" to reply.postId,
            "authorId" to reply.authorId,
            "authorName" to reply.authorName,
            "authorPhotoUrl" to (reply.authorPhotoUrl ?: ""),
            "text" to reply.text,
            "imageUrl" to (reply.imageUrl ?: ""),
            "mentionedUser" to (reply.mentionedUser ?: ""),
            "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP,
            "likesCount" to reply.likesCount,
            "isEdited" to reply.isEdited
        )
    }

    private suspend fun incrementReplyCounts(postId: String, commentId: String, authorId: String) {
        rtdb.getReference("comments").child(postId).child(commentId)
            .child("repliesCount").setValue(com.google.firebase.database.ServerValue.increment(1)).await()

        firestore.collection("posts").document(postId)
            .update("commentsCount", com.google.firebase.firestore.FieldValue.increment(1)).await()

        if (authorId.isNotEmpty()) {
            firestore.collection("users").document(authorId)
                .update("commentsCount", com.google.firebase.firestore.FieldValue.increment(1))
        }
    }


    override suspend fun toggleCommentLike(userId: String, postId: String, commentId: String): Result<Unit> = runCatching {
        val likeRef = rtdb.getReference("comment_likes").child(commentId).child(userId)
        val commentRef = rtdb.getReference("comments").child(postId).child(commentId)
        
        val snapshot = likeRef.get().await()
        if (snapshot.exists()) {
            likeRef.removeValue().await()
            commentRef.child("likesCount").setValue(com.google.firebase.database.ServerValue.increment(-1)).await()
        } else {
            likeRef.setValue(System.currentTimeMillis()).await()
            commentRef.child("likesCount").setValue(com.google.firebase.database.ServerValue.increment(1)).await()
        }
    }

    override suspend fun toggleReplyLike(userId: String, commentId: String, replyId: String): Result<Unit> = runCatching {
        val likeRef = rtdb.getReference("reply_likes").child(replyId).child(userId)
        val replyRef = rtdb.getReference("replies").child(commentId).child(replyId)
        
        val snapshot = likeRef.get().await()
        if (snapshot.exists()) {
            likeRef.removeValue().await()
            replyRef.child("likesCount").setValue(com.google.firebase.database.ServerValue.increment(-1)).await()
        } else {
            likeRef.setValue(System.currentTimeMillis()).await()
            replyRef.child("likesCount").setValue(com.google.firebase.database.ServerValue.increment(1)).await()
        }
    }

    override suspend fun deleteComment(postId: String, commentId: String): Result<Unit> = runCatching {
        // 1. Obtener la cantidad de respuestas y autor del comentario
        val commentSnapshot = rtdb.getReference("comments").child(postId).child(commentId).get().await()
        val repliesCount = commentSnapshot.child("repliesCount").getValue(Long::class.java)?.toInt() ?: 0
        val authorId = commentSnapshot.child("authorId").getValue(String::class.java) ?: ""
        val totalToRemove = 1 + repliesCount

        // 2. Delete all replies from RTDB
        rtdb.getReference("replies").child(commentId).removeValue().await()
        // 3. Delete comment from RTDB
        rtdb.getReference("comments").child(postId).child(commentId).removeValue().await()
        // 4. Decrement total count in Firestore (post + autor del comentario)
        firestore.collection("posts").document(postId)
            .update("commentsCount", com.google.firebase.firestore.FieldValue.increment(-totalToRemove.toLong())).await()

        if (authorId.isNotEmpty()) {
            firestore.collection("users").document(authorId)
                .update("commentsCount", com.google.firebase.firestore.FieldValue.increment(-1))
        }
    }

    override suspend fun editComment(postId: String, commentId: String, newText: String): Result<Unit> = runCatching {
        val updates = mapOf<String, Any>(
            "text" to newText,
            "isEdited" to true,
            "editedAt" to com.google.firebase.database.ServerValue.TIMESTAMP
        )
        rtdb.getReference("comments").child(postId).child(commentId).updateChildren(updates).await()
    }

    override suspend fun deleteReply(commentId: String, replyId: String, postId: String): Result<Unit> = runCatching {
        val replySnapshot = rtdb.getReference("replies").child(commentId).child(replyId).get().await()
        val authorId = replySnapshot.child("authorId").getValue(String::class.java) ?: ""

        // Delete reply from RTDB
        rtdb.getReference("replies").child(commentId).child(replyId).removeValue().await()
        // Decrement repliesCount in the parent comment
        val commentRef = rtdb.getReference("comments").child(postId).child(commentId)
        commentRef.child("repliesCount").setValue(com.google.firebase.database.ServerValue.increment(-1)).await()

        // Decrement commentsCount in Firestore (-1 por cada reply eliminada)
        firestore.collection("posts").document(postId)
            .update("commentsCount", com.google.firebase.firestore.FieldValue.increment(-1)).await()

        if (authorId.isNotEmpty()) {
            firestore.collection("users").document(authorId)
                .update("commentsCount", com.google.firebase.firestore.FieldValue.increment(-1))
        }
    }

    override suspend fun editReply(commentId: String, replyId: String, newText: String): Result<Unit> = runCatching {
        val updates = mapOf<String, Any>(
            "text" to newText,
            "isEdited" to true,
            "editedAt" to com.google.firebase.database.ServerValue.TIMESTAMP
        )
        rtdb.getReference("replies").child(commentId).child(replyId).updateChildren(updates).await()
    }

    override suspend fun reportComment(reportedComment: com.farbalapps.rinde.domain.model.ReportedComment): Result<Unit> = runCatching {
        val docRef = firestore.collection("reported_comments").document()
        val finalReport = reportedComment.copy(reportId = docRef.id)
        docRef.set(finalReport).await()
    }

}
