package com.farbalapps.rinde.ui.screen.home.assistant

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.farbalapps.rinde.domain.model.ChatConversation
import com.farbalapps.rinde.domain.model.ChatMessage
import com.farbalapps.rinde.domain.model.RecipeCard
import com.farbalapps.rinde.ui.theme.RindePrimary
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: AssistantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showListDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is ChefChatUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // Scroll automático al último mensaje cuando aumenta la cantidad
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(RindePrimary.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = RindePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Chef",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Recetas y Cocina",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Historial de Sesiones (Máx 10)
                    IconButton(onClick = { viewModel.toggleHistorySheet(true) }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Historial de recetas",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Nueva Conversación
                    IconButton(onClick = { viewModel.startNewConversation() }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Nueva conversación",
                            tint = RindePrimary
                        )
                    }
                },
                windowInsets = WindowInsets(0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding())
                .imePadding()
        ) {
            // Lista de Mensajes
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    ChefChatMessageItem(message = message)
                }

                if (uiState.isThinking) {
                    item {
                        ThinkingIndicatorItem()
                    }
                }
            }

            // Context Bar (Listas e Ingredientes con contador X/5)
            PantryContextBar(
                uiState = uiState,
                showListDropdown = showListDropdown,
                onShowListDropdownChange = { showListDropdown = it },
                onSelectList = { viewModel.selectList(it) },
                onToggleIngredient = { viewModel.toggleIngredientSelection(it) }
            )

            // Dock de Entrada (máx 150 caracteres)
            ChefChatInputDock(
                inputText = uiState.inputText,
                onInputTextChanged = { viewModel.onInputTextChanged(it) },
                onSendMessage = { viewModel.sendMessage() }
            )
        }
    }

    // Modal BottomSheet para el Historial de Conversaciones (Máx 10)
    if (uiState.showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleHistorySheet(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ChatHistorySheetContent(
                conversations = uiState.historyConversations,
                onSelectConversation = { viewModel.loadConversation(it) },
                onDeleteConversation = { viewModel.deleteConversation(it) },
                onNewConversation = { viewModel.startNewConversation() }
            )
        }
    }
}

@Composable
private fun ChefChatMessageItem(message: ChatMessage) {
    val isUser = message.role == "user"
    val formattedTime = remember(message.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
    }

    // Determine if this is a freshly received/initialized message
    val isRecentMessage = remember(message.id) {
        (System.currentTimeMillis() - message.timestamp) < 5000L
    }

    var displayedText by remember(message.id) {
        mutableStateOf(if (isUser || !isRecentMessage) message.text else "")
    }
    var isTypingComplete by remember(message.id) {
        mutableStateOf(isUser || !isRecentMessage)
    }

    // Streaming effect (stream text word by word like ChatGPT/Gemini)
    LaunchedEffect(message.id, message.text) {
        if (isUser || !isRecentMessage) {
            displayedText = message.text
            isTypingComplete = true
        } else {
            // Split preserving whitespace and newlines
            val words = message.text.split(Regex("(?<=\\s)"))
            val builder = StringBuilder()
            for (word in words) {
                builder.append(word)
                displayedText = builder.toString()
                delay(40L) // smooth delay per word
            }
            displayedText = message.text
            isTypingComplete = true
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = if (isUser) {
                RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
            } else {
                RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)
            },
            color = if (isUser) RindePrimary else MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = displayedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )

                message.recipeCard?.let { recipe ->
                    AnimatedVisibility(
                        visible = isTypingComplete,
                        enter = fadeIn() + expandVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            RecipeCardItem(recipe = recipe)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun RecipeCardItem(recipe: RecipeCard) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipe.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (recipe.subtitle.isNotBlank()) {
                        Text(
                            text = recipe.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = RindePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = RindePrimary.copy(alpha = 0.15f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = RindePrimary,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(16.dp)
                    )
                }
            }

            if (recipe.ingredients.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ingredientes: " + recipe.ingredients.joinToString(", "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (recipe.steps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pasos de Preparación:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                recipe.steps.forEachIndexed { index, step ->
                    Text(
                        text = "${index + 1}. $step",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            recipe.tips?.let { tip ->
                if (tip.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "💡 Tip: $tip",
                        style = MaterialTheme.typography.labelSmall,
                        color = RindePrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinkingIndicatorItem() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinkingPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = RindePrimary.copy(alpha = alpha),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Chef IA procesando tu receta...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
            )
        }
    }
}

@Composable
private fun PantryContextBar(
    uiState: ChefChatUiState,
    showListDropdown: Boolean,
    onShowListDropdownChange: (Boolean) -> Unit,
    onSelectList: (String) -> Unit,
    onToggleIngredient: (String) -> Unit
) {
    val selectedCount = uiState.availableIngredients.count { it.isSelected }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { onShowListDropdownChange(true) }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ListAlt,
                            contentDescription = null,
                            tint = RindePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = uiState.selectedListName.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showListDropdown,
                        onDismissRequest = { onShowListDropdownChange(false) }
                    ) {
                        uiState.availableLists.forEach { listName ->
                            DropdownMenuItem(
                                text = { Text(listName) },
                                onClick = {
                                    onSelectList(listName)
                                    onShowListDropdownChange(false)
                                }
                            )
                        }
                    }
                }

                // Ingredient Counter X / 5
                Text(
                    text = "Seleccionados: $selectedCount / 5",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selectedCount == 5) MaterialTheme.colorScheme.error else RindePrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.availableIngredients.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Sin productos comprados en esta lista.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.availableIngredients, key = { it.id }) { chip ->
                        if (chip.isCookable) {
                            // ✅ Cookable item: interactive FilterChip
                            FilterChip(
                                selected = chip.isSelected,
                                onClick = { onToggleIngredient(chip.id) },
                                label = {
                                    Text(
                                        text = chip.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (chip.isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (chip.isSelected) Icons.Default.CheckCircle else Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RindePrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedLeadingIconColor = RindePrimary,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(50)
                            )
                        } else {
                            // 🚫 Non-cookable item: disabled FilterChip (exact same height, shape & vertical alignment)
                            FilterChip(
                                selected = false,
                                onClick = { },
                                enabled = false,
                                label = {
                                    Text(
                                        text = chip.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Text(
                                        text = "🚫",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = false,
                                    selected = false,
                                    disabledBorderColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChefChatInputDock(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val charCount = inputText.length
    val isLimitReached = charCount >= 150

    val handleSend = {
        keyboardController?.hide()
        focusManager.clearFocus()
        onSendMessage()
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChanged,
                    placeholder = {
                        Text(
                            text = "Escribe una consulta o receta...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    supportingText = {
                        // Contador 150 caracteres máximo
                        Text(
                            text = "$charCount / 150",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isLimitReached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() || charCount > 0) {
                                handleSend()
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = if (isLimitReached) MaterialTheme.colorScheme.error else RindePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                IconButton(
                    onClick = handleSend,
                    enabled = inputText.isNotBlank() || charCount > 0,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank()) RindePrimary else MaterialTheme.colorScheme.surfaceVariant
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = if (inputText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatHistorySheetContent(
    conversations: List<ChatConversation>,
    onSelectConversation: (ChatConversation) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onNewConversation: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Historial de Recetas (Máx 10)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onNewConversation,
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nueva", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (conversations.isEmpty()) {
            Text(
                text = "No tienes conversaciones guardadas aún.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(conversations, key = { it.id }) { item ->
                    Card(
                        onClick = { onSelectConversation(item) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.messages.size} mensajes • ${item.sourceListName ?: "Despensa"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = { onDeleteConversation(item.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
