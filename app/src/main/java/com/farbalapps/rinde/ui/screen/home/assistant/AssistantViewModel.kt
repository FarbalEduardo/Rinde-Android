package com.farbalapps.rinde.ui.screen.home.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.domain.model.SavedShoppingList
import com.farbalapps.rinde.domain.model.ShoppingItem
import com.farbalapps.rinde.domain.repository.ListRepository
import com.farbalapps.rinde.domain.repository.SavedListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

enum class MessageSender {
    USER, CHEF
}

data class RecipeRecommendation(
    val title: String,
    val subtitle: String,
    val calories: String,
    val prepTime: String,
    val imageUrl: String? = null,
    val ingredients: List<String> = emptyList()
)

data class ChefChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: String = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
    val recipeCard: RecipeRecommendation? = null,
    val isStreaming: Boolean = false
)

data class IngredientChipState(
    val id: String,
    val name: String,
    val isSelected: Boolean = true
)

data class ChefChatUiState(
    val messages: List<ChefChatMessage> = emptyList(),
    val availableLists: List<String> = listOf("Mi Lista Actual"),
    val selectedListName: String = "Mi Lista Actual",
    val availableIngredients: List<IngredientChipState> = emptyList(),
    val isThinking: Boolean = false,
    val inputText: String = ""
)

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val listRepository: ListRepository,
    private val savedListRepository: SavedListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChefChatUiState())
    val uiState: StateFlow<ChefChatUiState> = _uiState.asStateFlow()

    private var hasPlayedInitialAnimation = false

    private var savedLists: List<SavedShoppingList> = emptyList()
    private var activeShoppingItems: List<ShoppingItem> = emptyList()

    private val defaultFallbackIngredients = listOf(
        IngredientChipState("def-1", "Limones", isSelected = true),
        IngredientChipState("def-2", "Salmón", isSelected = true),
        IngredientChipState("def-3", "Uvas", isSelected = true),
        IngredientChipState("def-4", "Leche", isSelected = false),
        IngredientChipState("def-5", "Espinacas", isSelected = false),
        IngredientChipState("def-6", "Ajo", isSelected = false)
    )

    private val fullGreetingText =
        "¡Hola! Soy tu Chef IA de Rinde 👨‍🍳✨. Puedo ayudarte a crear recetas deliciosas y saludables aprovechando al máximo los ingredientes de tu despensa o lista de compras. ¿Qué vamos a cocinar hoy?"

    init {
        observeListsAndIngredients()
        initWelcomeMessage()
    }

    private fun observeListsAndIngredients() {
        viewModelScope.launch {
            try {
                combine(
                    listRepository.getItems(),
                    savedListRepository.getSavedLists()
                ) { activeItems, savedListsList ->
                    activeShoppingItems = activeItems
                    savedLists = savedListsList

                    val listNames = listOf("Mi Lista Actual") + savedListsList.map { it.name }
                    val currentSelected = _uiState.value.selectedListName
                    val validSelected = if (listNames.contains(currentSelected)) currentSelected else "Mi Lista Actual"

                    val ingredientNames = getIngredientsForListName(validSelected)
                    val ingredientChips = if (ingredientNames.isNotEmpty()) {
                        ingredientNames.take(10).mapIndexed { index, name ->
                            IngredientChipState(
                                id = "$validSelected-$index-$name",
                                name = name,
                                isSelected = index < 3
                            )
                        }
                    } else {
                        emptyList()
                    }

                    _uiState.value.copy(
                        availableLists = listNames,
                        selectedListName = validSelected,
                        availableIngredients = ingredientChips
                    )
                }.collect { updatedState ->
                    _uiState.value = updatedState
                }
            } catch (_: Exception) { }
        }
    }

    private fun getIngredientsForListName(listName: String): List<String> {
        return if (listName == "Mi Lista Actual") {
            activeShoppingItems.filter { !it.isCompleted }.map { it.name }
        } else {
            savedLists.find { it.name == listName }?.items?.map { it.name } ?: emptyList()
        }
    }

    private fun initWelcomeMessage() {
        if (hasPlayedInitialAnimation) return

        viewModelScope.launch {
            val initialMessageId = UUID.randomUUID().toString()
            val initialMessage = ChefChatMessage(
                id = initialMessageId,
                sender = MessageSender.CHEF,
                text = "",
                isStreaming = true
            )

            _uiState.update { state ->
                state.copy(messages = listOf(initialMessage))
            }

            val builder = StringBuilder()
            for (char in fullGreetingText) {
                builder.append(char)
                delay(20)
                _uiState.update { state ->
                    val updatedMessages = state.messages.map { msg ->
                        if (msg.id == initialMessageId) msg.copy(text = builder.toString()) else msg
                    }
                    state.copy(messages = updatedMessages)
                }
            }

            _uiState.update { state ->
                val finalMessages = state.messages.map { msg ->
                    if (msg.id == initialMessageId) msg.copy(isStreaming = false) else msg
                }
                state.copy(messages = finalMessages)
            }
            hasPlayedInitialAnimation = true
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun selectList(listName: String) {
        val ingredientNames = getIngredientsForListName(listName)
        val ingredientChips = if (ingredientNames.isNotEmpty()) {
            ingredientNames.take(10).mapIndexed { index, name ->
                IngredientChipState(
                    id = "$listName-$index-$name",
                    name = name,
                    isSelected = index < 3
                )
            }
        } else {
            emptyList()
        }

        _uiState.update { state ->
            state.copy(
                selectedListName = listName,
                availableIngredients = ingredientChips
            )
        }
    }

    fun toggleIngredientSelection(ingredientId: String) {
        _uiState.update { state ->
            val updatedChips = state.availableIngredients.map { chip ->
                if (chip.id == ingredientId) chip.copy(isSelected = !chip.isSelected) else chip
            }
            state.copy(availableIngredients = updatedChips)
        }
    }

    fun sendMessage(userTextPrompt: String? = null) {
        val textToSend = userTextPrompt ?: _uiState.value.inputText.trim()
        if (textToSend.isEmpty()) return

        val selectedIngredients = _uiState.value.availableIngredients
            .filter { it.isSelected }
            .map { it.name }

        val userMsg = ChefChatMessage(
            sender = MessageSender.USER,
            text = textToSend
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMsg,
                inputText = "",
                isThinking = true
            )
        }

        viewModelScope.launch {
            delay(1500)

            val recipe = if (selectedIngredients.any { it.contains("Salmón", ignoreCase = true) } || textToSend.lowercase().contains("salmón")) {
                RecipeRecommendation(
                    title = "Salmón Glaseado al Cítrico",
                    subtitle = "Alto en Proteína • 340 kcal",
                    calories = "340 kcal",
                    prepTime = "20 min",
                    ingredients = listOf("Salmón", "Limones", "Uvas frescas")
                )
            } else {
                RecipeRecommendation(
                    title = "Bowl Fresco de la Despensa",
                    subtitle = "Receta Balanceada • 280 kcal",
                    calories = "280 kcal",
                    prepTime = "15 min",
                    ingredients = selectedIngredients.ifEmpty { listOf("Espinacas", "Limones", "Ajo") }
                )
            }

            val chefResponse = ChefChatMessage(
                sender = MessageSender.CHEF,
                text = "¡Excelente idea! Analizando tus ingredientes (${selectedIngredients.joinToString(", ")}), te recomiendo preparar **${recipe.title}**. ¡Toma solo ${recipe.prepTime}!",
                recipeCard = recipe
            )

            _uiState.update { state ->
                state.copy(
                    messages = state.messages + chefResponse,
                    isThinking = false
                )
            }
        }
    }

    fun resetChat() {
        hasPlayedInitialAnimation = false
        _uiState.update { state ->
            state.copy(
                messages = emptyList(),
                isThinking = false
            )
        }
        initWelcomeMessage()
    }
}
