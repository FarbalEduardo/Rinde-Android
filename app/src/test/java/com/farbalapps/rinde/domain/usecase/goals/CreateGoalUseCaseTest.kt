package com.farbalapps.rinde.domain.usecase.goals

import com.farbalapps.rinde.domain.model.SavingsGoal
import com.farbalapps.rinde.domain.repository.GoalsRepository
import com.farbalapps.rinde.domain.usecase.goals.CreateGoalUseCase
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateGoalUseCaseTest {

    private val repository = mockk<GoalsRepository>()
    private lateinit var createGoalUseCase: CreateGoalUseCase

    @Before
    fun setUp() {
        createGoalUseCase = CreateGoalUseCase(repository)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun given_validGoal_when_invoke_then_insertsSuccessfully() = runBlocking {
        // Arrange
        coEvery { repository.getGoalsSnapshot() } returns emptyList()
        val goal = SavingsGoal("1", "user1", "Viaje a Japón", 5000.0, 0.0, "flight", "blue", false, 0, 0, 0.0)
        coEvery { repository.createGoal(goal) } just Runs

        // Act
        val result = createGoalUseCase(goal)

        // Assert
        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.createGoal(goal) }
    }

    @Test
    fun given_emptyTitle_when_invoke_then_failsWithValidationError() = runBlocking {
        // Arrange
        coEvery { repository.getGoalsSnapshot() } returns emptyList()
        val goal = SavingsGoal("1", "user1", "", 5000.0, 0.0, "flight", "blue", false, 0, 0, 0.0)

        // Act
        val result = createGoalUseCase(goal)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { repository.createGoal(any()) }
    }

    @Test
    fun given_zeroAmount_when_invoke_then_failsWithValidationError() = runBlocking {
        // Arrange
        coEvery { repository.getGoalsSnapshot() } returns emptyList()
        val goal = SavingsGoal("1", "user1", "Viaje a Japón", 0.0, 0.0, "flight", "blue", false, 0, 0, 0.0)

        // Act
        val result = createGoalUseCase(goal)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { repository.createGoal(any()) }
    }

    @Test
    fun given_3ExistingGoals_when_invoke_then_failsDueToFreeTierLimit() = runBlocking {
        // Arrange
        val existing = listOf(
            mockk<SavingsGoal>(),
            mockk<SavingsGoal>(),
            mockk<SavingsGoal>()
        )
        coEvery { repository.getGoalsSnapshot() } returns existing
        val newGoal = SavingsGoal("4", "user1", "Viaje a Japón", 5000.0, 0.0, "flight", "blue", false, 0, 0, 0.0)

        // Act
        val result = createGoalUseCase(newGoal)

        // Assert
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        coVerify(exactly = 0) { repository.createGoal(any()) }
    }
}
