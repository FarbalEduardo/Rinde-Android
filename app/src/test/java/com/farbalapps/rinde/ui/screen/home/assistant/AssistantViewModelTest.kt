package com.farbalapps.rinde.ui.screen.home.assistant

import app.cash.turbine.test
import com.farbalapps.rinde.domain.model.ChatMessage
import com.farbalapps.rinde.domain.model.RecipeCard
import com.farbalapps.rinde.domain.model.ShoppingItem
import com.farbalapps.rinde.domain.repository.AiRepository
import com.farbalapps.rinde.domain.repository.ChatRepository
import com.farbalapps.rinde.domain.repository.ListRepository
import com.farbalapps.rinde.domain.repository.SavedListRepository
import com.farbalapps.rinde.domain.usecase.FilterCookableItemsUseCase
import com.farbalapps.rinde.util.MainDispatcherRule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AssistantViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val listRepository: ListRepository = mockk(relaxed = true)
    private val savedListRepository: SavedListRepository = mockk(relaxed = true)
    private val filterCookableItemsUseCase: FilterCookableItemsUseCase = FilterCookableItemsUseCase()
    private val chatRepository: ChatRepository = mockk(relaxed = true)
    private val aiRepository: AiRepository = mockk()
    private val auth: FirebaseAuth = mockk(relaxed = true)
    private val firebaseUser: FirebaseUser = mockk(relaxed = true)

    private lateinit var viewModel: AssistantViewModel

    @Before
    fun setUp() {
        every { auth.currentUser } returns firebaseUser
        every { firebaseUser.uid } returns "user_123"
        every { listRepository.getItems() } returns flowOf(
            listOf(
                ShoppingItem(id = "1", name = "Pollo", category = "Carnes", isCompleted = true),
                ShoppingItem(id = "2", name = "Arroz", category = "Granos", isCompleted = true),
                ShoppingItem(id = "3", name = "Detergente", category = "Limpieza", isCompleted = true) // non-cookable
            )
        )
        every { savedListRepository.getSavedLists() } returns flowOf(emptyList())
        every { chatRepository.getConversations(any()) } returns flowOf(emptyList())

        viewModel = AssistantViewModel(
            listRepository = listRepository,
            savedListRepository = savedListRepository,
            filterCookableItemsUseCase = filterCookableItemsUseCase,
            chatRepository = chatRepository,
            aiRepository = aiRepository,
            auth = auth
        )
    }

    @Test
    fun `initial state contains greeting message and filters out non-cookable items`() = runTest {
        val state = viewModel.uiState.value
        assertTrue(state.messages.isNotEmpty())
        assertEquals("model", state.messages.first().role)
        
        // Detergente should be filtered out by FilterCookableItemsUseCase
        val ingredientNames = state.availableIngredients.map { it.name }
        assertTrue(ingredientNames.contains("Pollo"))
        assertTrue(ingredientNames.contains("Arroz"))
        assertFalse(ingredientNames.contains("Detergente"))
    }

    @Test
    fun `sendMessage success updates uiState with AI recipe response and saves session`() = runTest {
        val expectedRecipe = RecipeCard(
            title = "Arroz con Pollo Express",
            subtitle = "Rápido y rendidor",
            calories = "~400 kcal",
            prepTime = "30 min",
            servings = 4,
            ingredients = listOf("Pollo", "Arroz", "Cebolla"),
            steps = listOf("Dorar el pollo", "Agregar el arroz y agua", "Cocinar 20 min"),
            tips = "Usa caldo de pollo para mejor sabor"
        )

        val aiResponse = ChatMessage(
            id = "ai_msg_1",
            role = "model",
            text = "¡Aquí tienes una receta deliciosa!",
            recipeCard = expectedRecipe
        )

        coEvery {
            aiRepository.generateRecipeResponse(any(), any(), any())
        } returns Result.success(aiResponse)

        coEvery { chatRepository.saveConversation(any()) } returns Result.success(Unit)

        viewModel.onInputTextChanged("Quiero cocinar algo rápido")
        viewModel.sendMessage()

        val state = viewModel.uiState.value
        assertFalse(state.isThinking)
        assertEquals(3, state.messages.size) // Greeting + User + Model AI
        
        val lastMessage = state.messages.last()
        assertEquals("model", lastMessage.role)
        assertNotNull(lastMessage.recipeCard)
        assertEquals("Arroz con Pollo Express", lastMessage.recipeCard?.title)
    }

    @Test
    fun `sendMessage failure emits snackbar event and resets isThinking`() = runTest {
        coEvery {
            aiRepository.generateRecipeResponse(any(), any(), any())
        } returns Result.failure(Exception("Unable to resolve host"))

        viewModel.uiEvent.test {
            viewModel.onInputTextChanged("Dame una receta")
            viewModel.sendMessage()

            val event = awaitItem()
            assertTrue(event is ChefChatUiEvent.ShowSnackbar)
            val snackbarEvent = event as ChefChatUiEvent.ShowSnackbar
            assertTrue(snackbarEvent.message.contains("Sin conexión"))
            
            assertFalse(viewModel.uiState.value.isThinking)
        }
    }

    @Test
    fun `toggleIngredientSelection limits to maximum 5 ingredients`() = runTest {
        // Prepare list with 6 cookable items
        every { listRepository.getItems() } returns flowOf(
            (1..6).map { ShoppingItem(id = "$it", name = "Ingrediente $it", category = "Varios", isCompleted = true) }
        )

        val vm = AssistantViewModel(
            listRepository = listRepository,
            savedListRepository = savedListRepository,
            filterCookableItemsUseCase = filterCookableItemsUseCase,
            chatRepository = chatRepository,
            aiRepository = aiRepository,
            auth = auth
        )

        val chips = vm.uiState.value.availableIngredients
        assertEquals(6, chips.size)

        // Select first 5
        chips.take(5).forEach { vm.toggleIngredientSelection(it.id) }
        assertEquals(5, vm.uiState.value.availableIngredients.count { it.isSelected })

        // Attempt to select 6th
        vm.uiEvent.test {
            vm.toggleIngredientSelection(chips[5].id)
            val event = awaitItem()
            assertTrue(event is ChefChatUiEvent.ShowSnackbar)
            assertEquals(5, vm.uiState.value.availableIngredients.count { it.isSelected })
        }
    }
}
