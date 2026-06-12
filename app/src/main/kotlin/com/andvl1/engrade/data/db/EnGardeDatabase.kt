package com.andvl1.engrade.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.andvl1.engrade.data.db.dao.DeTableauDao
import com.andvl1.engrade.data.db.dao.FencerDao
import com.andvl1.engrade.data.db.dao.PoolBoutDao
import com.andvl1.engrade.data.db.dao.PoolDao
import com.andvl1.engrade.data.db.dao.PoolFencerDao
import com.andvl1.engrade.data.db.entity.DeMatchEntity
import com.andvl1.engrade.data.db.entity.DeTableauEntity
import com.andvl1.engrade.data.db.entity.FencerEntity
import com.andvl1.engrade.data.db.entity.PoolBoutEntity
import com.andvl1.engrade.data.db.entity.PoolEntity
import com.andvl1.engrade.data.db.entity.PoolFencerEntity

@Database(
    entities = [
        FencerEntity::class,
        PoolEntity::class,
        PoolFencerEntity::class,
        PoolBoutEntity::class,
        DeTableauEntity::class,
        DeMatchEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class EnGardeDatabase : RoomDatabase() {
    abstract fun fencerDao(): FencerDao
    abstract fun poolDao(): PoolDao
    abstract fun poolFencerDao(): PoolFencerDao
    abstract fun poolBoutDao(): PoolBoutDao
    abstract fun deTableauDao(): DeTableauDao

    companion object {
        /**
         * Additive migration from version 1 to 2.
         *
         * SAFE: Only creates two new tables (de_tableau, de_match) and their indices.
         * Does NOT alter, drop, or rename any existing tables.
         * Existing pool / fencer / pool_fencer / pool_bout data is fully preserved.
         *
         * The SQL is cross-verified against
         * app/schemas/com.andvl1.engrade.data.db.EnGardeDatabase/2.json
         * which Room KSP generates from the entity definitions.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create de_tableau — FK to pool(id) CASCADE
                @Suppress("MaxLineLength")
                db.execSQL("CREATE TABLE IF NOT EXISTS `de_tableau` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `poolId` INTEGER NOT NULL, `tableauSize` INTEGER NOT NULL, `fencerCount` INTEGER NOT NULL, `weapon` TEXT NOT NULL, `mode` INTEGER NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`poolId`) REFERENCES `pool`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_de_tableau_poolId` ON `de_tableau` (`poolId`)")

                // Create de_match — FK to de_tableau(id) CASCADE
                @Suppress("MaxLineLength")
                db.execSQL("CREATE TABLE IF NOT EXISTS `de_match` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tableauId` INTEGER NOT NULL, `matchId` INTEGER NOT NULL, `round` INTEGER NOT NULL, `position` INTEGER NOT NULL, `topSlotType` TEXT NOT NULL, `topSeed` INTEGER, `topFencerName` TEXT, `bottomSlotType` TEXT NOT NULL, `bottomSeed` INTEGER, `bottomFencerName` TEXT, `topScore` INTEGER, `bottomScore` INTEGER, `winnerSeed` INTEGER, `winnerFencerName` TEXT, `isBye` INTEGER NOT NULL, `status` TEXT NOT NULL, FOREIGN KEY(`tableauId`) REFERENCES `de_tableau`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_de_match_tableauId` ON `de_match` (`tableauId`)")
            }
        }
    }
}
