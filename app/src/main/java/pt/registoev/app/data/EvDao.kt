package pt.registoev.app.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EvDao {
 @Query("SELECT * FROM ev_charges ORDER BY date DESC")
 fun all(): Flow<List<EvChargeEntity>>

 @Query("SELECT * FROM ev_charges ORDER BY date DESC")
 suspend fun allList(): List<EvChargeEntity>

 @Insert(onConflict = OnConflictStrategy.REPLACE)
 suspend fun insert(e: EvChargeEntity): Long

 @Delete
 suspend fun delete(e: EvChargeEntity)

 @Query("DELETE FROM ev_charges WHERE id IN (:ids)")
 suspend fun deleteByIds(ids: List<Long>)

 @Query("UPDATE ev_charges SET localidade = '' WHERE codPosto != ''")
 suspend fun resetLocalidadesComPosto()

 @Query("SELECT localidade FROM ev_charges WHERE codPosto = :stationCode AND localidade != '' LIMIT 1")
 suspend fun getResolvedLocalidade(stationCode: String): String?

 @Query("UPDATE ev_charges SET localidade = '' WHERE codPosto != '' AND localidade = destination")
 suspend fun clearBadLocalities()

 @Query("UPDATE ev_charges SET localidade = :localidade WHERE codPosto = :stationCode")
 suspend fun updateLocalidadeByStationCode(stationCode: String, localidade: String)
}
