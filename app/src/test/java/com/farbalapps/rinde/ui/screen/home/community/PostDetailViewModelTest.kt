package com.farbalapps.rinde.ui.screen.home.community

import app.cash.turbine.test
import com.farbalapps.rinde.data.local.dao.PostDao
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

    private val feedRepository = mockk<FeedRepository>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val toggleVoteUseCase = mockk<ToggleVoteUseCase>()
    private val getCommentsUseCase = mockk<GetCommentsUseCase>(relaxed = true)
    private val addCommentUseCase = mockk<AddCommentUseCase>(relaxed = true)
    private val addReplyUseCase = mockk<AddReplyUseCase>(relaxed = true)
    private val toggleLikeUseCase = mockk<ToggleCommentLikeUseCase>(relaxed = true)
    private val deleteCommentUseCase = mockk<DeleteCommentUseCase>(relaxed = true)
    private val editCommentUseCase = mockk<EditCommentUseCase>(relaxed = true)
    private val deleteReplyUseCase = mockk<DeleteReplyUseCase>(relaxed = true)
    private val editReplyUseCase = mockk<EditReplyUseCase>(relaxed = true)
    private val reportCommentUseCase = mockk<ReportCommentUseCase>(relaxed = true)
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
            reportCommentUseCase
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

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            // Verifica que el voto optimista en el UI se aplico
            assertEquals(1, state.post?.myVoteValue)
            assertEquals(3, state.post?.truthCount) // 2 + 1 optimista

            testScheduler.runCurrent()

            val confirmedState = expectMostRecentItem()
            // Verifica que finalmente se actualizo con los datos reales del servidor
            assertEquals(5, confirmedState.post?.truthCount)
            assertEquals(1, confirmedState.post?.falseCount)
            assertEquals(VoteUiState.IDLE, confirmedState.voteState)
        }

        coVerify { postDao.updateVoteState(testPostId, 1, 5, 1, 4) }
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
}
