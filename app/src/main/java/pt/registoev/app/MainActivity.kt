package pt.registoev.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pt.registoev.app.data.AppDatabase
import pt.registoev.app.data.EvChargeEntity
import pt.registoev.app.ui.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

enum class AppTab(val title: String, val icon: ImageVector) {
    NEW("Novo", Icons.Default.Add),
    KM("Movimentos", Icons.Default.DirectionsCar),
    CHARGES("Cargas", Icons.Default.Bolt),
    EDIT("Edição", Icons.Default.Edit),
    BACKUP("Gestão", Icons.Default.Storage),
    SETTINGS("Definições", Icons.Default.Settings),
    ABOUT("Sobre", Icons.Default.Info)
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        val db = AppDatabase.get(this)
        val dao = db.dao()

        setContent {
            RegistoEVTheme(darkTheme = true) {
                var editingRecord by remember { mutableStateOf<EvChargeEntity?>(null) }
                val charges by dao.all().collectAsState(initial = emptyList())
                val scope = rememberCoroutineScope()

                // Estado do Pager para permitir swipe lateral
                val pagerState = rememberPagerState(initialPage = AppTab.NEW.ordinal) { AppTab.entries.size }

                // Sincronizar apenas o título e ícones quando o deslize termina ou o alvo muda
                // Efeito para limpar edição ao sair da aba Novo
                LaunchedEffect(pagerState.currentPage) {
                    if (AppTab.entries[pagerState.currentPage] != AppTab.NEW && !pagerState.isScrollInProgress) {
                        editingRecord = null
                    }
                }

                Scaffold(
                    containerColor = Color(0xFF000000),
                    topBar = {
                        TopAppBar(
                            title = { Text(AppTab.entries[pagerState.targetPage].title) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                titleContentColor = Color.White
                            )
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                userScrollEnabled = true,
                                verticalAlignment = Alignment.Top
                            ) { page ->
                                val tab = AppTab.entries[page]
                                when (tab) {
                                    AppTab.NEW -> {
                                        AddChargeScreen(
                                            existingCharges = charges,
                                            editingRecord = editingRecord,
                                            onSave = { origin, destination, odometer, chargeType, kwh, date, id, codPosto ->
                                                scope.launch {
                                                    dao.insert(
                                                        EvChargeEntity(
                                                            id = id ?: 0,
                                                            origin = origin,
                                                            destination = destination,
                                                            odometer = odometer,
                                                            chargeType = chargeType,
                                                            kwh = kwh,
                                                            date = date,
                                                            codPosto = codPosto
                                                        )
                                                    )
                                                    val wasEditing = id != null
                                                    editingRecord = null
                                                    val targetPage = if (wasEditing) AppTab.EDIT.ordinal else AppTab.KM.ordinal
                                                    pagerState.animateScrollToPage(
                                                        page = targetPage,
                                                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                                    )
                                                }
                                            },
                                            onCancelEdit = {
                                                editingRecord = null
                                                scope.launch {
                                                    pagerState.animateScrollToPage(
                                                        page = AppTab.EDIT.ordinal,
                                                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                                    )
                                                }
                                            }
                                        )
                                    }
                                    AppTab.KM -> KmHistoryScreen(charges = charges)
                                    AppTab.CHARGES -> ChargeHistoryScreen(charges = charges)
                                    AppTab.EDIT -> {
                                        EditHistoryScreen(
                                            charges = charges,
                                            onEditClick = { record ->
                                                editingRecord = record
                                                scope.launch {
                                                    pagerState.animateScrollToPage(
                                                        page = AppTab.NEW.ordinal,
                                                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                                    )
                                                }
                                            },
                                            onDeleteSelected = { ids ->
                                                scope.launch {
                                                    dao.deleteByIds(ids)
                                                }
                                            }
                                        )
                                    }
                                    AppTab.BACKUP -> BackupScreen(
                                        onImport = { list ->
                                            scope.launch {
                                                list.forEach { dao.insert(it) }
                                            }
                                        },
                                        onExportToFile = { uri ->
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    contentResolver.openOutputStream(uri)?.use { outputStream ->
                                                        BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                                                            writer.write(exportRecordsToJson(charges))
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        },
                                        onImportFromFile = { uri ->
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    contentResolver.openInputStream(uri)?.use { inputStream ->
                                                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                                                            val json = reader.readText()
                                                            val list = parseJsonBackup(json)
                                                            launch(Dispatchers.Main) {
                                                                list.forEach { dao.insert(it) }
                                                            }
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        },
                                        charges = charges,
                                        onExportCsv = { uri, content ->
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        }
                                    )
                                    AppTab.SETTINGS -> SettingsScreen()
                                    AppTab.ABOUT -> AboutScreen()
                                }
                            }
                        }

                        // BARRA DE NAVEGAÇÃO FLUTUANTE
                        Surface(
                            modifier = Modifier
                                .padding(bottom = 32.dp)
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 8.dp),
                            shape = RoundedCornerShape(28.dp),
                            color = Color(0xFF1E1E1E).copy(alpha = 0.95f),
                            shadowElevation = 12.dp,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AppTab.entries.forEach { tab ->
                                    val isSelected = pagerState.targetPage == tab.ordinal
                                    val tint by animateColorAsState(if (isSelected) Color.White else Color.Gray, label = "")
                                    val background by animateColorAsState(if (isSelected) Color(0xFF2196F3) else Color.Transparent, label = "")

                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(background)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {
                                                    scope.launch {
                                                        pagerState.animateScrollToPage(
                                                            page = tab.ordinal,
                                                            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                                        )
                                                    }
                                                }
                                            )
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.title,
                                                tint = tint,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            AnimatedVisibility(visible = isSelected) {
                                                Row {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = tab.title,
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}
