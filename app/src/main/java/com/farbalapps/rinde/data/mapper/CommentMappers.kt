package com.farbalapps.rinde.data.mapper

import com.farbalapps.rinde.data.remote.model.CommentDto
import com.farbalapps.rinde.data.remote.model.ReplyDto
import com.farbalapps.rinde.domain.model.Comment
import com.farbalapps.rinde.domain.model.Reply

fun CommentDto.toDomain(): Comment {
    return Comment(
        id = id,
        postId = postId,
        authorId = authorId,
        authorName = authorName,
        authorPhotoUrl = authorPhotoUrl,
        text = text,
        imageUrl = imageUrl,
        timestamp = timestamp,
        likesCount = likesCount,
        repliesCount = repliesCount
    )
}

fun Comment.toDto(): CommentDto {
    return CommentDto(
        id = id,
        postId = postId,
        authorId = authorId,
        authorName = authorName,
        authorPhotoUrl = authorPhotoUrl,
        text = text,
        imageUrl = imageUrl,
        timestamp = timestamp,
        likesCount = likesCount,
        repliesCount = repliesCount
    )
}

fun ReplyDto.toDomain(): Reply {
    return Reply(
        id = id,
        commentId = commentId,
        postId = postId,
        authorId = authorId,
        authorName = authorName,
        authorPhotoUrl = authorPhotoUrl,
        text = text,
        imageUrl = imageUrl,
        mentionedUser = mentionedUser,
        timestamp = timestamp,
        likesCount = likesCount
    )
}

fun Reply.toDto(): ReplyDto {
    return ReplyDto(
        id = id,
        commentId = commentId,
        postId = postId,
        authorId = authorId,
        authorName = authorName,
        authorPhotoUrl = authorPhotoUrl,
        text = text,
        imageUrl = imageUrl,
        mentionedUser = mentionedUser,
        timestamp = timestamp,
        likesCount = likesCount
    )
}
