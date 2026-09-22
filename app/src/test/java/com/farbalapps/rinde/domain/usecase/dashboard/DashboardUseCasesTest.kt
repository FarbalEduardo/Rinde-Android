package com.farbalapps.rinde.domain.usecase.dashboard

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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardUseCasesTest {

    private val dashboardRepository = mockk<DashboardRepository>(relaxed = true)
    private val listRepository = mockk<ListRepository>(relaxed = true)
    private val goalsRepository = mockk<GoalsRepository>(relaxed = true)
    private val savedListRepository = mockk<SavedListRepository>(relaxed = true)

    @Test
    fun `GetDashboardSummaryUseCase combines reactive flows and filters completed items and active goals`() = runTest {
        val profile = FinancialProfile("u1", 10000.0, IncomeFrequency.MONTHLY)
        val items = listOf(
            ShoppingItem(id = "1", name = "A", category = "General", price = 100.0, quantity = 1.0, isCompleted = true),
            ShoppingItem(id = "2", name = "B", category = "General", price = 50.0, quantity = 1.0, isCompleted = false)
        )
        val goals = listOf(
            SavingsGoal(id = "g1", title = "Meta 1", targetAmount = 500.0, currentAmount = 200.0, isArchived = false),
            SavingsGoal(id = "g2", title = "Meta 2", targetAmount = 1000.0, currentAmount = 500.0, isArchived = true)
        )
        val extraExpenses = listOf(ExtraExpense(id = "e1", label = "Luz", amount = 200.0, month = 9, year = 2026))
        val extraIncomes = listOf(ExtraIncome(id = "i1", label = "Bono", amount = 500.0, month = 9, year = 2026))

        every { dashboardRepository.getFinancialProfile() } returns flowOf(profile)
        every { listRepository.getItems() } returns flowOf(items)
        every { goalsRepository.getGoals() } returns flowOf(goals)
        every { dashboardRepository.getExtraExpenses(2026, 9) } returns flowOf(extraExpenses)
        every { dashboardRepository.getExtraIncomes(2026, 9) } returns flowOf(extraIncomes)
        every { dashboardRepository.getMonthlyVariableIncome(2026, 9) } returns flowOf(null)

        val useCase = GetDashboardSummaryUseCase(dashboardRepository, listRepository, goalsRepository)

        useCase(2026, 9).test {
            val summary = awaitItem()
            assertEquals("u1", summary.profile?.id)
            assertEquals(100.0, summary.listTotal, 0.001)
            assertEquals(1, summary.activeGoals.size)
            assertEquals("g1", summary.activeGoals.first().id)
            assertEquals(1, summary.extraExpenses.size)
            assertEquals(1, summary.extraIncomes.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ManageExtraExpenseUseCase addExpense validates inputs and delegates to repository`() = runTest {
        val useCase = ManageExtraExpenseUseCase(dashboardRepository)

        // Valid add
        val result = useCase.addExpense("Luz", 300.0, "electricity", 2026, 9)
        assertTrue(result.isSuccess)
        coVerify { dashboardRepository.addExtraExpense("Luz", 300.0, "electricity", 2026, 9, any()) }

        // Invalid amount
        val invalidAmountResult = useCase.addExpense("Agua", 0.0, "water", 2026, 9)
        assertTrue(invalidAmountResult.isFailure)

        // Invalid label
        val invalidLabelResult = useCase.addExpense("   ", 100.0, "water", 2026, 9)
        assertTrue(invalidLabelResult.isFailure)
    }

    @Test
    fun `ManageExtraExpenseUseCase updateExpense and deleteExpense delegate to repository`() = runTest {
        val useCase = ManageExtraExpenseUseCase(dashboardRepository)

        val updateResult = useCase.updateExpense("e1", "Gas", 450.0)
        assertTrue(updateResult.isSuccess)
        coVerify { dashboardRepository.updateExtraExpense("e1", "Gas", 450.0, any()) }

        val deleteResult = useCase.deleteExpense("e1")
        assertTrue(deleteResult.isSuccess)
        coVerify { dashboardRepository.deleteExtraExpense("e1") }
    }

    @Test
    fun `ManageExtraIncomeUseCase addIncome validates inputs and delegates to repository`() = runTest {
        val useCase = ManageExtraIncomeUseCase(dashboardRepository)

        // Valid add
        val result = useCase.addIncome("Freelance", 1200.0, "code", 2026, 9)
        assertTrue(result.isSuccess)
        coVerify { dashboardRepository.addExtraIncome("Freelance", 1200.0, "code", 2026, 9, any()) }

        // Invalid amount
        val invalidAmountResult = useCase.addIncome("Freelance", -10.0, "code", 2026, 9)
        assertTrue(invalidAmountResult.isFailure)

        // Invalid label
        val invalidLabelResult = useCase.addIncome("", 500.0, "code", 2026, 9)
        assertTrue(invalidLabelResult.isFailure)
    }

    @Test
    fun `ManageExtraIncomeUseCase updateIncome and deleteIncome delegate to repository`() = runTest {
        val useCase = ManageExtraIncomeUseCase(dashboardRepository)

        val updateResult = useCase.updateIncome("i1", "Venta", 600.0)
        assertTrue(updateResult.isSuccess)
        coVerify { dashboardRepository.updateExtraIncome("i1", "Venta", 600.0, any()) }

        val deleteResult = useCase.deleteIncome("i1")
        assertTrue(deleteResult.isSuccess)
        coVerify { dashboardRepository.deleteExtraIncome("i1") }
    }

    @Test
    fun `SyncDashboardDataUseCase syncs firebase, goals, saved lists, and performs rollover`() = runTest {
        val useCase = SyncDashboardDataUseCase(dashboardRepository, goalsRepository, savedListRepository)

        val result = useCase()
        assertTrue(result.isSuccess)

        coVerify { dashboardRepository.syncFromFirebase() }
        coVerify { dashboardRepository.checkAndPerformMonthlyRollover() }
        coVerify { goalsRepository.syncGoals() }
        coVerify { savedListRepository.syncSavedLists() }
    }
}
