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
import org.junit.Assert.assertFalse
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
        every { dashboardRepository.getExtraExpenses(any(), any()) } returns extraExpensesFlow
        every { dashboardRepository.getMonthlyRecord(any(), any()) } returns MutableStateFlow(null)

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

            // 1. Intentar avanzar desde el mes actual debe bloquearse (no permite meses futuros)
            viewModel.navigateNext()
            testScheduler.advanceUntilIdle()
            assertEquals(initialMonth, (viewModel.uiState.value.selectedPeriod as FinancialPeriod.Month).month)

            // 2. Navegar hacia atrás sí debe permitir ver meses pasados
            viewModel.navigatePrevious()
            testScheduler.advanceUntilIdle()
            val prevItem = expectMostRecentItem()
            val prevMonth = (prevItem.selectedPeriod as FinancialPeriod.Month).month
            val expectedPrev = if (initialMonth == 1) 12 else initialMonth - 1
            assertEquals(expectedPrev, prevMonth)

            // 3. Desde un mes pasado, avanzar hacia adelante regresa al mes actual
            viewModel.navigateNext()
            testScheduler.advanceUntilIdle()
            val backItem = expectMostRecentItem()
            val backMonth = (backItem.selectedPeriod as FinancialPeriod.Month).month
            assertEquals(initialMonth, backMonth)

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

    @Test
    fun testOnlyBoughtShoppingItemsAreCountedAndDisplayed() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial

            itemsFlow.value = listOf(
                ShoppingItem(id = "1", name = "Manzanas", category = "Fruta", price = 50.0, quantity = 2.0, isCompleted = true),
                ShoppingItem(id = "2", name = "Carne", category = "Carnicería", price = 200.0, quantity = 1.0, isCompleted = false) // Not bought
            )
            testScheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            // Only Manzanas (50 * 2 = 100) should count
            assertEquals(1, state.shoppingItems.size)
            assertEquals("Manzanas", state.shoppingItems.first().name)
            assertEquals(100.0, state.listTotal, 0.01)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testPastMonthDisplaysArchivedMonthlyRecord() = runTest {
        val historicalRecord = MonthlyFinancialRecord(
            year = 2026,
            month = 1,
            income = 25000.0,
            listTotal = 3500.0,
            extraExpensesTotal = 2000.0,
            goalsCommittedTotal = 4000.0,
            availableAmount = 15500.0
        )
        every { dashboardRepository.getMonthlyRecord(2026, 1) } returns MutableStateFlow(historicalRecord)

        viewModel.uiState.test {
            awaitItem() // initial

            // Establecer el periodo a un mes pasado conocido
            viewModel.onDateSelected(java.util.GregorianCalendar(2026, java.util.Calendar.JANUARY, 15).timeInMillis)
            testScheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(25000.0, state.periodIncome, 0.01)
            assertEquals(3500.0, state.listTotal, 0.01)
            assertEquals(4000.0, state.goalsCommittedTotal, 0.01)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun testActiveGoalsExcludesArchivedAndIncludesReactivatedOrCompletedGoals() = runTest {
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
}
