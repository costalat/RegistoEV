package pt.registoev.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.google.gson.Gson
import pt.registoev.app.data.EvChargeEntity

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val chargeJson = inputData.getString("charge_json") ?: return Result.failure()
        val charge = try {
            Gson().fromJson(chargeJson, EvChargeEntity::class.java)
        } catch (e: Exception) {
            return Result.failure()
        }

        val isSuccess = CloudSyncManager.syncRecord(charge)
        
        return if (isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
