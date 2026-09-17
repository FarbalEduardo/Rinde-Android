package com.farbalapps.rinde.ui.screen.profile.legal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.theme.RindeTheme
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalScreen(
    initialTab: Int = 0,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 1)) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.legal_screen_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.legal_tab_privacy), fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.legal_tab_terms), fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            if (selectedTab == 0) {
                PrivacyPolicyContent()
            } else {
                TermsOfUseContent()
            }
        }
    }
}

@Composable
private fun PrivacyPolicyContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeaderBadge(
                date = "Septiembre 2026",
                tag = "Conformidad Google Play & ARCO"
            )
        }

        item {
            HighlightCard(
                icon = Icons.Default.Lock,
                title = "Compromiso de Privacidad",
                description = "En Rinde nos tomamos en serio tu privacidad. No comercializamos tus datos personales ni tus listas con terceros. Tienes el control total sobre tu cuenta y puedes eliminarla en cualquier momento.",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                iconColor = MaterialTheme.colorScheme.primary
            )
        }

        item {
            LegalSectionCard(
                number = "1",
                icon = Icons.Default.Badge,
                title = "Información que Recopilamos",
                content = "• Datos de Cuenta: Nombre o alias público, correo electrónico y fotografía de perfil administrados con Firebase Authentication.\n" +
                        "• Contenido Generado por el Usuario (UGC): Ofertas compartidas, títulos, precios, descripciones, sucursales y fotos de comprobación.\n" +
                        "• Interacciones Sociales: Votos de veracidad (Verdad/Falso), comentarios, ofertas guardadas y reportes comunitarios.\n" +
                        "• Listas y Presupuesto Personal: Nombres de listas, productos y metas de ahorro (almacenados localmente o sincronizados en la nube según el Modo Búnker).\n" +
                        "• Métricas y Diagnóstico: Datos técnicos anonimizados sobre versión del sistema y registros de fallos para estabilidad."
            )
        }

        item {
            LegalSectionCard(
                number = "2",
                icon = Icons.Default.Handshake,
                title = "Uso de la Información",
                content = "La información se emplea exclusivamente para:\n" +
                        "1. Operar y sincronizar tus listas de compras y publicaciones.\n" +
                        "2. Calcular la reputación comunitaria y veracidad de ofertas.\n" +
                        "3. Mantener la seguridad de tu sesión y prevenir fraude.\n\n" +
                        "Rinde NO vende, alquila ni comparte tus datos personales o hábitos de compra con empresas publicitarias de terceros."
            )
        }

        item {
            LegalSectionCard(
                number = "3",
                icon = Icons.Default.CameraAlt,
                title = "Permisos del Dispositivo",
                content = "• Cámara: Permite tomar fotos de etiquetas o tickets de compra para respaldar ofertas. No se accede a la cámara fuera de esta acción.\n" +
                        "• Galería Multimedia: Permite adjuntar fotos existentes de ofertas que decidas compartir.\n" +
                        "• Conexión a Internet: Para sincronizar el feed colaborativo en tiempo real."
            )
        }

        item {
            LegalSectionCard(
                number = "4",
                icon = Icons.Default.Cloud,
                title = "Servicios de Terceros e Infraestructura",
                content = "Utilizamos la infraestructura de Google Firebase (Authentication, Cloud Firestore y Storage) con cifrado estándar TLS/SSL en tránsito y AES-256 en reposo, y Coil para almacenamiento en caché local eficiente de imágenes."
            )
        }

        item {
            LegalSectionCard(
                number = "5",
                icon = Icons.Default.Storage,
                title = "Almacenamiento Local y Modo Búnker",
                content = "Tus listas y productos locales se gestionan mediante una base de datos SQLite segura (Room) y Jetpack DataStore en la memoria privada de la app, cumpliendo con las pautas OWASP MASVS de seguridad móvil."
            )
        }

        item {
            LegalSectionCard(
                number = "6",
                icon = Icons.Default.PersonRemove,
                title = "Derechos ARCO y Eliminación de Cuenta",
                content = "Puedes actualizar tu información en cualquier momento desde Editar Perfil.\n\n" +
                        "Derecho al olvido: Conforme a las normativas de Google Play, Rinde incluye la opción de eliminación directa e irreversible en 'Perfil > Editar Perfil > Eliminar Cuenta', borrando tu usuario de Firebase y datos personales de nuestros registros."
            )
        }

        item {
            LegalSectionCard(
                number = "7",
                icon = Icons.Default.Mail,
                title = "Contacto de Privacidad",
                content = "Para cualquier duda o ejercicio de tus derechos de privacidad:\n" +
                        "• Correo: soporte@rinde.app / farbalapps1993@gmail.com\n" +
                        "• Desarrollador: FarbalApps\n" +
                        "• Sitio Web: https://rinde.app"
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TermsOfUseContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeaderBadge(
                date = "Septiembre 2026",
                tag = "Acuerdo de Uso Oficial"
            )
        }

        item {
            HighlightCard(
                icon = Icons.Default.WarningAmber,
                title = "Aviso Crucial sobre Precios",
                description = "Rinde es una plataforma comunitaria y colaborativa. NO somos una tienda ni vendemos productos. Los precios y promociones pertenecen a comercios terceros y pueden variar o agotarse sin previo aviso.",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                iconColor = MaterialTheme.colorScheme.tertiary
            )
        }

        item {
            LegalSectionCard(
                number = "1",
                icon = Icons.Default.Storefront,
                title = "Naturaleza del Servicio y Exención de Responsabilidad",
                content = "• Rinde no fija precios ni garantiza la disponibilidad de los artículos en tiendas físicas u online.\n" +
                        "• Toda compra se realiza directamente entre el usuario y el comercio vendedor externo.\n" +
                        "• Rinde no asume responsabilidad por diferencias tarifarias, promociones caducadas o reclamos de garantía ante tiendas terceras."
            )
        }

        item {
            LegalSectionCard(
                number = "2",
                icon = Icons.Default.Groups,
                title = "Normas de la Comunidad y Contenido de Usuarios (UGC)",
                content = "Está estrictamente prohibido:\n" +
                        "1. Publicar ofertas falsas, engañosas o enlaces fraudulentos (phishing).\n" +
                        "2. Compartir contenido ilícito, dañino o enlaces a malware.\n" +
                        "3. Realizar spam comercial o manipular votos de veracidad mediante cuentas falsas.\n" +
                        "4. Emitir comentarios difamatorios o agresivos.\n\n" +
                        "Moderación: La comunidad cuenta con votos 'Verdad/Falso', reportes de ofertas y bloqueo de usuarios. Rinde se reserva el derecho de retirar publicaciones o cancelar cuentas infractoras sin previo aviso."
            )
        }

        item {
            LegalSectionCard(
                number = "3",
                icon = Icons.Default.Block,
                title = "Uso Permitido y Restricciones Técnicas",
                content = "Queda prohibido descompilar o aplicar ingeniería inversa a la aplicación, vulnerar las medidas de seguridad, o usar bots o scrapers automáticos para extraer información sin consentimiento expreso."
            )
        }

        item {
            LegalSectionCard(
                number = "4",
                icon = Icons.Default.Copyright,
                title = "Propiedad Intelectual",
                content = "La marca Rinde, su imagotipo, diseño de interfaz, código fuente y algoritmos de reputación son propiedad de FarbalApps.\n\n" +
                        "Las marcas registradas, nombres comerciales y logotipos de comercios o productos exhibidos en las ofertas pertenecen a sus respectivos titulares y se utilizan con carácter meramente informativo y referencial (uso legítimo / fair use)."
            )
        }

        item {
            LegalSectionCard(
                number = "5",
                icon = Icons.Default.Shield,
                title = "Limitación de Garantías",
                content = "La app se ofrece 'tal cual' (as is). Rinde no se responsabiliza por pérdidas de datos ocasionadas por fallas en el dispositivo del usuario o por interrupciones de red ajenas a nuestro control en servidores en la nube."
            )
        }

        item {
            LegalSectionCard(
                number = "6",
                icon = Icons.Default.Update,
                title = "Modificaciones a las Condiciones",
                content = "Nos reservamos el derecho de actualizar estos términos para incorporar mejoras o cumplir cambios normativos. La fecha de última actualización siempre estará visible en este apartado."
            )
        }

        item {
            LegalSectionCard(
                number = "7",
                icon = Icons.Default.Gavel,
                title = "Ley Aplicable y Jurisdicción",
                content = "Estos términos se rigen por las leyes aplicables de comercio electrónico y protección de datos. Para resolver cualquier controversia, las partes se someterán a las instancias y tribunales competentes correspondientes."
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeaderBadge(date: String, tag: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Text(
                text = "Actualizado: $date",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = tag,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun HighlightCard(
    icon: ImageVector,
    title: String,
    description: String,
    containerColor: Color,
    contentColor: Color,
    iconColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.9f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun LegalSectionCard(
    number: String,
    icon: ImageVector,
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "$number. $title",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LegalScreenPreview() {
    RindeTheme {
        LegalScreen(onBack = {})
    }
}

