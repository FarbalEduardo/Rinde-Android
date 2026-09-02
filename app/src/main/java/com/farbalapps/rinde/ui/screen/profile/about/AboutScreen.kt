package com.farbalapps.rinde.ui.screen.profile.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farbalapps.rinde.ui.theme.RindePrimary
import kotlinx.coroutines.delay

private data class Feature(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color
)

private data class TechChip(val label: String, val icon: ImageVector)

private data class StatItem(val value: String, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val listState = rememberLazyListState()

    var heroVisible by remember { mutableStateOf(false) }
    var featuresVisible by remember { mutableStateOf(false) }
    var techVisible by remember { mutableStateOf(false) }
    var statsVisible by remember { mutableStateOf(false) }
    var businessVisible by remember { mutableStateOf(false) }
    var footerVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        heroVisible = true
        delay(150)
        featuresVisible = true
        delay(100)
        techVisible = true
        delay(100)
        statsVisible = true
        delay(100)
        businessVisible = true
        delay(100)
        footerVisible = true
    }

    val features = remember {
        listOf(
            Feature(Icons.Default.Psychology, "IA con Google Gemini",
                "Escanea etiquetas y analiza ofertas complejas (3x2, segundo al 50%) para darte el precio neto real por unidad.",
                Color(0xFF4285F4)),
            Feature(Icons.Default.People, "Comunidad Activa",
                "Miles de cazadores de ofertas comparten precios en tiempo real desde tiendas fisicas de tu zona.",
                Color(0xFF34A853)),
            Feature(Icons.Default.Notifications, "Radar de Alertas",
                "Crea tu wishlist y recibe notificaciones push en cuanto la comunidad o tiendas en linea bajen el precio.",
                Color(0xFFFA7B17)),
            Feature(Icons.AutoMirrored.Filled.CompareArrows, "Comparativa 360 grados",
                "Compara precios de la comunidad contra Amazon, Mercado Libre y Google Shopping en una sola vista.",
                Color(0xFF9334E6)),
            Feature(Icons.Default.Shield, "Privacidad Primero",
                "Zonas de caza generales sin GPS exacto, y un algoritmo de veracidad que protege a la comunidad.",
                Color(0xFFEA4335)),
            Feature(Icons.Default.CloudOff, "Modo Bunker Offline",
                "Escanea y guarda productos sin senal dentro de la tienda. Todo se sincroniza al detectar conexion.",
                Color(0xFF00BCD4))
        )
    }

    val techStack = remember {
        listOf(
            TechChip("Kotlin 2.1", Icons.Default.Code),
            TechChip("Jetpack Compose", Icons.Default.PhoneAndroid),
            TechChip("Google Gemini", Icons.Default.AutoAwesome),
            TechChip("Firebase", Icons.Default.Cloud),
            TechChip("Room Offline", Icons.Default.Storage),
            TechChip("Hilt DI", Icons.Default.AccountTree),
            TechChip("Clean Architecture", Icons.Default.Architecture),
            TechChip("Cloudinary", Icons.Default.Image)
        )
    }

    val stats = remember {
        listOf(
            StatItem("1.1.1", "Version Beta"),
            StatItem("Android 8+", "Plataforma"),
            StatItem("API 26+", "SDK Minimo"),
            StatItem("2026", "Inicio")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                AnimatedVisibility(visible = heroVisible, enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -40 }) {
                    AboutHeroSection()
                }
            }

            item {
                AnimatedVisibility(visible = heroVisible, enter = fadeIn(tween(600)) + slideInVertically(tween(500)) { 30 }) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Rinde es tu asistente inteligente de ahorro impulsado por IA y por una comunidad de cazadores de ofertas. Compara precios, valida ofertas en tiempo real y ahorra en cada compra.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp)
                    )
                }
            }

            item {
                AnimatedVisibility(visible = statsVisible, enter = fadeIn(tween(500)) + slideInVertically(tween(450)) { 30 }) {
                    Spacer(Modifier.height(28.dp))
                    AboutStatsRow(stats)
                }
            }

            item {
                AnimatedVisibility(visible = featuresVisible, enter = fadeIn(tween(500)) + slideInVertically(tween(450)) { 30 }) {
                    Spacer(Modifier.height(28.dp))
                    AboutSectionTitle(title = "Que puedes hacer con Rinde", modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(12.dp))
                }
            }

            items(features) { feature ->
                AnimatedVisibility(visible = featuresVisible, enter = fadeIn(tween(500)) + slideInVertically(tween(400)) { 40 }) {
                    FeatureCard(feature = feature)
                    Spacer(Modifier.height(10.dp))
                }
            }

            item {
                AnimatedVisibility(visible = techVisible, enter = fadeIn(tween(500)) + slideInVertically(tween(450)) { 30 }) {
                    Spacer(Modifier.height(24.dp))
                    AboutSectionTitle(title = "Tecnologia de vanguardia", modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(techStack) { chip -> TechChipItem(chip) }
                    }
                }
            }

            item {
                AnimatedVisibility(visible = businessVisible, enter = fadeIn(tween(500)) + slideInVertically(tween(450)) { 30 }) {
                    Spacer(Modifier.height(28.dp))
                    AboutSectionTitle(title = "Modelo Freemium", modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(12.dp))
                    FreemiumCard()
                }
            }

            item {
                AnimatedVisibility(visible = footerVisible, enter = fadeIn(tween(500)) + slideInVertically(tween(450)) { 30 }) {
                    Spacer(Modifier.height(28.dp))
                    AboutSectionTitle(title = "Soporte y Proyecto", modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(Modifier.height(12.dp))
                    AboutLinkCard(
                        icon = Icons.Default.Code,
                        title = "Codigo fuente",
                        subtitle = "github.com/FarbalEduardo/Rinde-Android",
                        onClick = { runCatching { uriHandler.openUri("https://github.com/FarbalEduardo/Rinde-Android") } }
                    )
                    Spacer(Modifier.height(10.dp))
                    AboutLinkCard(
                        icon = Icons.Default.BugReport,
                        title = "Reportar un problema",
                        subtitle = "Abre un issue en GitHub para ayudarnos a mejorar",
                        onClick = { runCatching { uriHandler.openUri("https://github.com/FarbalEduardo/Rinde-Android/issues") } }
                    )
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = "Desarrollado con amor como proyecto de portafolio\npor Eduardo Farbal - 2026",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutHeroSection() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(Brush.linearGradient(colors = listOf(RindePrimary, Color(0xFF1565C0)))),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "R", style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 42.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text(text = "Rinde", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(4.dp))
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Text(text = "v1.1.1 Beta", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
    }
}

@Composable
private fun AboutStatsRow(stats: List<StatItem>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            stats.forEach { stat ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stat.value, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(2.dp))
                    Text(text = stat.label, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(feature: Feature) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(feature.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = feature.icon, contentDescription = null, tint = feature.color, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = feature.title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(3.dp))
                Text(text = feature.description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun TechChipItem(chip: TechChip) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(imageVector = chip.icon, contentDescription = null, modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(text = chip.label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun FreemiumCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            FreemiumTier(Icons.Default.StarBorder, "Gratis",
                "Publicaciones ilimitadas para incentivar a la comunidad y cuota limitada de escaneos inteligentes.",
                MaterialTheme.colorScheme.primary)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            FreemiumTier(Icons.Default.Star, "Pro - \$1 USD/mes",
                "Escaneos ilimitados, alertas de stock prioritarias y busqueda global avanzada en tiempo real.",
                Color(0xFFFA7B17))
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            FreemiumTier(Icons.Default.EmojiEvents, "Moneda Social",
                "Los usuarios con ofertas validadas desbloquean escaneos premium sin costo alguno.",
                Color(0xFF34A853))
        }
    }
}

@Composable
private fun FreemiumTier(icon: ImageVector, tier: String, description: String, color: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp).padding(top = 1.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(text = tier, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = color)
            Spacer(Modifier.height(2.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun AboutLinkCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AboutSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(text = title, style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = modifier)
}
