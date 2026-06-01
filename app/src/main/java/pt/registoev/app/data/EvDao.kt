package pt.registoev.app.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EvDao {
 @Query("SELECT * FROM ev_charges ORDER BY date DESC")
 fun all(): Flow<List<EvChargeEntity>>

 @Insert(onConflict = OnConflictStrategy.REPLACE)
 suspend fun insert(e: EvChargeEntity)

 @Delete
 suspend fun delete(e: EvChargeEntity)

 @Query("DELETE FROM ev_charges WHERE id IN (:ids)")
 suspend fun deleteByIds(ids: List<Long>)
}
