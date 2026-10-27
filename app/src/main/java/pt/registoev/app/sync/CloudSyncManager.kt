package pt.registoev.app.sync

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import pt.registoev.app.data.EvChargeEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object CloudSyncManager {
    private val GOOGLE_SCRIPT_URL = AppConfig.GOOGLE_SCRIPT_URL
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(followRedirects = true)
        .followSslRedirects(followProtocolRedirects = true)
        .build()

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()

    /**
     * Gera uma chave de sincronização única por registo.
     */
    fun getSyncId(charge: EvChargeEntity): String {
        return if (charge.id > 1000000000000L) {
            charge.id.toString()
        } else {
            "${charge.date}_${charge.id}"
        }
    }

    /**
     * Envia um registo para o Google Sheets com dados extras de perfil.
     * Inclui mapeamento duplo de chaves (Inglês e Português) para garantir
     * compatibilidade com o Google Apps Script.
     */
    suspend fun syncRecord(
        charge: EvChargeEntity,
        plate: String = "",
        driver: String = "",
        isEdit: Boolean = false,
    ): Boolean = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(charge.date))
        val syncId = getSyncId(charge)

        val payload = mapOf(
            "id" to syncId,
            "syncId" to syncId,
            "isEdit" to isEdit,
            // Mapeamento duplo para Origem
            "origin" to charge.origin,
            "origem" to charge.origin,
            // Mapeamento duplo para Destino
            "destination" to charge.destination,
            "destino" to charge.destination,
            // Mapeamento duplo para Odómetro
            "odometer" to charge.odometer,
            "odometro" to charge.odometer,
            "odo" to charge.odometer,
            // Tipo de Carga / Combustível
            "chargeType" to charge.chargeType,
            "tipoCarga" to charge.chargeType,
            // kWh e Litros
            "kwh" to charge.kwh,
            "kw" to charge.kwh,
            "liters" to (charge.liters ?: 0.0),
            "litros" to (charge.liters ?: 0.0),
            // Data
            "date" to charge.date,
            "data" to formattedDate,
            // Estação e Localidade
            "codPosto" to charge.codPosto,
            "localidade" to charge.localidade,
            // Perfil
            "plate" to plate,
            "matricula" to plate,
            "driver" to driver,
            "condutor" to driver,
            // Ação
            "action" to "SAVE",
        )
        
        val json = gson.toJson(payload)
        android.util.Log.d("CloudSync", "A enviar payload SAVE (isEdit=$isEdit): $json")

        return@withContext try {
            executePostRequest(json).use { response ->
                val responseText = response.body?.string() ?: ""
                val isSuccess = response.isSuccessful
                android.util.Log.d("CloudSync", "Resposta SAVE da nuvem (HTTP ${response.code}): $responseText")
                if (!isSuccess) {
                    android.util.Log.e("CloudSync", "Erro na sincronização: ${response.code}")
                }
                isSuccess
            }
        } catch (e: Exception) {
            android.util.Log.e("CloudSync", "Falha de rede ao sincronizar: ${e.message}")
            false
        }
    }

    /**
     * Executa um pedido POST com reencaminhamento manual mantendo o corpo JSON intacto.
     */
    private fun executePostRequest(jsonPayload: String): okhttp3.Response {
        val noRedirectClient = client.newBuilder()
            .followRedirects(followRedirects = false)
            .followSslRedirects(followProtocolRedirects = false)
            .build()

        val bytes = jsonPayload.toByteArray(Charsets.UTF_8)
        val body = bytes.toRequestBody(JSON_TYPE)
        val initialRequest = Request.Builder()
            .url(GOOGLE_SCRIPT_URL)
            .post(body)
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .build()

        val response = noRedirectClient.newCall(initialRequest).execute()

        if ((response.code in 300..399)) {
            val redirectUrl = response.header("Location")
            response.close()

            if (!redirectUrl.isNullOrBlank()) {
                android.util.Log.d("CloudSync", "A seguir reencaminhamento POST para: $redirectUrl")
                val redirectedRequest = Request.Builder()
                    .url(redirectUrl)
                    .post(bytes.toRequestBody(JSON_TYPE))
                    .addHeader("Content-Type", "application/json; charset=utf-8")
                    .build()
                return client.newCall(redirectedRequest).execute()
            }
        }
        return response
    }

    /**
     * Envia um comando para eliminar um registo no Google Sheets por entidade.
     */
    suspend fun deleteRecord(charge: EvChargeEntity): Boolean {
        return deleteRecordBySyncId(getSyncId(charge))
    }

    /**
     * Envia um comando para eliminar um registo no Google Sheets por SyncID.
     */
    suspend fun deleteRecordBySyncId(syncId: String): Boolean = withContext(Dispatchers.IO) {
        val payload = mapOf(
            "id" to syncId,
            "syncId" to syncId,
            "action" to "DELETE",
        )
        
        val json = gson.toJson(payload)
        android.util.Log.d("CloudSync", "A eliminar registo na nuvem -> SyncID: $syncId")

        return@withContext try {
            executePostRequest(json).use { response ->
                val responseText = response.body?.string() ?: ""
                val isSuccess = response.isSuccessful
                android.util.Log.d("CloudSync", "Resposta DELETE da nuvem (HTTP ${response.code}): $responseText")
                if (!isSuccess) {
                    android.util.Log.e("CloudSync", "Erro ao eliminar na nuvem: ${response.code}")
                }
                isSuccess
            }
        } catch (e: Exception) {
            android.util.Log.e("CloudSync", "Falha de rede ao eliminar na nuvem: ${e.message}")
            false
        }
    }

    /**
     * Pesquisa e descarrega da nuvem (Google Sheets) APENAS o registo com o maior odómetro
     * ou data mais recente para a matrícula configurada.
     */
    suspend fun fetchLatestRecord(plate: String): EvChargeEntity? = withContext(Dispatchers.IO) {
        if (plate.isBlank()) return@withContext null

        val encodedPlate = try {
            java.net.URLEncoder.encode(plate.trim(), "UTF-8")
        } catch (_: Exception) {
            plate.trim()
        }

        val url = "$GOOGLE_SCRIPT_URL?action=FETCH&plate=$encodedPlate"
        android.util.Log.d("CloudSync", "A pesquisar último registo de controlo para matrícula $plate: $url")

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return@withContext try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseText = response.body?.string() ?: ""
                    if (responseText.isNotBlank() && responseText.trim().startsWith("[")) {
                        val jsonArray = com.google.gson.JsonParser.parseString(responseText).asJsonArray
                        var latestRecord: EvChargeEntity? = null
                        var maxOdometer = -1
                        var maxDate = -1L

                        for (element in jsonArray) {
                            try {
                                val obj = element.asJsonObject
                                val origin = obj.get("origin")?.takeIf { !it.isJsonNull }?.asString ?: ""
                                val destination = obj.get("destination")?.takeIf { !it.isJsonNull }?.asString ?: ""
                                val odometer = obj.get("odometer")?.takeIf { !it.isJsonNull }?.asInt ?: 0
                                val chargeType = obj.get("chargeType")?.takeIf { !it.isJsonNull }?.asString ?: "Nenhum"
                                val kwh = obj.get("kwh")?.takeIf { !it.isJsonNull }?.asDouble ?: 0.0
                                val date = obj.get("date")?.takeIf { !it.isJsonNull }?.asLong ?: 0L
                                val codPosto = obj.get("codPosto")?.takeIf { !it.isJsonNull }?.asString ?: ""
                                val localidade = obj.get("localidade")?.takeIf { !it.isJsonNull }?.asString ?: ""
                                val liters = obj.get("liters")?.takeIf { !it.isJsonNull }?.asDouble
                                val vPlate = obj.get("vehiclePlate")?.takeIf { !it.isJsonNull }?.asString ?: plate

                                if (odometer > 0 && date > 0) {
                                    if (odometer > maxOdometer || (odometer == maxOdometer && date > maxDate)) {
                                        maxOdometer = odometer
                                        maxDate = date
                                        latestRecord = EvChargeEntity(
                                            id = 0,
                                            origin = origin,
                                            destination = destination,
                                            odometer = odometer,
                                            chargeType = chargeType,
                                            kwh = kwh,
                                            date = date,
                                            codPosto = codPosto,
                                            localidade = localidade,
                                            liters = liters,
                                            vehiclePlate = vPlate
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("CloudSync", "Erro ao converter registo: ${e.message}")
                            }
                        }
                        latestRecord
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CloudSync", "Falha de rede ao descarregar último registo: ${e.message}")
            null
        }
    }

    /**
     * Consulta a nuvem para a matrícula configurada e descarrega/guarda o registo se
     * o odómetro da nuvem for superior ao registo local mais recente no smartphone.
     */
    suspend fun syncLatestRecordIfNeeded(dao: pt.registoev.app.data.EvDao, plate: String): Boolean = withContext(Dispatchers.IO) {
        if (plate.isBlank()) return@withContext false
        val cleanPlate = if (plate.trim().uppercase() == "00-AA-00") "" else plate.trim()
        if (cleanPlate.isBlank()) return@withContext false

        val cloudRecord = fetchLatestRecord(cleanPlate) ?: return@withContext false
        val localList = dao.allListForPlate(cleanPlate)
        val localMaxOdometer = localList.maxOfOrNull { it.odometer } ?: 0

        if (cloudRecord.odometer > localMaxOdometer) {
            val alreadyExists = localList.any { 
                it.odometer == cloudRecord.odometer && it.date == cloudRecord.date 
            }
            if (!alreadyExists) {
                android.util.Log.d("CloudSync", "Descarregado registo mais recente da nuvem para $cleanPlate: Odo ${cloudRecord.odometer} km (Local máx: $localMaxOdometer km)")
                dao.insert(cloudRecord.copy(id = 0, vehiclePlate = cleanPlate))
                return@withContext true
            }
        }
        return@withContext false
    }
}
