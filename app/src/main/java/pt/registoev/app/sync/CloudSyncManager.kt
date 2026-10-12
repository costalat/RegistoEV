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
    private const val GOOGLE_SCRIPT_URL = "https://script.google.com/macros/s/AKfycby4MNZfDu24sI8aDjJCE-XQjKuM8ZNoMGwPmux704UnwbSJDlh55NmqFfnOoglSObZ6Aw/exec"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()

    /**
     * Envia um registo para o Google Sheets com dados extras de perfil.
     * Inclui mapeamento duplo de chaves (Inglês e Português) para garantir
     * compatibilidade com o Google Apps Script.
     */
    suspend fun syncRecord(
        charge: EvChargeEntity,
        plate: String = "",
        driver: String = "",
    ): Boolean = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(charge.date))

        val payload = mapOf(
            "id" to charge.id,
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
        android.util.Log.d("CloudSync", "A enviar payload: $json")

        // Garantir explicitamente que os bytes são codificados em UTF-8
        val body = json.toByteArray(Charsets.UTF_8).toRequestBody(JSON_TYPE)
        val request = Request.Builder()
            .url(GOOGLE_SCRIPT_URL)
            .post(body)
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .build()

        return@withContext try {
            client.newCall(request).execute().use { response ->
                val isSuccess = response.isSuccessful
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
     * Envia um comando para eliminar um registo no Google Sheets.
     */
    suspend fun deleteRecord(id: Long): Boolean = withContext(Dispatchers.IO) {
        val payload = mapOf(
            "id" to id,
            "action" to "DELETE",
        )
        
        val json = gson.toJson(payload)
        android.util.Log.d("CloudSync", "A eliminar registo na nuvem -> ID: $id")

        val body = json.toByteArray(Charsets.UTF_8).toRequestBody(JSON_TYPE)
        val request = Request.Builder()
            .url(GOOGLE_SCRIPT_URL)
            .post(body)
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .build()

        return@withContext try {
            client.newCall(request).execute().use { response ->
                val isSuccess = response.isSuccessful
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
