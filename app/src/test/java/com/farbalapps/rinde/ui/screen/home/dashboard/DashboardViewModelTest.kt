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

        viewModel = DashboardViewModel(dashboardRepository, listRepository, goalsRepository)
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
                ShoppingItem(id = "1", name = "Super", category = "Comida", price = 4200.0, quantity = 1.0)
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
}
