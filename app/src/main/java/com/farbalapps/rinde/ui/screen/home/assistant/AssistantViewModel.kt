package com.farbalapps.rinde.ui.screen.home.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.domain.repository.ListRepository
import com.farbalapps.rinde.domain.repository.SavedListRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val availableLists: List<String> = listOf("Mi Lista Actual", "Despensa Semanal", "Compras Saludables"),
    val selectedListName: String = "Mi Lista Actual",
    val availableIngredients: List<IngredientChipState> = listOf(
        IngredientChipState("1", "Limones", isSelected = true),
        IngredientChipState("2", "Salmón", isSelected = true),
        IngredientChipState("3", "Uvas", isSelected = true),
        IngredientChipState("4", "Leche", isSelected = false),
        IngredientChipState("5", "Espinacas", isSelected = false),
        IngredientChipState("6", "Ajo", isSelected = false)
    ),
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

    private val fullGreetingText =
        "¡Hola! Soy tu Chef IA de Rinde 👨‍🍳✨. Puedo ayudarte a crear recetas deliciosas y saludables aprovechando al máximo los ingredientes de tu despensa o lista de compras. ¿Qué vamos a cocinar hoy?"

    init {
        loadPantryIngredients()
        initWelcomeMessage()
    }

    private fun loadPantryIngredients() {
        viewModelScope.launch {
            try {
                listRepository.getItems().collect { items ->
                    val activeItems = items.filter { !it.isCompleted }
                    if (activeItems.isNotEmpty()) {
                        val pantryChips = activeItems.take(8).mapIndexed { index, item ->
                            IngredientChipState(
                                id = item.id,
                                name = item.name,
                                isSelected = index < 3
                            )
                        }
                        _uiState.update { it.copy(availableIngredients = pantryChips) }
                    }
                }
            } catch (_: Exception) { }
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

            // Efecto Typewriter: desglosa caracter por caracter como una IA en vivo
            val builder = StringBuilder()
            for (char in fullGreetingText) {
                builder.append(char)
                delay(20) // 20ms por caracter
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
        _uiState.update { it.copy(selectedListName = listName) }
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
            delay(1500) // Simular tiempo de procesamiento de la IA Chef

            val recipe = if (selectedIngredients.contains("Salmón") || textToSend.lowercase().contains("salmón")) {
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
