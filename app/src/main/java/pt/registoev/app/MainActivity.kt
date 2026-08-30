package pt.registoev.app

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import pt.registoev.app.data.AppDatabase
import pt.registoev.app.data.EvChargeEntity
import pt.registoev.app.ui.*
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

enum class AppTab(val title: String, val icon: ImageVector) {
    NEW("Novo", Icons.Default.Add),
    KM("Movimentos", Icons.Default.DirectionsCar),
    CHARGES("Cargas", Icons.Default.Bolt),
    FUEL("Combustível", Icons.Default.LocalGasStation),
    EDIT("Edição", Icons.Default.Edit),
    BACKUP("Gestão", Icons.Default.Storage),
    SETTINGS("Definições", Icons.Default.Settings),
    ABOUT("Sobre", Icons.Default.Info)
}

class MainActivity : ComponentActivity() {
    
    private var stationMap: Map<String, Pair<Double, Double>>? = null

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        val db = AppDatabase.get(this)
        val dao = db.dao()

        var isUpdatingData by mutableStateOf(false)

        // Lógica de Sincronização Inicial V1.7.1 (Mapa em Memória)
        lifecycleScope.launch(Dispatchers.IO) {
            val prefs = getSharedPreferences("registoev_prefs", Context.MODE_PRIVATE)
            val isFirstLaunchV171 = prefs.getBoolean("v171_sync_stable_final_v10", false).not()
            
            if (isFirstLaunchV171) {
                isUpdatingData = true
                android.util.Log.d("RegistoEV", "A iniciar sincronização estável V1.7.1...")
                try {
                    // 1. Limpar localidades residuais
                    dao.clearBadLocalities()
                    dao.resetLocalidadesComPosto()
                    
                    // 2. Indexar Postos
                    val map = getStationMap()
                    
                    // 3. Reprocessar histórico
                    val allWithPosto = dao.allList().filter { it.codPosto.isNotBlank() }.reversed()
                    
                    allWithPosto.forEach { charge ->
                        val code = charge.codPosto
                        val alreadyResolved = dao.getResolvedLocalidade(code)
                        if (alreadyResolved.isNullOrEmpty()) {
                            resolveAndSaveWithMap(this@MainActivity, code, map, dao)
                            delay(3000) // Pausa Nominatim (Aumentada para 3s para evitar bloqueio)
                        }
                    }
                    prefs.edit { putBoolean("v171_sync_stable_final_v10", true) }
                } catch (e: Exception) {
                    android.util.Log.e("RegistoEV", "Erro fatal na sincronização: ${e.message}")
                } finally {
                    isUpdatingData = false
                }
            }
        }

        setContent {
            RegistoEVTheme(darkTheme = true) {
                if (isUpdatingData) {
                    // Ecrã de Sincronização
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF2196F3))
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "A sincronizar localizações oficiais...",
                                color = Color.White, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            Text(
                                "A ler base de dados de postos. Por favor aguarde.",
                                color = Color.Gray, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                } else {
                    var editingRecord by remember { mutableStateOf<EvChargeEntity?>(null) }
                    val charges by dao.all().collectAsState(initial = emptyList())
                    val scope = rememberCoroutineScope()
                    val pagerState = rememberPagerState(initialPage = AppTab.NEW.ordinal) { AppTab.entries.size }

                    LaunchedEffect(pagerState.targetPage) {
                        if (AppTab.entries[pagerState.targetPage] != AppTab.NEW && !pagerState.isScrollInProgress) {
                            editingRecord = null
                        }
                    }

                    Scaffold(
                        containerColor = Color(0xFF000000),
                        topBar = {
                            TopAppBar(
                                title = { Text(AppTab.entries[pagerState.targetPage].title) },
                                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, titleContentColor = Color.White)
                            )
                        }
                    ) { padding ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                                HorizontalPager(
                                    state = pagerState, modifier = Modifier.fillMaxSize(), userScrollEnabled = true, verticalAlignment = Alignment.Top
                                ) { page ->
                                    val tab = AppTab.entries[page]
                                    when (tab) {
                                        AppTab.NEW -> {
                                            AddChargeScreen(
                                                existingCharges = charges,
                                                editingRecord = editingRecord,
                                                onSave = { origin, destination, odometer, chargeType, kwh, date, id, codPosto, liters, manualLocality ->
                                                    scope.launch {
                                                        var locality = manualLocality ?: ""
                                                        if (locality.isEmpty() && codPosto.isNotBlank()) {
                                                            if (id != null && codPosto == (editingRecord?.codPosto ?: "")) {
                                                                locality = editingRecord?.localidade ?: ""
                                                            } else {
                                                                locality = dao.getResolvedLocalidade(codPosto) ?: ""
                                                            }
                                                        }

                                                        val entity = EvChargeEntity(
                                                            id = id ?: 0, origin = origin, destination = destination,
                                                            odometer = odometer, chargeType = chargeType, kwh = kwh, date = date,
                                                            codPosto = codPosto, localidade = locality, liters = liters
                                                        )
                                                        dao.insert(entity)
                                                        
                                                        // Trigger API silenciosa se for código novo
                                                        if (codPosto.isNotBlank() && locality.isEmpty()) {
                                                            lifecycleScope.launch(Dispatchers.IO) {
                                                                val map = getStationMap()
                                                                resolveAndSaveWithMap(this@MainActivity, codPosto, map, dao)
                                                            }
                                                        }

                                                        editingRecord = null
                                                        pagerState.animateScrollToPage(if (id != null) AppTab.EDIT.ordinal else AppTab.KM.ordinal)
                                                    }
                                                },
                                                onCancelEdit = {
                                                    editingRecord = null
                                                    scope.launch { pagerState.animateScrollToPage(AppTab.EDIT.ordinal) }
                                                }
                                            )
                                        }
                                        AppTab.KM -> KmHistoryScreen(charges = charges)
                                        AppTab.CHARGES -> ChargeHistoryScreen(charges = charges)
                                        AppTab.FUEL -> FuelHistoryScreen(charges = charges)
                                        AppTab.EDIT -> {
                                            EditHistoryScreen(
                                                charges = charges,
                                                onEditClick = { record ->
                                                    editingRecord = record
                                                    scope.launch { pagerState.animateScrollToPage(AppTab.NEW.ordinal) }
                                                },
                                                onDeleteSelected = { ids ->
                                                    scope.launch { dao.deleteByIds(ids) }
                                                }
                                            )
                                        }
                                        AppTab.BACKUP -> BackupScreen(
                                            onImport = { list -> scope.launch { list.forEach { dao.insert(it) } } },
                                            onExportToFile = { uri ->
                                                scope.launch(Dispatchers.IO) {
                                                    try {
                                                        contentResolver.openOutputStream(uri)?.use { os ->
                                                            BufferedWriter(OutputStreamWriter(os)).use { writer ->
                                                                writer.write(exportRecordsToJson(charges))
                                                            }
                                                        }
                                                    } catch (e: Exception) { e.printStackTrace() }
                                                }
                                            },
                                            onImportFromFile = { uri ->
                                                scope.launch(Dispatchers.IO) {
                                                    try {
                                                        contentResolver.openInputStream(uri)?.use { istream ->
                                                            BufferedReader(InputStreamReader(istream)).use { reader ->
                                                                val json = reader.readText()
                                                                val list = parseJsonBackup(json)
                                                                launch(Dispatchers.Main) { list.forEach { dao.insert(it) } }
                                                            }
                                                        }
                                                    } catch (e: Exception) { e.printStackTrace() }
                                                }
                                            },
                                            charges = charges,
                                            onExportCsv = { uri, content ->
                                                scope.launch(Dispatchers.IO) {
                                                    try { contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) } }
                                                    catch (e: Exception) { e.printStackTrace() }
                                                }
                                            }
                                        )
                                        AppTab.SETTINGS -> SettingsScreen()
                                        AppTab.ABOUT -> AboutScreen()
                                    }
                                }
                            }

                            // BARRA DE NAVEGAÇÃO
                            Surface(
                                modifier = Modifier.padding(bottom = 32.dp).align(Alignment.BottomCenter).padding(horizontal = 8.dp),
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
                                                .clip(CircleShape).background(background)
                                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                                    scope.launch { pagerState.animateScrollToPage(tab.ordinal) }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(tab.icon, tab.title, tint = tint, modifier = Modifier.size(20.dp))
                                                AnimatedVisibility(visible = isSelected) {
                                                    Row {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(tab.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
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

    private fun getStationMap(): Map<String, Pair<Double, Double>> {
        if (stationMap != null) return stationMap!!
        val newMap = mutableMapOf<String, Pair<Double, Double>>()
        try {
            val jsonText = assets.open("Todos_Simplificado.json").bufferedReader().use { it.readText() }
            val elements = JSONObject(jsonText).getJSONArray("elements")
            for (i in 0 until elements.length()) {
                val el = elements.getJSONObject(i)
                val tags = el.optJSONObject("tags")
                if (tags != null) {
                    val ref = tags.optString("ref", "")
                    val name = tags.optString("name", "")
                    val lat = if (el.has("lat")) el.getDouble("lat") else el.optJSONObject("center")?.optDouble("lat") ?: 0.0
                    val lon = if (el.has("lon")) el.getDouble("lon") else el.optJSONObject("center")?.optDouble("lon") ?: 0.0
                    
                    if (ref.isNotBlank()) newMap[ref.lowercase()] = Pair(lat, lon)
                    if (name.isNotBlank()) newMap[name.lowercase()] = Pair(lat, lon)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        stationMap = newMap
        return stationMap!!
    }
}

suspend fun resolveAndSaveWithMap(context: Context, stationCode: String, map: Map<String, Pair<Double, Double>>, dao: pt.registoev.app.data.EvDao) {
    try {
        val coords = map[stationCode.lowercase()]
        if (coords != null) {
            val nominatimUrl = "https://nominatim.openstreetmap.org/reverse?lat=${coords.first}&lon=${coords.second}&format=json"
            val conn = URL(nominatimUrl).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val address = JSONObject(response).optJSONObject("address")
                val localidade = listOf("city", "town", "village", "suburb", "hamlet", "municipality")
                    .map { address?.optString(it, "") ?: "" }
                    .firstOrNull { it.isNotBlank() } ?: ""
                
                if (localidade.isNotBlank()) {
                    dao.updateLocalidadeByStationCode(stationCode, localidade)
                    (context as? android.app.Activity)?.runOnUiThread {
                        Toast.makeText(context, "Resolvido: $localidade", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("RegistoEV", "Erro ao resolver $stationCode: ${e.message}")
    }
}
