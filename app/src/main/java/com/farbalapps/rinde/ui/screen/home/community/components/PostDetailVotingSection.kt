package com.farbalapps.rinde.ui.screen.home.community.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.farbalapps.rinde.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.ui.screen.home.community.VoteUiState
import com.farbalapps.rinde.ui.theme.RindeTheme

/**
 * Banner de advertencia cuando el usuario vota sin conexión o cuando ocurre un error al votar.
 */
fun LazyListScope.VoteStateBannerItem(
    voteState: VoteUiState,
    voteErrorMessage: String?
) {
    if (voteState == VoteUiState.OFFLINE || voteState == VoteUiState.ERROR) {
        item(key = "vote_state_banner") {
            val bannerColor = if (voteState == VoteUiState.OFFLINE)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.errorContainer

            val bannerContentColor = if (voteState == VoteUiState.OFFLINE)
                MaterialTheme.colorScheme.onTertiaryContainer
            else
                MaterialTheme.colorScheme.onErrorContainer

            val bannerIcon = if (voteState == VoteUiState.OFFLINE)
                Icons.Default.CloudOff
            else
                Icons.Default.ErrorOutline

            val bannerMessage = voteErrorMessage ?: if (voteState == VoteUiState.OFFLINE)
                "Sin conexión. Tu voto se enviará cuando vuelva internet."
            else
                "Error al registrar tu voto."

            Surface(
                color = bannerColor,
                contentColor = bannerContentColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(bannerIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(bannerMessage, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/**
 * Tarjeta interactiva de votación comunitaria ("¿Qué te parece esta oferta?").
 */
@Composable
fun VotingSectionCard(
    post: CommunityPost,
    voteState: VoteUiState,
    onVoteTrue: () -> Unit,
    onVoteFalse: () -> Unit,
    onJumpToComments: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "¿Qué te parece esta oferta?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Vota para ayudar a otros usuarios a saber si esta oferta es real o falsa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onJumpToComments != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = onJumpToComments,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.comment_btn_jump_to_comments, post.commentsCount),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            VotingActions(
                userVote = post.myVoteValue,
                truthCount = post.truthCount,
                falseCount = post.falseCount,
                onVoteTrue = { if (voteState != VoteUiState.SENDING) onVoteTrue() },
                onVoteFalse = { if (voteState != VoteUiState.SENDING) onVoteFalse() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

fun LazyListScope.VotingSectionItem(
    post: CommunityPost,
    voteState: VoteUiState,
    onVoteTrue: () -> Unit,
    onVoteFalse: () -> Unit,
    onJumpToComments: (() -> Unit)? = null
) {
    item(key = "voting_section") {
        VotingSectionCard(
            post = post,
            voteState = voteState,
            onVoteTrue = onVoteTrue,
            onVoteFalse = onVoteFalse,
            onJumpToComments = onJumpToComments
        )
    }
}

@PreviewLightDark
@Composable
private fun VotingSectionPreview() {
    RindeTheme {
        VotingSectionCard(
            post = CommunityPost(
                id = "1",
                title = "Oferta",
                truthCount = 10,
                falseCount = 2,
                myVoteValue = 1
            ),
            voteState = VoteUiState.IDLE,
            onVoteTrue = {},
            onVoteFalse = {}
        )
    }
}
