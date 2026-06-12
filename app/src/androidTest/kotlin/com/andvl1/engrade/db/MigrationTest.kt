package com.andvl1.engrade.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.andvl1.engrade.data.db.EnGardeDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented migration test for [EnGardeDatabase].
 *
 * Proves that MIGRATION_1_2 is additive-only:
 * - Creates de_tableau and de_match tables.
 * - Existing pool / fencer / pool_fencer / pool_bout rows survive intact.
 * - The new tables are empty and queryable immediately after migration.
 *
 * [MigrationTestHelper.runMigrationsAndValidate] additionally runs Room's automated
 * schema validation against the exported 2.json schema — it will throw if the
 * SQL we wrote in the migration does not match what Room generated from the entities.
 *
 * Run via: ./gradlew :app:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EnGardeDatabase::class.java
    )

    @Test
    fun migration1To2_existingDataSurvivesAndNewTablesExist() {
        val testDbName = "engarde-migration-test"

        // 1. Create a v1 database and populate it with representative data.
        helper.createDatabase(testDbName, 1).use { db ->
            // Pool (COMPLETED = all bouts done, ready for DE)
            db.execSQL(
                "INSERT INTO pool (createdAt, mode, weapon, status) VALUES (1000, 5, 'SABRE', 'COMPLETED')"
            )
            // Fencers
            db.execSQL("INSERT INTO fencer (name, organization, region) VALUES ('Alice', 'CFC', 'Moscow')")
            db.execSQL("INSERT INTO fencer (name, organization, region) VALUES ('Bob', null, null)")
            // Pool fencers
            db.execSQL("INSERT INTO pool_fencer (poolId, fencerId, seedNumber, excluded) VALUES (1, 1, 1, 0)")
            db.execSQL("INSERT INTO pool_fencer (poolId, fencerId, seedNumber, excluded) VALUES (1, 2, 2, 0)")
            // Pool bout (Alice beat Bob 5:3)
            db.execSQL(
                "INSERT INTO pool_bout (poolId, boutOrder, leftFencerSeed, rightFencerSeed, leftScore, rightScore, winner, status) " +
                    "VALUES (1, 1, 1, 2, 5, 3, 'LEFT', 'COMPLETED')"
            )
        }

        // 2. Run migration and let Room validate the resulting schema against 2.json.
        //    validateDroppedTables = true ensures no accidental drops occurred.
        helper.runMigrationsAndValidate(
            testDbName,
            2,
            true,  // validateDroppedTables
            EnGardeDatabase.MIGRATION_1_2
        ).use { db ->

            // --- Verify existing pool data survived ---
            db.query("SELECT * FROM pool WHERE id = 1").use { cursor ->
                assertTrue("Pool row must survive migration", cursor.moveToFirst())
                assertEquals(5, cursor.getInt(cursor.getColumnIndexOrThrow("mode")))
                assertEquals("SABRE", cursor.getString(cursor.getColumnIndexOrThrow("weapon")))
                assertEquals("COMPLETED", cursor.getString(cursor.getColumnIndexOrThrow("status")))
                assertFalse("Only one pool row expected", cursor.moveToNext())
            }

            // --- Verify fencer data survived ---
            db.query("SELECT * FROM fencer WHERE id = 1").use { cursor ->
                assertTrue("Fencer row must survive migration", cursor.moveToFirst())
                assertEquals("Alice", cursor.getString(cursor.getColumnIndexOrThrow("name")))
                assertEquals("CFC", cursor.getString(cursor.getColumnIndexOrThrow("organization")))
            }

            db.query("SELECT * FROM fencer WHERE id = 2").use { cursor ->
                assertTrue("Second fencer row must survive migration", cursor.moveToFirst())
                assertEquals("Bob", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }

            // --- Verify pool_fencer data survived ---
            db.query("SELECT COUNT(*) FROM pool_fencer WHERE poolId = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(2, cursor.getInt(0))
            }

            // --- Verify pool_bout data survived ---
            db.query("SELECT * FROM pool_bout WHERE id = 1").use { cursor ->
                assertTrue("Bout row must survive migration", cursor.moveToFirst())
                assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("boutOrder")))
                assertEquals(5, cursor.getInt(cursor.getColumnIndexOrThrow("leftScore")))
                assertEquals(3, cursor.getInt(cursor.getColumnIndexOrThrow("rightScore")))
                assertEquals("LEFT", cursor.getString(cursor.getColumnIndexOrThrow("winner")))
                assertEquals("COMPLETED", cursor.getString(cursor.getColumnIndexOrThrow("status")))
            }

            // --- Verify new de_tableau table exists and is empty ---
            db.query("SELECT COUNT(*) FROM de_tableau").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("de_tableau must be empty after migration", 0, cursor.getInt(0))
            }

            // --- Verify new de_match table exists and is empty ---
            db.query("SELECT COUNT(*) FROM de_match").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("de_match must be empty after migration", 0, cursor.getInt(0))
            }

            // --- Verify the FK index on de_tableau exists ---
            db.query(
                "SELECT name FROM sqlite_master WHERE type='index' AND name='index_de_tableau_poolId'"
            ).use { cursor ->
                assertTrue("Index index_de_tableau_poolId must exist", cursor.moveToFirst())
            }

            // --- Verify the FK index on de_match exists ---
            db.query(
                "SELECT name FROM sqlite_master WHERE type='index' AND name='index_de_match_tableauId'"
            ).use { cursor ->
                assertTrue("Index index_de_match_tableauId must exist", cursor.moveToFirst())
            }
        }
    }
}
