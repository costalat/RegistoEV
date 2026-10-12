package pt.registoev.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import pt.registoev.app.data.EvChargeEntity

@Suppress("unused")
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val chargeJson = inputData.getString("charge_json") ?: return Result.failure()
        val charge = try {
            Gson().fromJson(chargeJson, EvChargeEntity::class.java)
        } catch (_: Exception) {
            return Result.failure()
        }

        val prefs = applicationContext.getSharedPreferences("registoev_prefs", Context.MODE_PRIVATE)
        val vehiclePlate = prefs.getString("vehicle_plate", "") ?: ""
        val driverName = prefs.getString("driver_name", "") ?: ""

        val isSuccess = CloudSyncManager.syncRecord(charge, vehiclePlate, driverName)
        
        return if (isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
