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
                val comments = snapshot.children.mapNotNull { it.getValue(CommentDto::class.java)?.toDomain() }
                    .reversed() // Más recientes primero
                trySend(comments)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override fun getReplies(commentId: String): Flow<List<Reply>> = callbackFlow {
        val ref = rtdb.getReference("replies").child(commentId).orderByChild("timestamp")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val replies = snapshot.children.mapNotNull { it.getValue(ReplyDto::class.java)?.toDomain() }
                trySend(replies)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun addComment(postId: String, comment: Comment, localImageUri: android.net.Uri?): Result<Unit> = runCatching {
        var imageUrl = comment.imageUrl
        
        // 1. Subir a Cloudinary si hay imagen local
        if (localImageUri != null) {
            val optimizedFile = com.farbalapps.rinde.util.ImageOptimizer.optimizeImage(context, localImageUri)
            optimizedFile?.let { file ->
                imageUrl = CloudinaryHelper.uploadImage(file.absolutePath, "Comentarios")
                file.delete()
            }
        }
        
        // 2. Guardar en RTDB
        val commentRef = rtdb.getReference("comments").child(postId).push()
        val commentId = commentRef.key ?: throw Exception("Error generando ID de comentario")
        
        val finalComment = comment.copy(id = commentId, imageUrl = imageUrl)
        commentRef.setValue(finalComment.toDto()).await()
        
        // 3. Incrementar contador en Firestore
        firestore.collection("posts").document(postId)
            .update("commentsCount", com.google.firebase.firestore.FieldValue.increment(1)).await()
    }

    override suspend fun addReply(commentId: String, reply: Reply, localImageUri: android.net.Uri?): Result<Unit> = runCatching {
        var imageUrl = reply.imageUrl
        
        // 1. Subir a Cloudinary si hay imagen local
        if (localImageUri != null) {
            val optimizedFile = com.farbalapps.rinde.util.ImageOptimizer.optimizeImage(context, localImageUri)
            optimizedFile?.let { file ->
                imageUrl = CloudinaryHelper.uploadImage(file.absolutePath, "Comentarios")
                file.delete()
            }
        }
        
        // 2. Guardar en RTDB
        val replyRef = rtdb.getReference("replies").child(commentId).push()
        val replyId = replyRef.key ?: throw Exception("Error generando ID de respuesta")
        
        val finalReply = reply.copy(id = replyId, imageUrl = imageUrl)
        replyRef.setValue(finalReply.toDto()).await()
        
        // 3. Incrementar contador de respuestas en el comentario (RTDB)
        val commentRef = rtdb.getReference("comments").child(reply.postId).child(commentId)
        commentRef.child("repliesCount").setValue(com.google.firebase.database.ServerValue.increment(1)).await()
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

}
