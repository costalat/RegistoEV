
package pt.registoev.app.data
import androidx.room.*
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities=[EvChargeEntity::class], version=6)
abstract class AppDatabase: RoomDatabase() {
 abstract fun dao(): EvDao
 companion object {
  private val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE ev_charges ADD COLUMN codPosto TEXT NOT NULL DEFAULT ''")
      }
  }

  private val MIGRATION_4_5 = object : Migration(4, 5) {
      override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE ev_charges ADD COLUMN localidade TEXT NOT NULL DEFAULT ''")
      }
  }

  private val MIGRATION_5_6 = object : Migration(5, 6) {
      override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE ev_charges ADD COLUMN liters REAL")
      }
  }

  fun get(ctx: Context) = Room.databaseBuilder(ctx, AppDatabase::class.java, "ev.db")
      .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
      .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
      .build()
 }
}
