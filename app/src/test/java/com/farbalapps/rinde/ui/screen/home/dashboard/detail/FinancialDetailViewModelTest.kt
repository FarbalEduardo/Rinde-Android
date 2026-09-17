package com.farbalapps.rinde.ui.screen.home.dashboard.detail

import app.cash.turbine.test
import com.farbalapps.rinde.domain.model.*
import com.farbalapps.rinde.domain.repository.DashboardRepository
import com.farbalapps.rinde.domain.repository.GoalsRepository
import com.farbalapps.rinde.domain.repository.ListRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FinancialDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val dashboardRepository = mockk<DashboardRepository>(relaxed = true)
    private val listRepository = mockk<ListRepository>(relaxed = true)
    private val goalsRepository = mockk<GoalsRepository>(relaxed = true)

    private val profileFlow = MutableStateFlow<FinancialProfile?>(null)
    private val itemsFlow = MutableStateFlow<List<ShoppingItem>>(emptyList())
    private val goalsFlow = MutableStateFlow<List<SavingsGoal>>(emptyList())
    private val extraExpensesFlow = MutableStateFlow<List<ExtraExpense>>(emptyList())

    private lateinit var viewModel: FinancialDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { dashboardRepository.getFinancialProfile() } returns profileFlow
        every { listRepository.getItems() } returns itemsFlow
        every { goalsRepository.getGoals() } returns goalsFlow
        every { dashboardRepository.getExtraExpensesBetween(any(), any()) } returns extraExpensesFlow

        viewModel = FinancialDetailViewModel(dashboardRepository, listRepository, goalsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPeriodTypeChangeUpdatesState() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.setPeriodType(PeriodType.FORTNIGHT)
            testScheduler.advanceUntilIdle()

            val item = expectMostRecentItem()
            assertEquals(PeriodType.FORTNIGHT, item.periodType)
            assertTrue(item.selectedPeriod is FinancialPeriod.Fortnight)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testNavigationPreviousAndNext() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem() // Initial state is current Month
            val initialMonth = (initial.selectedPeriod as FinancialPeriod.Month).month

            viewModel.navigateNext()
            testScheduler.advanceUntilIdle()

            val nextItem = awaitItem()
            val nextMonth = (nextItem.selectedPeriod as FinancialPeriod.Month).month
            val expected = if (initialMonth == 12) 1 else initialMonth + 1
            assertEquals(expected, nextMonth)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testFinancialCalculationForFortnight() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial

            // Ingreso mensual $30,000 -> Quincena = $15,000
            profileFlow.value = FinancialProfile(
                income = 30000.0,
                incomeFrequency = IncomeFrequency.MONTHLY
            )
            // Extra expense de $1,000
            extraExpensesFlow.value = listOf(
                ExtraExpense(
                    id = "1",
                    label = "Luz",
                    amount = 1000.0,
                    month = 9,
                    year = 2026,
                    createdAt = System.currentTimeMillis()
                )
            )

            viewModel.setPeriodType(PeriodType.FORTNIGHT)
            testScheduler.advanceUntilIdle()

            val item = expectMostRecentItem()
            assertEquals(15000.0, item.periodIncome, 0.01)
            assertEquals(1000.0, item.extraExpensesTotal, 0.01)
            // Available = 15000 - 1000 = 14000
            assertEquals(14000.0, item.availableAmount, 0.01)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
