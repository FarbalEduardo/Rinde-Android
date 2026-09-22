package com.farbalapps.rinde.ui.screen.home.dashboard

import app.cash.turbine.test
import com.farbalapps.rinde.domain.model.ExtraExpense
import com.farbalapps.rinde.domain.model.ExtraIncome
import com.farbalapps.rinde.domain.model.FinancialProfile
import com.farbalapps.rinde.domain.model.IncomeFrequency
import com.farbalapps.rinde.domain.model.SavingsGoal
import com.farbalapps.rinde.domain.model.ShoppingItem
import com.farbalapps.rinde.domain.repository.DashboardRepository
import com.farbalapps.rinde.domain.repository.GoalsRepository
import com.farbalapps.rinde.domain.repository.ListRepository
import com.farbalapps.rinde.domain.repository.SavedListRepository
import com.farbalapps.rinde.domain.usecase.dashboard.GetDashboardSummaryUseCase
import com.farbalapps.rinde.domain.usecase.dashboard.ManageExtraExpenseUseCase
import com.farbalapps.rinde.domain.usecase.dashboard.ManageExtraIncomeUseCase
import com.farbalapps.rinde.domain.usecase.dashboard.SyncDashboardDataUseCase
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertNull
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
    private val monthlyVariableIncomeFlow = MutableStateFlow<Double?>(null)
    private val extraIncomesFlow = MutableStateFlow<List<ExtraIncome>>(emptyList())

    private lateinit var getDashboardSummaryUseCase: GetDashboardSummaryUseCase
    private lateinit var manageExtraExpenseUseCase: ManageExtraExpenseUseCase
    private lateinit var manageExtraIncomeUseCase: ManageExtraIncomeUseCase
    private lateinit var syncDashboardDataUseCase: SyncDashboardDataUseCase
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { dashboardRepository.getFinancialProfile() } returns profileFlow
        every { listRepository.getItems() } returns itemsFlow
        every { goalsRepository.getGoals() } returns goalsFlow
        every { dashboardRepository.getExtraExpenses(any(), any()) } returns extraExpensesFlow
        every { dashboardRepository.getMonthlyVariableIncome(any(), any()) } returns monthlyVariableIncomeFlow
        every { dashboardRepository.getExtraIncomes(any(), any()) } returns extraIncomesFlow

        getDashboardSummaryUseCase = GetDashboardSummaryUseCase(dashboardRepository, listRepository, goalsRepository)
        manageExtraExpenseUseCase = ManageExtraExpenseUseCase(dashboardRepository)
        manageExtraIncomeUseCase = ManageExtraIncomeUseCase(dashboardRepository)
        syncDashboardDataUseCase = SyncDashboardDataUseCase(dashboardRepository, goalsRepository, savedListRepository)

        viewModel = DashboardViewModel(
            getDashboardSummaryUseCase = getDashboardSummaryUseCase,
            manageExtraExpenseUseCase = manageExtraExpenseUseCase,
            manageExtraIncomeUseCase = manageExtraIncomeUseCase,
            syncDashboardDataUseCase = syncDashboardDataUseCase,
            dashboardRepository = dashboardRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialState when profile is null shows no income configured and SETUP_REQUIRED health status`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.hasIncomeConfigured)
            assertEquals(0.0, state.monthlyIncome, 0.001)
            assertEquals(0.0, state.availableAmount, 0.001)
            assertEquals(FinancialHealthStatus.SETUP_REQUIRED, state.healthStatus)
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
        coEvery { dashboardRepository.addExtraExpense(any(), any(), any(), any(), any(), any()) } returns Unit

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.openExpenseSheet()
            testScheduler.runCurrent()
            val openState = awaitItem()
            assertTrue(openState.isExpenseSheetOpen)

            viewModel.addExtraExpense("Agua", 350.0, "water")
            testScheduler.advanceUntilIdle()

            coVerify {
                dashboardRepository.addExtraExpense("Agua", 350.0, "water", viewModel.currentYear, viewModel.currentMonth, any())
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
    fun `refreshDashboard sets isRefreshing true, executes sync, and resets isRefreshing to false`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.refreshDashboard()
            testScheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertFalse(state.isRefreshing)
            coVerify(atLeast = 2) { dashboardRepository.syncFromFirebase() }
            coVerify(atLeast = 2) { dashboardRepository.checkAndPerformMonthlyRollover() }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `openFinancialCalendar and closeFinancialCalendar toggle modal visibility state`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial

            assertFalse(viewModel.uiState.value.isFinancialCalendarOpen)

            viewModel.openFinancialCalendar()
            testScheduler.runCurrent()
            assertTrue(awaitItem().isFinancialCalendarOpen)

            viewModel.closeFinancialCalendar()
            testScheduler.runCurrent()
            assertFalse(awaitItem().isFinancialCalendarOpen)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pending delete states for extra expense can be requested and cancelled`() = runTest {
        val expense = ExtraExpense(id = "e1", label = "Gas", amount = 400.0, month = 9, year = 2026)

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.requestDeleteExpense(expense)
            testScheduler.runCurrent()
            val stateWithPending = awaitItem()
            assertEquals("e1", stateWithPending.pendingDeleteExpense?.id)

            viewModel.cancelDeleteExpense()
            testScheduler.runCurrent()
            val stateAfterCancel = awaitItem()
            assertNull(stateAfterCancel.pendingDeleteExpense)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pending delete states for extra income can be requested and cancelled`() = runTest {
        val income = ExtraIncome(id = "i1", label = "Venta", amount = 800.0, month = 9, year = 2026)

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.requestDeleteIncome(income)
            testScheduler.runCurrent()
            val stateWithPending = awaitItem()
            assertEquals("i1", stateWithPending.pendingDeleteIncome?.id)

            viewModel.cancelDeleteIncome()
            testScheduler.runCurrent()
            val stateAfterCancel = awaitItem()
            assertNull(stateAfterCancel.pendingDeleteIncome)

            cancelAndIgnoreRemainingEvents()
        }
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

    @Test
    fun `fixed income plus extra incomes correctly sums monthlyIncome and availableAmount`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial

            profileFlow.value = FinancialProfile(
                id = "u1",
                income = 10000.0,
                incomeFrequency = IncomeFrequency.MONTHLY,
                isVariableIncome = false
            )

            extraIncomesFlow.value = listOf(
                ExtraIncome(id = "ei1", label = "Freelance", amount = 3000.0, month = viewModel.currentMonth, year = viewModel.currentYear),
                ExtraIncome(id = "ei2", label = "Venta garage", amount = 500.0, month = viewModel.currentMonth, year = viewModel.currentYear)
            )

            testScheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(10000.0, state.baseMonthlyIncome, 0.001)
            assertEquals(3500.0, state.extraIncomesTotal, 0.001)
            assertEquals(13500.0, state.monthlyIncome, 0.001)
            assertEquals(13500.0, state.availableAmount, 0.001)
            assertFalse(state.needsMonthlyIncomeCapture)
            assertEquals(2, state.extraIncomes.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `variable income in new month without captured amount starts at zero and prompts for capture`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial

            profileFlow.value = FinancialProfile(
                id = "u1",
                income = 0.0,
                incomeFrequency = IncomeFrequency.MONTHLY,
                isVariableIncome = true
            )
            monthlyVariableIncomeFlow.value = null

            testScheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertTrue(state.isVariableIncome)
            assertEquals(0.0, state.baseMonthlyIncome, 0.001)
            assertEquals(0.0, state.monthlyIncome, 0.001)
            assertTrue(state.needsMonthlyIncomeCapture)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `variable income with captured amount calculates availableAmount and clears prompt`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial

            profileFlow.value = FinancialProfile(
                id = "u1",
                income = 0.0,
                incomeFrequency = IncomeFrequency.MONTHLY,
                isVariableIncome = true
            )
            monthlyVariableIncomeFlow.value = 12000.0
            extraIncomesFlow.value = listOf(
                ExtraIncome(id = "ei1", label = "Bono", amount = 2000.0, month = viewModel.currentMonth, year = viewModel.currentYear)
            )

            testScheduler.advanceUntilIdle()

            val state = expectMostRecentItem()
            assertTrue(state.isVariableIncome)
            assertEquals(12000.0, state.baseMonthlyIncome, 0.001)
            assertEquals(2000.0, state.extraIncomesTotal, 0.001)
            assertEquals(14000.0, state.monthlyIncome, 0.001)
            assertFalse(state.needsMonthlyIncomeCapture)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveIncome with isVariable true delegates to saveFinancialProfile and saveMonthlyVariableIncome`() = runTest {
        coEvery { dashboardRepository.saveFinancialProfile(any(), any(), any(), any(), any(), any()) } returns Unit
        coEvery { dashboardRepository.saveMonthlyVariableIncome(any(), any(), any(), any()) } returns Unit

        viewModel.saveIncome(
            income = 16000.0,
            frequency = IncomeFrequency.MONTHLY,
            currency = "MXN",
            isVariable = true
        )
        testScheduler.advanceUntilIdle()

        coVerify {
            dashboardRepository.saveFinancialProfile(
                income = 16000.0,
                frequency = IncomeFrequency.MONTHLY,
                currency = "MXN",
                customStartDate = null,
                customEndDate = null,
                isVariableIncome = true
            )
            dashboardRepository.saveMonthlyVariableIncome(
                year = viewModel.currentYear,
                month = viewModel.currentMonth,
                amount = 16000.0,
                paymentDate = any()
            )
        }
    }

    @Test
    fun `extra income CRUD operations delegate to repository and manage sheets`() = runTest {
        coEvery { dashboardRepository.addExtraIncome(any(), any(), any(), any(), any(), any()) } returns Unit
        coEvery { dashboardRepository.updateExtraIncome(any(), any(), any(), any()) } returns Unit
        coEvery { dashboardRepository.deleteExtraIncome(any()) } returns Unit

        viewModel.uiState.test {
            awaitItem() // initial

            // Open Extra Income sheet
            viewModel.openExtraIncomeSheet()
            testScheduler.runCurrent()
            assertTrue(awaitItem().isExtraIncomeSheetOpen)

            // Add Extra Income
            viewModel.addExtraIncome("Venta pastel", 450.0, "cash")
            testScheduler.advanceUntilIdle()
            coVerify {
                dashboardRepository.addExtraIncome("Venta pastel", 450.0, "cash", viewModel.currentYear, viewModel.currentMonth, any())
            }
            assertFalse(awaitItem().isExtraIncomeSheetOpen)

            // Open Edit Extra Income sheet
            val incomeToEdit = ExtraIncome(id = "ei9", label = "Pastel", amount = 450.0, month = viewModel.currentMonth, year = viewModel.currentYear)
            viewModel.openEditExtraIncomeSheet(incomeToEdit)
            testScheduler.runCurrent()
            val editingState = awaitItem()
            assertTrue(editingState.isEditExtraIncomeSheetOpen)
            assertEquals("ei9", editingState.editingExtraIncome?.id)

            // Update Extra Income
            viewModel.updateExtraIncome("ei9", "Pastel grande", 600.0)
            testScheduler.advanceUntilIdle()
            coVerify { dashboardRepository.updateExtraIncome("ei9", "Pastel grande", 600.0, any()) }
            assertFalse(awaitItem().isEditExtraIncomeSheetOpen)

            // Delete Extra Income
            viewModel.openEditExtraIncomeSheet(incomeToEdit)
            testScheduler.runCurrent()
            awaitItem()
            viewModel.deleteExtraIncome("ei9")
            testScheduler.advanceUntilIdle()
            coVerify { dashboardRepository.deleteExtraIncome("ei9") }
            assertFalse(awaitItem().isEditExtraIncomeSheetOpen)

            cancelAndIgnoreRemainingEvents()
        }
    }
}

