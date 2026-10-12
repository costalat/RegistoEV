
package pt.registoev.app.data
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ev_charges")
data class EvChargeEntity(
 @PrimaryKey(autoGenerate = true) val id: Long = 0,
 val origin: String,
 val destination: String,
 val odometer: Int,
 val chargeType: String,
 val kwh: Double,
 val date: Long,
 val codPosto: String = "",
 val localidade: String = "",
 val liters: Double? = null,
)
