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
    private const val GOOGLE_SCRIPT_URL = "https://script.google.com/macros/s/AKfycbx-EQjd6GX5uKI0Ig_tUlr2NtgzKJhUx95XPK8_DP6XEY73iriBZWD74gZ02kNGDH4xTA/exec"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
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
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

        val bytes = jsonPayload.toByteArray(Charsets.UTF_8)
        val body = bytes.toRequestBody(JSON_TYPE)
        val initialRequest = Request.Builder()
            .url(GOOGLE_SCRIPT_URL)
            .post(body)
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .build()

        val response = noRedirectClient.newCall(initialRequest).execute()

        if (response.code in 300..399) {
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
     * Envia um comando para eliminar um registo no Google Sheets por ID numérico simples.
     */
    suspend fun deleteRecord(id: Long): Boolean = withContext(Dispatchers.IO) {
        return@withContext deleteRecordBySyncId(id.toString())
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
}
