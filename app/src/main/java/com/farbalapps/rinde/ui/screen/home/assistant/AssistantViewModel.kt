package com.farbalapps.rinde.ui.screen.home.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farbalapps.rinde.domain.model.ChatConversation
import com.farbalapps.rinde.domain.model.ChatMessage
import com.farbalapps.rinde.domain.model.RecipeCard
import com.farbalapps.rinde.domain.model.SavedShoppingList
import com.farbalapps.rinde.domain.model.ShoppingItem
import com.farbalapps.rinde.domain.repository.AiRepository
import com.farbalapps.rinde.domain.repository.ChatRepository
import com.farbalapps.rinde.domain.repository.ListRepository
import com.farbalapps.rinde.domain.repository.SavedListRepository
import com.farbalapps.rinde.domain.usecase.FilterCookableItemsUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class IngredientChipState(
    val id: String,
    val name: String,
    val isSelected: Boolean = false, // ALL UNSELECTED BY DEFAULT
    val isCookable: Boolean = true   // false for pet food, cleaning products, etc.
)

data class ChefChatUiState(
    val currentConversationId: String = UUID.randomUUID().toString(),
    val messages: List<ChatMessage> = emptyList(),
    val availableLists: List<String> = listOf("Mi Lista Actual"),
    val selectedListName: String = "Mi Lista Actual",
    val availableIngredients: List<IngredientChipState> = emptyList(),
    val isThinking: Boolean = false,
    val inputText: String = "",
    val historyConversations: List<ChatConversation> = emptyList(),
    val showHistorySheet: Boolean = false
)

sealed interface ChefChatUiEvent {
    data class ShowSnackbar(val message: String) : ChefChatUiEvent
}

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val listRepository: ListRepository,
    private val savedListRepository: SavedListRepository,
    private val filterCookableItemsUseCase: FilterCookableItemsUseCase,
    private val chatRepository: ChatRepository,
    private val aiRepository: AiRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChefChatUiState())
    val uiState: StateFlow<ChefChatUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ChefChatUiEvent>()
    val uiEvent: SharedFlow<ChefChatUiEvent> = _uiEvent.asSharedFlow()

    private var savedLists: List<SavedShoppingList> = emptyList()
    private var activeShoppingItems: List<ShoppingItem> = emptyList()

    private val fullGreetingText =
        "¡Hola! Soy tu Chef IA de Rinde 👨‍🍳✨. Puedo ayudarte a crear recetas deliciosas y saludables aprovechando al máximo los ingredientes de tu despensa o lista de compras. ¿Qué vamos a cocinar hoy?"

    init {
        observeListsAndIngredients()
        observeHistoryConversations()
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
                    val validSelected = getValidSelectedList(listNames)
                    val ingredientChips = buildIngredientChipsForList(validSelected)

                    Triple(listNames, validSelected, ingredientChips)
                }.collect { (listNames, validSelected, ingredientChips) ->
                    _uiState.update { current ->
                        current.copy(
                            availableLists = listNames,
                            selectedListName = validSelected,
                            availableIngredients = ingredientChips
                        )
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun observeHistoryConversations() {
        val currentUserId = auth.currentUser?.uid.orEmpty()
        if (currentUserId.isNotEmpty()) {
            viewModelScope.launch {
                chatRepository.getConversations(currentUserId).collect { conversations ->
                    _uiState.update { it.copy(historyConversations = conversations) }
                }
            }
        }
    }

    private fun getValidSelectedList(listNames: List<String>): String {
        val currentSelected = _uiState.value.selectedListName
        return if (listNames.contains(currentSelected)) currentSelected else "Mi Lista Actual"
    }

    /**
     * Builds ingredient chips for a given list.
     * ALL purchased items are shown, but non-cookable items get isCookable=false
     * so the UI can display them as disabled chips (greyed-out with 🚫 icon).
     */
    private fun buildIngredientChipsForList(listName: String): List<IngredientChipState> {
        val purchasedItems = getPurchasedItemsForListName(listName)

        return purchasedItems.mapIndexed { index, item ->
            IngredientChipState(
                id = "$listName-$index-${item.name}",
                name = item.name,
                isSelected = false,
                isCookable = filterCookableItemsUseCase.isCookable(item.name, item.category)
            )
        }
    }

    /**
     * Returns only PURCHASED items (isCompleted=true) for Chef IA.
     *
     * Rationale: isCompleted means the user has already bought the item.
     * These are the ingredients they have at home and can cook with.
     * Non-completed items are still pending purchase → NOT available yet.
     *
     * Both "Mi Lista Actual" and saved lists apply the same filter.
     */
    private fun getPurchasedItemsForListName(listName: String): List<com.farbalapps.rinde.domain.model.ShoppingItem> {
        return if (listName == "Mi Lista Actual") {
            // Only show PURCHASED items (checked = already at home)
            activeShoppingItems.filter { it.isCompleted }
        } else {
            // For saved lists: also only show the completed/purchased items
            savedLists.find { it.name == listName }
                ?.items
                ?.filter { it.isCompleted }
                ?: emptyList()
        }
    }

    private fun initWelcomeMessage() {
        val welcomeMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = "model",
            text = fullGreetingText,
            timestamp = System.currentTimeMillis()
        )
        _uiState.update { state -> state.copy(messages = listOf(welcomeMsg)) }
    }

    fun onInputTextChanged(newText: String) {
        // Enforce max 150 character limit
        if (newText.length <= 150) {
            _uiState.update { it.copy(inputText = newText) }
        }
    }

    fun selectList(listName: String) {
        val ingredientChips = buildIngredientChipsForList(listName)
        _uiState.update { state ->
            state.copy(
                selectedListName = listName,
                availableIngredients = ingredientChips
            )
        }
    }

    fun toggleIngredientSelection(ingredientId: String) {
        val targetChip = _uiState.value.availableIngredients.find { it.id == ingredientId } ?: return

        // Non-cookable items (pet food, cleaning products, etc.) cannot be selected
        if (!targetChip.isCookable) return

        val currentSelectedCount = _uiState.value.availableIngredients.count { it.isSelected }

        // Enforce maximum 5 ingredients selected limit
        if (!targetChip.isSelected && currentSelectedCount >= 5) {
            viewModelScope.launch {
                _uiEvent.emit(ChefChatUiEvent.ShowSnackbar("Máximo 5 ingredientes permitidos. Deselecciona uno para cambiar."))
            }
            return
        }

        _uiState.update { state ->
            val updatedChips = state.availableIngredients.map { chip ->
                if (chip.id == ingredientId) chip.copy(isSelected = !chip.isSelected) else chip
            }
            state.copy(availableIngredients = updatedChips)
        }
    }

    fun sendMessage() {
        val textToSend = _uiState.value.inputText.trim().take(150)
        val selectedIngredients = getSelectedIngredients()

        if (textToSend.isEmpty() && selectedIngredients.isEmpty()) return

        val promptText = textToSend.ifEmpty { "Recomiéndame una receta con estos ingredientes." }

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            text = promptText,
            timestamp = System.currentTimeMillis()
        )

        val previousMessages = _uiState.value.messages

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                inputText = "",
                isThinking = true
            )
        }

        viewModelScope.launch {
            val result = aiRepository.generateRecipeResponse(
                history = previousMessages,
                userPrompt = textToSend,
                selectedIngredients = selectedIngredients
            )

            result.fold(
                onSuccess = { responseMessage ->
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + responseMessage,
                            isThinking = false
                        )
                    }
                    saveCurrentSession()
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(isThinking = false)
                    }

                    val errorMsg = error.message.orEmpty()
                    val exceptionName = error.javaClass.simpleName

                    android.util.Log.e(
                        "ChefAI_ViewModel",
                        "❌ [CHEF AI UI ERROR] Fallo al enviar mensaje: $exceptionName - $errorMsg",
                        error
                    )

                    val userFriendlyMessage = when {
                        errorMsg.contains("Unable to resolve host", ignoreCase = true) ||
                        errorMsg.contains("timeout", ignoreCase = true) ||
                        errorMsg.contains("connect", ignoreCase = true) -> {
                            "Sin conexión. Por favor verifica tu acceso a internet."
                        }
                        errorMsg.contains("429", ignoreCase = true) ||
                        errorMsg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                        errorMsg.contains("Quota exceeded", ignoreCase = true) -> {
                            "Límite de consultas alcanzado. Por favor espera un momento."
                        }
                        errorMsg.contains("403", ignoreCase = true) ||
                        errorMsg.contains("401", ignoreCase = true) ||
                        errorMsg.contains("API_KEY_INVALID", ignoreCase = true) ||
                        errorMsg.contains("AppCheck", ignoreCase = true) ||
                        errorMsg.contains("PERMISSION_DENIED", ignoreCase = true) -> {
                            "Servicio temporalmente no disponible (verifica permisos en Firebase Console)."
                        }
                        errorMsg.contains("SAFETY", ignoreCase = true) ||
                        errorMsg.contains("BLOCKED", ignoreCase = true) -> {
                            "La consulta no pudo procesarse por políticas de seguridad del Chef IA."
                        }
                        errorMsg.contains("User location", ignoreCase = true) ||
                        errorMsg.contains("not supported", ignoreCase = true) -> {
                            "El servicio de Chef IA no está disponible en tu región actual."
                        }
                        else -> {
                            "No se pudo consultar al Chef IA. Inténtalo de nuevo."
                        }
                    }
                    _uiEvent.emit(ChefChatUiEvent.ShowSnackbar(userFriendlyMessage))
                }
            )
        }
    }

    private fun getSelectedIngredients(): List<String> {
        return _uiState.value.availableIngredients
            .filter { it.isSelected && it.isCookable } // Safety: only cookable items sent to IA
            .map { it.name }
            .take(5)
    }

    private fun saveCurrentSession() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val firstRecipe = currentState.messages.firstNotNullOfOrNull { it.recipeCard }
            val firstUserMsg = currentState.messages.firstOrNull { it.role == "user" }?.text

            val title = firstRecipe?.title ?: firstUserMsg?.take(25) ?: "Sesión de Receta"
            val userId = auth.currentUser?.uid.orEmpty()

            if (userId.isNotEmpty() && currentState.messages.size > 1) {
                val conversation = ChatConversation(
                    id = currentState.currentConversationId,
                    userId = userId,
                    title = title,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    sourceListName = currentState.selectedListName,
                    messages = currentState.messages
                )
                chatRepository.saveConversation(conversation)
            }
        }
    }

    fun startNewConversation() {
        val newId = UUID.randomUUID().toString()
        _uiState.update { state ->
            state.copy(
                currentConversationId = newId,
                messages = emptyList(),
                isThinking = false,
                inputText = "",
                showHistorySheet = false
            )
        }
        initWelcomeMessage()
    }

    fun loadConversation(conversation: ChatConversation) {
        _uiState.update { state ->
            state.copy(
                currentConversationId = conversation.id,
                messages = conversation.messages,
                selectedListName = conversation.sourceListName ?: "Mi Lista Actual",
                showHistorySheet = false
            )
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(id)
        }
    }

    fun toggleHistorySheet(show: Boolean) {
        _uiState.update { it.copy(showHistorySheet = show) }
    }
}
