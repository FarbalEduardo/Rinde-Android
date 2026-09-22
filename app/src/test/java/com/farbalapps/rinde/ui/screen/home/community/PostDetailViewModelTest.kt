package com.farbalapps.rinde.ui.screen.home.community

import app.cash.turbine.test
import com.farbalapps.rinde.data.local.dao.PostDao
import com.farbalapps.rinde.domain.model.Comment
import com.farbalapps.rinde.domain.model.CommunityPost
import com.farbalapps.rinde.domain.model.OfferType
import com.farbalapps.rinde.domain.model.PostLocation
import com.farbalapps.rinde.domain.model.VerificationStatus
import com.farbalapps.rinde.domain.repository.AuthRepository
import com.farbalapps.rinde.domain.repository.FeedRepository
import com.farbalapps.rinde.domain.repository.VoteOverlay
import com.farbalapps.rinde.domain.usecase.AddCommentUseCase
import com.farbalapps.rinde.domain.usecase.AddReplyUseCase
import com.farbalapps.rinde.domain.usecase.DeleteCommentUseCase
import com.farbalapps.rinde.domain.usecase.DeleteReplyUseCase
import com.farbalapps.rinde.domain.usecase.EditCommentUseCase
import com.farbalapps.rinde.domain.usecase.EditReplyUseCase
import com.farbalapps.rinde.domain.usecase.GetCommentsUseCase
import com.farbalapps.rinde.domain.usecase.ReportCommentUseCase
import com.farbalapps.rinde.domain.usecase.ToggleCommentLikeUseCase
import com.farbalapps.rinde.domain.usecase.ToggleVoteUseCase
import com.farbalapps.rinde.domain.usecase.VoteResult
import com.farbalapps.rinde.util.logger.AppLogger
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val feedRepository: FeedRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val toggleVoteUseCase: ToggleVoteUseCase = mockk(relaxed = true)
    private val getCommentsUseCase: GetCommentsUseCase = mockk(relaxed = true)
    private val addCommentUseCase: AddCommentUseCase = mockk(relaxed = true)
    private val addReplyUseCase: AddReplyUseCase = mockk(relaxed = true)
    private val toggleLikeUseCase: ToggleCommentLikeUseCase = mockk(relaxed = true)
    private val deleteCommentUseCase: DeleteCommentUseCase = mockk(relaxed = true)
    private val editCommentUseCase: EditCommentUseCase = mockk(relaxed = true)
    private val deleteReplyUseCase: DeleteReplyUseCase = mockk(relaxed = true)
    private val editReplyUseCase: EditReplyUseCase = mockk(relaxed = true)
    private val reportCommentUseCase: ReportCommentUseCase = mockk(relaxed = true)
    private val markPostExpiredUseCase: com.farbalapps.rinde.domain.usecase.community.MarkPostExpiredUseCase = mockk(relaxed = true)
    private val deletePostUseCase: com.farbalapps.rinde.domain.usecase.community.DeletePostUseCase = mockk(relaxed = true)
    private val reportPostExpiredUseCase: com.farbalapps.rinde.domain.usecase.community.ReportPostExpiredUseCase = mockk(relaxed = true)
    private val logger: AppLogger = mockk(relaxed = true)
    private val postDao = mockk<PostDao>(relaxed = true)

    private lateinit var viewModel: PostDetailViewModel

    private val testPostId = "post_123"
    private val testPost = CommunityPost(
        id = testPostId,
        authorId = "author_1",
        authorName = "Test Author",
        authorPhotoUrl = null,
        timestamp = 0L,
        title = "Test Post",
        descriptionShort = "Short",
        descriptionLong = "Long",
        photos = emptyList(),
        category = "Otros",
        location = PostLocation("Loc", null, null),
        isActive = true,
        likesCount = 0,
        commentsCount = 0,
        truthCount = 2,
        falseCount = 1,
        votesScore = 1,
        verificationStatus = VerificationStatus.PENDING,
        reportCount = 0,
        userReputationScore = 0f,
        isAuthorVerified = false,
        offerType = OfferType.PHYSICAL,
        websiteName = null,
        productLink = null,
        storeName = null,
        isRecommended = false,
        expiresAt = null,
        normalPrice = 10.0,
        discountPrice = 8.0,
        currency = "MXN",
        couponCode = null,
        discountPercentage = 20,
        isAvailable = true,
        condition = "Nuevo",
        myVoteValue = 0
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        every { authRepository.getCurrentUser() } returns mockk {
            every { id } returns "current_user_1"
            every { displayName } returns "Current User"
        }

        every { feedRepository.globalPostStatus } returns MutableStateFlow(emptyMap())
        every { feedRepository.globalSavedStatus } returns MutableStateFlow(emptyMap())
        every { feedRepository.globalVoteStatus } returns MutableStateFlow(emptyMap())

        coEvery { feedRepository.getPostById(testPostId) } returns flowOf(testPost)

        viewModel = PostDetailViewModel(
            feedRepository,
            authRepository,
            toggleVoteUseCase,
            getCommentsUseCase,
            addCommentUseCase,
            addReplyUseCase,
            toggleLikeUseCase,
            deleteCommentUseCase,
            editCommentUseCase,
            deleteReplyUseCase,
            editReplyUseCase,
            reportCommentUseCase,
            markPostExpiredUseCase,
            deletePostUseCase,
            reportPostExpiredUseCase,
            logger
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleVote should trigger optimistic updates and succeed with server response`() = runTest {
        coEvery { toggleVoteUseCase(testPostId, 1, "author_1") } returns VoteResult.Success(Triple(5, 1, 4))
        
        viewModel.loadPost(testPostId)
        testScheduler.runCurrent()

        viewModel.toggleVote(1)

        assertEquals(VoteUiState.SENDING, viewModel.uiState.value.voteState)

        testScheduler.runCurrent()

        val confirmedState = viewModel.uiState.value
        // Verifica que finalmente se actualizo con los datos reales del servidor
        assertEquals(1, confirmedState.post?.myVoteValue)
        assertEquals(5, confirmedState.post?.truthCount)
        assertEquals(1, confirmedState.post?.falseCount)
        assertEquals(VoteUiState.IDLE, confirmedState.voteState)

        coVerify { toggleVoteUseCase(testPostId, 1, "author_1") }
    }

    @Test
    fun `toggleVote should rollback when server returns error`() = runTest {
        coEvery { toggleVoteUseCase(testPostId, 1, "author_1") } returns VoteResult.ServerError("Fallo del servidor")

        viewModel.loadPost(testPostId)
        testScheduler.runCurrent()

        viewModel.toggleVote(1)

        viewModel.uiState.test {
            testScheduler.runCurrent()
            val state = expectMostRecentItem()
            // Debe haber vuelto a los valores originales (rollback)
            assertEquals(0, state.post?.myVoteValue)
            assertEquals(2, state.post?.truthCount)
            assertEquals(VoteUiState.ERROR, state.voteState)
            assertEquals("Fallo del servidor", state.voteErrorMessage)
        }
    }

    @Test
    fun `onCommentTextChange updates state and submitComment sends comment successfully`() = runTest {
        val mockComment = Comment(
            id = "c_1",
            postId = testPostId,
            authorId = "current_user_1",
            authorName = "Current User",
            text = "Gran oferta!",
            timestamp = 1000L
        )
        coEvery { addCommentUseCase(postId = testPostId, text = "Gran oferta!", imageUri = null) } returns Result.success(mockComment)

        viewModel.loadPost(testPostId)
        testScheduler.runCurrent()

        viewModel.onCommentTextChange("Gran oferta!")
        assertEquals("Gran oferta!", viewModel.uiState.value.commentText)

        viewModel.submitComment()
        testScheduler.runCurrent()

        assertEquals("", viewModel.uiState.value.commentText)
        assertEquals(false, viewModel.uiState.value.isSendingComment)
        coVerify { addCommentUseCase(postId = testPostId, text = "Gran oferta!", imageUri = null) }
    }

    @Test
    fun `setReplyingTo sets target comment and onReplyTextChange updates state`() = runTest {
        val targetComment = Comment(
            id = "c_1",
            postId = testPostId,
            authorId = "user_2",
            authorName = "Ana",
            text = "Comentario inicial",
            timestamp = 1000L
        )

        viewModel.setReplyingTo(targetComment)
        assertEquals(targetComment, viewModel.uiState.value.replyingToComment)
        assertEquals("", viewModel.uiState.value.replyText)

        viewModel.onReplyTextChange("Totalmente de acuerdo")
        assertEquals("Totalmente de acuerdo", viewModel.uiState.value.replyText)

        viewModel.setReplyingTo(null)
        assertEquals(null, viewModel.uiState.value.replyingToComment)
    }
}

