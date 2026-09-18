package com.farbalapps.rinde.ui.screen.home.dashboard

import app.cash.turbine.test
import com.farbalapps.rinde.domain.model.ExtraExpense
import com.farbalapps.rinde.domain.model.FinancialProfile
import com.farbalapps.rinde.domain.model.IncomeFrequency
import com.farbalapps.rinde.domain.model.SavingsGoal
import com.farbalapps.rinde.domain.model.ShoppingItem
import com.farbalapps.rinde.domain.repository.DashboardRepository
import com.farbalapps.rinde.domain.repository.GoalsRepository
import com.farbalapps.rinde.domain.repository.ListRepository
import com.farbalapps.rinde.domain.repository.SavedListRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val dashboardRepository = mockk<DashboardRepository>(relaxed = true)
    private val listRepository = mockk<ListRepository>(relaxed = true)
    private val goalsRepository = mockk<GoalsRepository>(relaxed = true)
    private val savedListRepository = mockk<SavedListRepository>(relaxed = true)

    private val profileFlow = MutableStateFlow<FinancialProfile?>(null)
    private val itemsFlow = MutableStateFlow<List<ShoppingItem>>(emptyList())
    private val goalsFlow = MutableStateFlow<List<SavingsGoal>>(emptyList())
    private val extraExpensesFlow = MutableStateFlow<List<ExtraExpense>>(emptyList())

    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { dashboardRepository.getFinancialProfile() } returns profileFlow
        every { listRepository.getItems() } returns itemsFlow
        every { goalsRepository.getGoals() } returns goalsFlow
        every { dashboardRepository.getExtraExpenses(any(), any()) } returns extraExpensesFlow

        viewModel = DashboardViewModel(dashboardRepository, listRepository, goalsRepository, savedListRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialState when profile is null shows no income configured`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.hasIncomeConfigured)
            assertEquals(0.0, state.monthlyIncome, 0.001)
            assertEquals(0.0, state.availableAmount, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `biweekly income normalizes to monthly correctly and calculates available amount`() = runTest {
        viewModel.uiState.test {
            // Initial state
            awaitItem()

            // Update profile with $9,000 quincenal (biweekly) -> $18,000 monthly
            val testProfile = FinancialProfile(
                id = "user1",
                income = 9000.0,
                incomeFrequency = IncomeFrequency.BIWEEKLY,
                currency = "MXN"
            )
            profileFlow.value = testProfile

            // Update shopping list items ($4,200 total)
            itemsFlow.value = listOf(
                ShoppingItem(id = "1", name = "Super", category = "Comida", price = 4200.0, quantity = 1.0, isCompleted = true)
            )

            // Update extra expenses ($1,500 total)
            extraExpensesFlow.value = listOf(
                ExtraExpense(id = "e1", label = "Luz", amount = 1500.0, month = viewModel.currentMonth, year = viewModel.currentYear)
            )

            testScheduler.advanceUntilIdle()

            val updatedState = expectMostRecentItem()
            assertTrue(updatedState.hasIncomeConfigured)
            assertEquals(18000.0, updatedState.monthlyIncome, 0.001)
            assertEquals(4200.0, updatedState.listTotal, 0.001)
            assertEquals(1500.0, updatedState.extraExpensesTotal, 0.001)
            // Available = 18000 - 4200 - 1500 = 12300
            assertEquals(12300.0, updatedState.availableAmount, 0.001)
            assertEquals(FinancialHealthStatus.GOOD, updatedState.healthStatus)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveIncome delegates to repository and closes sheet`() = runTest {
        coEvery { dashboardRepository.saveFinancialProfile(any(), any(), any()) } returns Unit

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.openIncomeSheet()
            testScheduler.runCurrent()
            val openState = awaitItem()
            assertTrue(openState.isIncomeSheetOpen)

            viewModel.saveIncome(15000.0, IncomeFrequency.MONTHLY, "MXN")
            testScheduler.advanceUntilIdle()

            coVerify { dashboardRepository.saveFinancialProfile(15000.0, IncomeFrequency.MONTHLY, "MXN") }
            val closedState = awaitItem()
            assertFalse(closedState.isIncomeSheetOpen)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `addExtraExpense delegates to repository and closes sheet`() = runTest {
        coEvery { dashboardRepository.addExtraExpense(any(), any(), any(), any(), any()) } returns Unit

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.openExpenseSheet()
            testScheduler.runCurrent()
            val openState = awaitItem()
            assertTrue(openState.isExpenseSheetOpen)

            viewModel.addExtraExpense("Agua", 350.0, "water")
            testScheduler.advanceUntilIdle()

            coVerify {
                dashboardRepository.addExtraExpense("Agua", 350.0, "water", viewModel.currentYear, viewModel.currentMonth)
            }
            val closedState = awaitItem()
            assertFalse(closedState.isExpenseSheetOpen)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init triggers sync of dashboard, goals and saved lists`() = runTest {
        testScheduler.advanceUntilIdle()
        coVerify { dashboardRepository.syncFromFirebase() }
        coVerify { dashboardRepository.checkAndPerformMonthlyRollover() }
        coVerify { goalsRepository.syncGoals() }
        coVerify { savedListRepository.syncSavedLists() }
    }

    @Test
    fun `activeGoals excludes archived goals and includes reactivated or completed goals`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial

            val goalActive = SavingsGoal(id = "g1", title = "Vacaciones", targetAmount = 5000.0, currentAmount = 1000.0, isArchived = false, isCompleted = false)
            val goalArchived = SavingsGoal(id = "g2", title = "Coche", targetAmount = 10000.0, currentAmount = 10000.0, isArchived = true, isCompleted = true)
            val goalReactivatedCompleted = SavingsGoal(id = "g3", title = "Fondo de emergencia", targetAmount = 20000.0, currentAmount = 20000.0, isArchived = false, isCompleted = true)

            goalsFlow.value = listOf(goalActive, goalArchived, goalReactivatedCompleted)
            testScheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(2, state.activeGoals.size)
            assertTrue(state.activeGoals.any { it.id == "g1" })
            assertFalse(state.activeGoals.any { it.id == "g2" })
            assertTrue(state.activeGoals.any { it.id == "g3" })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reactive goal archiving and unarchiving updates dashboard activeGoals dynamically`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial

            val goal1 = SavingsGoal(id = "g1", title = "Laptop", targetAmount = 15000.0, currentAmount = 5000.0, isArchived = false)
            goalsFlow.value = listOf(goal1)
            testScheduler.advanceUntilIdle()
            assertEquals(1, expectMostRecentItem().activeGoals.size)

            // Archivar la meta: debe desaparecer del Dashboard
            goalsFlow.value = listOf(goal1.copy(isArchived = true))
            testScheduler.advanceUntilIdle()
            assertEquals(0, expectMostRecentItem().activeGoals.size)

            // Reactivar la meta: debe volver a aparecer en el Dashboard
            goalsFlow.value = listOf(goal1.copy(isArchived = false))
            testScheduler.advanceUntilIdle()
            val reactivatedState = expectMostRecentItem()
            assertEquals(1, reactivatedState.activeGoals.size)
            assertEquals("g1", reactivatedState.activeGoals.first().id)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
