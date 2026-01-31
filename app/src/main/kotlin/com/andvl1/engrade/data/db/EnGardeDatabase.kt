package com.andvl1.engrade.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.andvl1.engrade.data.db.dao.FencerDao
import com.andvl1.engrade.data.db.dao.PoolBoutDao
import com.andvl1.engrade.data.db.dao.PoolDao
import com.andvl1.engrade.data.db.dao.PoolFencerDao
import com.andvl1.engrade.data.db.entity.FencerEntity
import com.andvl1.engrade.data.db.entity.PoolBoutEntity
import com.andvl1.engrade.data.db.entity.PoolEntity
import com.andvl1.engrade.data.db.entity.PoolFencerEntity

@Database(
    entities = [
        FencerEntity::class,
        PoolEntity::class,
        PoolFencerEntity::class,
        PoolBoutEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class EnGardeDatabase : RoomDatabase() {
    abstract fun fencerDao(): FencerDao
    abstract fun poolDao(): PoolDao
    abstract fun poolFencerDao(): PoolFencerDao
    abstract fun poolBoutDao(): PoolBoutDao
}
